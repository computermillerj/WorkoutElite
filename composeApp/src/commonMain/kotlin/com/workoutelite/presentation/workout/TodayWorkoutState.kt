package com.workoutelite.presentation.workout

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Stable
data class TodayWorkoutState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val workout: WorkoutSummaryUi? = null,
    val completedCount: Int = 0,
    val isAutoCompleted: Boolean = false,
    val hasActiveSession: Boolean = false,
    val isStartingWorkout: Boolean = false,
    val actionError: String? = null,
)

@Immutable
data class WorkoutSummaryUi(
    val dateLabel: String,
    val exerciseCount: Int,
    val estimatedMinutes: Int,
    val difficultyLabel: String,
    val exercises: List<WorkoutExercisePreviewUi>,
)

data class WorkoutExercisePreviewUi(
    val name: String,
    val workSeconds: Int,
    val isUnilateral: Boolean,
)

sealed interface TodayWorkoutAction {
    data object OnRetryClick : TodayWorkoutAction
    data object OnPrimaryClick : TodayWorkoutAction
    data object OnStartBonusClick : TodayWorkoutAction
    data object OnScreenResumed : TodayWorkoutAction
}

sealed interface TodayWorkoutEvent {
    data object NavigateToActiveWorkout : TodayWorkoutEvent
}
