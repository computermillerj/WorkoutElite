package com.workoutelite.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutelite.domain.exercise.ExerciseRepository
import com.workoutelite.domain.workout.CompletedWorkout
import com.workoutelite.domain.workout.DailyWorkout
import com.workoutelite.domain.workout.DifficultyFeedback
import com.workoutelite.domain.workout.WorkoutOrigin
import com.workoutelite.domain.workout.WorkoutRepository
import com.workoutelite.platform.ClockProvider
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

class CalendarViewModel(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val clockProvider: ClockProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(CalendarState())
    val state = _state.asStateFlow()

    private var completionsByDate = emptyMap<String, List<CompletedWorkout>>()
    private var workoutsById = emptyMap<String, DailyWorkout>()
    private var exerciseNames = emptyMap<String, String>()
    private var rollingDifficultyScore = 0.0
    private var hasActiveSession = false
    private var displayedMonth: LocalDate = today().firstOfMonth()
    private var selectedDate: String? = null
    private var loadJob: Job? = null

    fun onAction(action: CalendarAction) {
        when (action) {
            CalendarAction.OnRetryClick -> loadHistory(showLoading = true)
            CalendarAction.OnScreenResumed -> loadHistory(showLoading = completionsByDate.isEmpty())
            CalendarAction.OnPreviousMonthClick -> moveMonth(byMonths = -1)
            CalendarAction.OnNextMonthClick -> moveMonth(byMonths = 1)
            is CalendarAction.OnDayClick -> selectDay(action.date)
        }
    }

    private fun loadHistory(showLoading: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (showLoading) _state.update { it.copy(isLoading = true, error = null) }
            try {
                val completions = workoutRepository.getCompletedWorkouts()
                completionsByDate = completions.groupBy { it.localDate }
                workoutsById = workoutRepository
                    .getWorkoutsByIds(completions.map { it.workoutId }.distinct())
                    .associateBy { it.id }
                exerciseNames = exerciseRepository
                    .getExercisesByIds(
                        workoutsById.values
                            .flatMap { workout -> workout.items.map { it.exerciseId } }
                            .distinct(),
                    )
                    .associate { it.id to it.name }
                rollingDifficultyScore = workoutRepository.getRollingDifficultyScore()
                hasActiveSession = workoutRepository.getActiveSession() != null
                render(isLoading = false)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Workout history is unavailable. Please try again.",
                    )
                }
            }
        }
    }

    private fun moveMonth(byMonths: Int) {
        val moved = displayedMonth.plus(byMonths, DateTimeUnit.MONTH)
        if (moved > today().firstOfMonth()) return
        displayedMonth = moved
        render(isLoading = false)
    }

    private fun selectDay(date: String) {
        selectedDate = if (selectedDate == date) null else date
        render(isLoading = false)
    }

    private fun render(isLoading: Boolean) {
        val today = today()
        val allCompletions = completionsByDate.values.flatten()
        _state.update {
            it.copy(
                isLoading = isLoading,
                error = null,
                monthLabel = displayedMonth.format(MONTH_LABEL_FORMAT),
                canGoToNextMonth = displayedMonth < today.firstOfMonth(),
                weeks = buildWeeks(today),
                selectedDay = selectedDate?.let(::buildDayDetail),
                today = todayPeriod(today),
                week = weekPeriod(today),
                totalWorkouts = allCompletions.size,
                averageMinutes = if (allCompletions.isEmpty()) {
                    0
                } else {
                    (allCompletions.sumOf { completion -> completion.durationSeconds } /
                        60.0 / allCompletions.size).roundToInt()
                },
                streakDays = currentStreak(today),
                rollingDifficulty = ((rollingDifficultyScore * 10).roundToInt() / 10.0).toString(),
                activeSessionLabel = if (hasActiveSession) {
                    "Workout in progress"
                } else {
                    "No workout in progress"
                },
            )
        }
    }

    private fun todayPeriod(today: LocalDate): HistoryPeriodUi =
        completionsByDate[today.toString()].orEmpty().toPeriodUi("Today")

    private fun weekPeriod(today: LocalDate): HistoryPeriodUi {
        val weekStart = today.minus(today.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
        val weekCompletions = completionsByDate
            .filterKeys { date ->
                try {
                    LocalDate.parse(date) in weekStart..today
                } catch (_: IllegalArgumentException) {
                    false
                }
            }
            .values
            .flatten()
        return weekCompletions.toPeriodUi("This week")
    }

    private fun List<CompletedWorkout>.toPeriodUi(label: String) = HistoryPeriodUi(
        label = label,
        workouts = size,
        minutes = (sumOf { it.durationSeconds } / 60.0).roundToInt(),
    )

    private fun buildWeeks(today: LocalDate): List<List<CalendarCellUi?>> {
        val daysInMonth = displayedMonth
            .plus(1, DateTimeUnit.MONTH)
            .minus(1, DateTimeUnit.DAY)
            .day
        val leadingBlanks = displayedMonth.dayOfWeek.isoDayNumber - 1
        val cells: List<CalendarCellUi?> = List(leadingBlanks) { null } +
            (1..daysInMonth).map { dayNumber ->
                val date = LocalDate(displayedMonth.year, displayedMonth.month, dayNumber)
                val isoDate = date.toString()
                CalendarCellUi(
                    date = isoDate,
                    dayNumber = dayNumber,
                    isToday = date == today,
                    isFuture = date > today,
                    isSelected = isoDate == selectedDate,
                    workoutCount = completionsByDate[isoDate]?.size ?: 0,
                )
            }
        return cells.chunked(DAYS_PER_WEEK).map { week ->
            week + List(DAYS_PER_WEEK - week.size) { null }
        }
    }

    private fun buildDayDetail(date: String): WorkoutDayUi? {
        val completions = completionsByDate[date] ?: return null
        val dateLabel = try {
            LocalDate.parse(date).format(DAY_LABEL_FORMAT)
        } catch (_: IllegalArgumentException) {
            date
        }
        return WorkoutDayUi(
            date = date,
            dateLabel = dateLabel,
            workouts = completions.map { completion ->
                val workout = workoutsById[completion.workoutId]
                CompletedWorkoutUi(
                    title = when (workout?.origin) {
                        WorkoutOrigin.ON_DEMAND -> "Bonus workout"
                        else -> "Daily workout"
                    },
                    feedbackLabel = completion.feedback.toLabel(),
                    durationMinutes = (completion.durationSeconds / 60.0).roundToInt(),
                    exerciseNames = workout?.items
                        ?.map { item -> exerciseNames[item.exerciseId] ?: "Removed exercise" }
                        .orEmpty(),
                )
            },
        )
    }

    private fun currentStreak(today: LocalDate): Int {
        var streak = 0
        var cursor = if (completionsByDate.containsKey(today.toString())) {
            today
        } else {
            today.minus(1, DateTimeUnit.DAY)
        }
        while (completionsByDate.containsKey(cursor.toString())) {
            streak++
            cursor = cursor.minus(1, DateTimeUnit.DAY)
        }
        return streak
    }

    private fun today(): LocalDate = LocalDate.parse(clockProvider.todayString())

    private fun LocalDate.firstOfMonth(): LocalDate = LocalDate(year, month, 1)

    private fun DifficultyFeedback.toLabel(): String = when (this) {
        DifficultyFeedback.EASY -> "Easy"
        DifficultyFeedback.MEDIUM -> "Just right"
        DifficultyFeedback.HARD -> "Hard"
    }

    private companion object {
        const val DAYS_PER_WEEK = 7

        val MONTH_LABEL_FORMAT = LocalDate.Format {
            monthName(MonthNames.ENGLISH_FULL)
            char(' ')
            year()
        }

        val DAY_LABEL_FORMAT = LocalDate.Format {
            dayOfWeek(DayOfWeekNames.ENGLISH_FULL)
            chars(", ")
            monthName(MonthNames.ENGLISH_FULL)
            char(' ')
            day(padding = Padding.NONE)
        }
    }
}
