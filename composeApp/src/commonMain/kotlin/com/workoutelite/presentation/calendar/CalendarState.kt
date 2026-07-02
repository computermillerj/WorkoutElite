package com.workoutelite.presentation.calendar

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Stable
data class CalendarState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val monthLabel: String = "",
    val canGoToNextMonth: Boolean = false,
    val weeks: List<List<CalendarCellUi?>> = emptyList(),
    val selectedDay: WorkoutDayUi? = null,
    val totalWorkouts: Int = 0,
    val streakDays: Int = 0,
)

data class CalendarCellUi(
    val date: String,
    val dayNumber: Int,
    val isToday: Boolean,
    val isFuture: Boolean,
    val isSelected: Boolean,
    val workoutCount: Int,
) {
    val isCompleted: Boolean
        get() = workoutCount > 0
}

@Immutable
data class WorkoutDayUi(
    val date: String,
    val dateLabel: String,
    val workouts: List<CompletedWorkoutUi>,
)

@Immutable
data class CompletedWorkoutUi(
    val title: String,
    val feedbackLabel: String,
    val durationMinutes: Int,
    val exerciseNames: List<String>,
)

sealed interface CalendarAction {
    data object OnRetryClick : CalendarAction
    data object OnScreenResumed : CalendarAction
    data object OnPreviousMonthClick : CalendarAction
    data object OnNextMonthClick : CalendarAction
    data class OnDayClick(val date: String) : CalendarAction
}
