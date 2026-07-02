package com.workoutelite.presentation.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutelite.domain.common.Result
import com.workoutelite.domain.exercise.ExerciseRepository
import com.workoutelite.domain.workout.CreateOnDemandWorkoutUseCase
import com.workoutelite.domain.workout.DailyWorkout
import com.workoutelite.domain.workout.GetTodayOverviewUseCase
import com.workoutelite.domain.workout.StartWorkoutSessionUseCase
import com.workoutelite.domain.workout.TodayOverview
import com.workoutelite.domain.workout.WorkoutError
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char

class TodayWorkoutViewModel(
    private val getTodayOverview: GetTodayOverviewUseCase,
    private val createOnDemandWorkout: CreateOnDemandWorkoutUseCase,
    private val startWorkoutSession: StartWorkoutSessionUseCase,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(TodayWorkoutState())
    val state = _state.asStateFlow()

    private val _events = Channel<TodayWorkoutEvent>()
    val events = _events.receiveAsFlow()

    private var overview: TodayOverview? = null
    private var loadJob: Job? = null

    fun onAction(action: TodayWorkoutAction) {
        when (action) {
            TodayWorkoutAction.OnRetryClick -> loadOverview(showLoading = true)
            TodayWorkoutAction.OnScreenResumed -> loadOverview(showLoading = _state.value.workout == null)
            TodayWorkoutAction.OnPrimaryClick -> startOrResume()
            TodayWorkoutAction.OnStartBonusClick -> startBonusWorkout()
        }
    }

    private fun loadOverview(showLoading: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (showLoading) _state.update { it.copy(isLoading = true, error = null) }
            when (val result = getTodayOverview()) {
                is Result.Error -> _state.update {
                    it.copy(isLoading = false, error = result.error.toMessage())
                }
                is Result.Success -> {
                    overview = result.data
                    val summary = result.data.workout.toSummaryUi()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = null,
                            workout = summary,
                            completedCount = result.data.completions.size,
                            isAutoCompleted = result.data.isAutoWorkoutCompleted,
                            hasActiveSession = result.data.activeSession != null,
                        )
                    }
                }
            }
        }
    }

    private fun startOrResume() {
        val current = overview ?: return
        if (_state.value.isStartingWorkout) return
        viewModelScope.launch {
            if (current.activeSession != null) {
                _events.send(TodayWorkoutEvent.NavigateToActiveWorkout)
                return@launch
            }
            _state.update { it.copy(isStartingWorkout = true, actionError = null) }
            when (startWorkoutSession(current.workout.id)) {
                is Result.Success -> {
                    _state.update { it.copy(isStartingWorkout = false) }
                    _events.send(TodayWorkoutEvent.NavigateToActiveWorkout)
                }
                is Result.Error -> _state.update {
                    it.copy(
                        isStartingWorkout = false,
                        actionError = "Could not start the workout. Please try again.",
                    )
                }
            }
        }
    }

    private fun startBonusWorkout() {
        if (_state.value.isStartingWorkout) return
        viewModelScope.launch {
            _state.update { it.copy(isStartingWorkout = true, actionError = null) }
            when (val created = createOnDemandWorkout()) {
                is Result.Error -> _state.update {
                    it.copy(isStartingWorkout = false, actionError = created.error.toMessage())
                }
                is Result.Success -> when (startWorkoutSession(created.data.id)) {
                    is Result.Success -> {
                        _state.update { it.copy(isStartingWorkout = false) }
                        _events.send(TodayWorkoutEvent.NavigateToActiveWorkout)
                    }
                    is Result.Error -> _state.update {
                        it.copy(
                            isStartingWorkout = false,
                            actionError = "Could not start the workout. Please try again.",
                        )
                    }
                }
            }
        }
    }

    private fun WorkoutError.toMessage(): String = when (this) {
        WorkoutError.NO_ELIGIBLE_EXERCISES ->
            "Turn on at least one exercise in the Exercises tab to generate a workout."
        WorkoutError.DATABASE_UNAVAILABLE ->
            "Workout data is unavailable. Please try again."
    }

    private suspend fun DailyWorkout.toSummaryUi(): WorkoutSummaryUi {
        val exerciseLookup = try {
            exerciseRepository
                .getExercisesByIds(items.map { it.exerciseId })
                .associateBy { it.id }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            emptyMap()
        }
        return WorkoutSummaryUi(
            dateLabel = formatDateLabel(localDate),
            exerciseCount = items.size,
            estimatedMinutes = (estimatedDurationSeconds / 60.0).roundToInt(),
            difficultyLabel = ((targetDifficultyScore * 10).roundToInt() / 10.0).toString(),
            exercises = items.map { item ->
                WorkoutExercisePreviewUi(
                    name = exerciseLookup[item.exerciseId]?.name ?: "Unknown exercise",
                    workSeconds = item.workSeconds,
                    isUnilateral = item.isUnilateral,
                )
            },
        )
    }

    private fun formatDateLabel(isoDate: String): String = try {
        LocalDate.parse(isoDate).format(DATE_LABEL_FORMAT)
    } catch (_: IllegalArgumentException) {
        isoDate
    }

    private companion object {
        val DATE_LABEL_FORMAT = LocalDate.Format {
            dayOfWeek(DayOfWeekNames.ENGLISH_FULL)
            chars(", ")
            monthName(MonthNames.ENGLISH_FULL)
            char(' ')
            day(padding = Padding.NONE)
        }
    }
}
