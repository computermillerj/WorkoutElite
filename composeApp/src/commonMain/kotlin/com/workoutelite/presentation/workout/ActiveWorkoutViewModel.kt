package com.workoutelite.presentation.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutelite.domain.common.Result
import com.workoutelite.domain.exercise.ExerciseRepository
import com.workoutelite.domain.workout.ActiveWorkoutSession
import com.workoutelite.domain.workout.CompleteWorkoutUseCase
import com.workoutelite.domain.workout.DailyWorkout
import com.workoutelite.domain.workout.DifficultyFeedback
import com.workoutelite.domain.workout.TimerPhase
import com.workoutelite.domain.workout.WorkoutRepository
import com.workoutelite.platform.ClockProvider
import com.workoutelite.platform.WorkoutCuePlayer
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Drives the interval timer for the active workout. Remaining time is computed from a monotonic
 * time mark captured at phase start (plus time banked across pauses), never by counting ticks,
 * so the display cannot drift and survives wall-clock changes.
 */
class ActiveWorkoutViewModel(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val clockProvider: ClockProvider,
    private val completeWorkout: CompleteWorkoutUseCase,
    private val cuePlayer: WorkoutCuePlayer,
) : ViewModel() {
    private val _state = MutableStateFlow(ActiveWorkoutState())
    val state = _state.asStateFlow()

    private val _events = Channel<ActiveWorkoutEvent>()
    val events = _events.receiveAsFlow()

    private val timeSource = TimeSource.Monotonic

    private var workout: DailyWorkout? = null
    private var exercises = emptyMap<String, ExerciseInfo>()
    private var itemIndex = 0
    private var phase = RuntimePhase.PREPARE
    private var phaseElapsedBanked = Duration.ZERO
    private var resumeMark: TimeSource.Monotonic.ValueTimeMark? = null
    private var isPaused = false
    private var activeSecondsTotal = 0
    private var halfwayCuePlayed = false
    private var lastRenderedSecond = -1L
    private var ticker: Job? = null

    init {
        loadActiveWorkout()
    }

    fun onAction(action: ActiveWorkoutAction) {
        when (action) {
            ActiveWorkoutAction.OnPauseResumeClick -> if (isPaused) resume() else pause()
            ActiveWorkoutAction.OnSkipClick -> skipPhase()
            ActiveWorkoutAction.OnQuitClick -> showQuitConfirmation()
            ActiveWorkoutAction.OnConfirmQuitClick -> quitWorkout()
            ActiveWorkoutAction.OnDismissQuitClick ->
                _state.update { it.copy(showQuitConfirmation = false) }
            ActiveWorkoutAction.OnBackgrounded -> pauseFromBackground()
            ActiveWorkoutAction.OnSkipFeedbackClick -> saveFeedback(DifficultyFeedback.MEDIUM)
            is ActiveWorkoutAction.OnFeedbackClick -> saveFeedback(action.feedback)
        }
    }

    private fun loadActiveWorkout() {
        viewModelScope.launch {
            try {
                val session = workoutRepository.getActiveSession()
                val restored = session?.let { workoutRepository.getWorkoutById(it.workoutId) }
                if (session == null || restored == null || restored.items.isEmpty()) {
                    _state.update { it.copy(isLoading = false, error = "No active workout.") }
                    return@launch
                }
                workout = restored
                exercises = exerciseRepository
                    .getExercisesByIds(restored.items.map { it.exerciseId })
                    .associate { it.id to ExerciseInfo(name = it.name, description = it.description) }
                applySession(session, restored)
                persistSession()
                render()
                if (!isPaused) startTicker()
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _state.update { it.copy(isLoading = false, error = "Could not load the workout.") }
            }
        }
    }

    private fun applySession(session: ActiveWorkoutSession, restored: DailyWorkout) {
        val isFreshStart = session.currentItemIndex == 0 &&
            session.phase == TimerPhase.WORK &&
            session.elapsedSecondsInPhase == 0 &&
            !session.isPaused
        if (isFreshStart) {
            itemIndex = 0
            phase = RuntimePhase.PREPARE
            phaseElapsedBanked = Duration.ZERO
            resumeMark = timeSource.markNow()
            isPaused = false
            return
        }

        // Restore after process death or relaunch: land paused on the current exercise.
        itemIndex = session.currentItemIndex.coerceIn(0, restored.items.lastIndex)
        phase = when (session.phase) {
            TimerPhase.WORK -> RuntimePhase.WORK
            TimerPhase.REST -> RuntimePhase.REST
        }
        phaseElapsedBanked = session.elapsedSecondsInPhase
            .coerceIn(0, phaseDurationSeconds())
            .seconds
        resumeMark = null
        isPaused = true
        val item = restored.items[itemIndex]
        halfwayCuePlayed = phase == RuntimePhase.WORK &&
            session.elapsedSecondsInPhase >= item.workSeconds / 2
        activeSecondsTotal = restored.items.take(itemIndex).sumOf { it.workSeconds + it.restAfterSeconds } +
            session.elapsedSecondsInPhase +
            if (phase == RuntimePhase.REST) item.workSeconds else 0
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (isActive) {
                delay(TICK_MILLIS)
                tick()
            }
        }
    }

    private suspend fun tick() {
        if (isPaused) return
        val elapsedSeconds = elapsedInPhase().inWholeSeconds
        maybePlayHalfwayCue(elapsedSeconds)
        if (elapsedSeconds >= phaseDurationSeconds()) {
            advancePhase(playCue = true)
            return
        }
        if (elapsedSeconds != lastRenderedSecond) {
            lastRenderedSecond = elapsedSeconds
            persistSession()
            render()
        }
    }

    private fun elapsedInPhase(): Duration =
        phaseElapsedBanked + (resumeMark?.elapsedNow() ?: Duration.ZERO)

    private fun phaseDurationSeconds(): Int {
        val item = workout?.items?.getOrNull(itemIndex) ?: return 0
        return when (phase) {
            RuntimePhase.PREPARE -> PREPARE_SECONDS
            RuntimePhase.WORK -> item.workSeconds
            RuntimePhase.REST -> item.restAfterSeconds
        }
    }

    private fun maybePlayHalfwayCue(elapsedSeconds: Long) {
        val item = workout?.items?.getOrNull(itemIndex) ?: return
        if (
            item.isUnilateral &&
            phase == RuntimePhase.WORK &&
            !halfwayCuePlayed &&
            elapsedSeconds >= item.workSeconds / 2
        ) {
            halfwayCuePlayed = true
            cuePlayer.playHalfwayCue()
        }
    }

    private suspend fun advancePhase(playCue: Boolean) {
        if (_state.value.isComplete) return
        val currentWorkout = workout ?: return
        val item = currentWorkout.items.getOrNull(itemIndex) ?: return
        val phaseSeconds = elapsedInPhase().inWholeSeconds
            .coerceAtMost(phaseDurationSeconds().toLong())
            .toInt()

        var completed = false
        when (phase) {
            RuntimePhase.PREPARE -> phase = RuntimePhase.WORK
            RuntimePhase.WORK -> {
                activeSecondsTotal += phaseSeconds
                if (item.restAfterSeconds > 0) {
                    phase = RuntimePhase.REST
                } else {
                    completed = !moveToNextItem(currentWorkout)
                }
            }
            RuntimePhase.REST -> {
                activeSecondsTotal += phaseSeconds
                completed = !moveToNextItem(currentWorkout)
            }
        }
        if (completed) {
            complete()
            return
        }

        phaseElapsedBanked = Duration.ZERO
        resumeMark = if (isPaused) null else timeSource.markNow()
        halfwayCuePlayed = false
        lastRenderedSecond = -1
        if (playCue) cuePlayer.playIntervalCue()
        persistSession()
        render()
    }

    private fun moveToNextItem(currentWorkout: DailyWorkout): Boolean {
        val nextIndex = itemIndex + 1
        if (nextIndex >= currentWorkout.items.size) return false
        itemIndex = nextIndex
        phase = RuntimePhase.WORK
        return true
    }

    private fun pause() {
        if (isPaused) return
        phaseElapsedBanked = elapsedInPhase()
        resumeMark = null
        isPaused = true
        ticker?.cancel()
        viewModelScope.launch { persistSession() }
        render()
    }

    private fun resume() {
        if (!isPaused) return
        resumeMark = timeSource.markNow()
        isPaused = false
        startTicker()
        viewModelScope.launch { persistSession() }
        render()
    }

    private fun pauseFromBackground() {
        val current = _state.value
        if (!current.isLoading && current.error == null && !current.isComplete) pause()
    }

    private fun skipPhase() {
        viewModelScope.launch { advancePhase(playCue = false) }
    }

    private fun showQuitConfirmation() {
        pause()
        _state.update { it.copy(showQuitConfirmation = true) }
    }

    private fun quitWorkout() {
        ticker?.cancel()
        viewModelScope.launch {
            runCatching { workoutRepository.clearActiveSession() }
            _events.send(ActiveWorkoutEvent.NavigateBack)
        }
    }

    private fun complete() {
        ticker?.cancel()
        viewModelScope.launch { runCatching { workoutRepository.clearActiveSession() } }
        cuePlayer.playCompleteCue()
        _state.update {
            it.copy(
                isLoading = false,
                isComplete = true,
                showQuitConfirmation = false,
                completedMinutes = (activeSecondsTotal / 60.0).roundToInt().coerceAtLeast(1),
            )
        }
    }

    private fun saveFeedback(feedback: DifficultyFeedback) {
        val completedWorkout = workout ?: return
        if (_state.value.isSavingFeedback) return
        viewModelScope.launch {
            _state.update { it.copy(isSavingFeedback = true, feedbackError = null) }
            val result = completeWorkout(
                workout = completedWorkout,
                feedback = feedback,
                durationSeconds = activeSecondsTotal,
            )
            when (result) {
                is Result.Success -> _events.send(ActiveWorkoutEvent.NavigateBack)
                is Result.Error -> _state.update {
                    it.copy(
                        isSavingFeedback = false,
                        feedbackError = "Could not save your feedback. Please try again.",
                    )
                }
            }
        }
    }

    private suspend fun persistSession() {
        val currentWorkout = workout ?: return
        if (_state.value.isComplete) return
        val session = ActiveWorkoutSession(
            workoutId = currentWorkout.id,
            currentItemIndex = itemIndex,
            phase = if (phase == RuntimePhase.REST) TimerPhase.REST else TimerPhase.WORK,
            elapsedSecondsInPhase = if (phase == RuntimePhase.PREPARE) {
                0
            } else {
                elapsedInPhase().inWholeSeconds.toInt()
            },
            isPaused = isPaused,
            updatedAtEpochMillis = clockProvider.nowEpochMillis(),
        )
        runCatching { workoutRepository.saveActiveSession(session) }
    }

    private fun render() {
        val currentWorkout = workout ?: return
        val item = currentWorkout.items.getOrNull(itemIndex) ?: return
        val info = exercises[item.exerciseId]
        val duration = phaseDurationSeconds()
        val elapsedSeconds = elapsedInPhase().inWholeSeconds.coerceAtMost(duration.toLong()).toInt()
        val nextItem = currentWorkout.items.getOrNull(itemIndex + 1)

        _state.update {
            it.copy(
                isLoading = false,
                error = null,
                phase = when (phase) {
                    RuntimePhase.PREPARE -> WorkoutPhaseUi.PREPARE
                    RuntimePhase.WORK -> WorkoutPhaseUi.WORK
                    RuntimePhase.REST -> WorkoutPhaseUi.REST
                },
                exerciseName = info?.name ?: "Unknown exercise",
                exerciseDescription = info?.description.orEmpty(),
                remainingSeconds = duration - elapsedSeconds,
                phaseTotalSeconds = duration.coerceAtLeast(1),
                itemPosition = itemIndex + 1,
                itemCount = currentWorkout.items.size,
                nextExerciseName = nextItem?.let { next ->
                    exercises[next.exerciseId]?.name ?: "Unknown exercise"
                },
                isPaused = isPaused,
                showSwitchSides = item.isUnilateral &&
                    phase == RuntimePhase.WORK &&
                    elapsedSeconds >= item.workSeconds / 2,
            )
        }
    }

    private enum class RuntimePhase {
        PREPARE,
        WORK,
        REST,
    }

    private data class ExerciseInfo(
        val name: String,
        val description: String,
    )

    private companion object {
        const val TICK_MILLIS = 200L
        const val PREPARE_SECONDS = 3
    }
}
