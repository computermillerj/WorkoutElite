package com.workoutelite.presentation.workout

import com.workoutelite.domain.workout.DifficultyFeedback

data class ActiveWorkoutState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val isComplete: Boolean = false,
    val isSavingFeedback: Boolean = false,
    val feedbackError: String? = null,
    val phase: WorkoutPhaseUi = WorkoutPhaseUi.PREPARE,
    val exerciseName: String = "",
    val exerciseDescription: String = "",
    val remainingSeconds: Int = 0,
    val phaseTotalSeconds: Int = 1,
    val itemPosition: Int = 1,
    val itemCount: Int = 1,
    val nextExerciseName: String? = null,
    val isPaused: Boolean = false,
    val showSwitchSides: Boolean = false,
    val showQuitConfirmation: Boolean = false,
    val completedMinutes: Int = 0,
)

enum class WorkoutPhaseUi {
    PREPARE,
    WORK,
    REST,
}

sealed interface ActiveWorkoutAction {
    data object OnPauseResumeClick : ActiveWorkoutAction
    data object OnSkipClick : ActiveWorkoutAction
    data object OnQuitClick : ActiveWorkoutAction
    data object OnConfirmQuitClick : ActiveWorkoutAction
    data object OnDismissQuitClick : ActiveWorkoutAction
    data object OnBackgrounded : ActiveWorkoutAction
    data object OnSkipFeedbackClick : ActiveWorkoutAction
    data class OnFeedbackClick(val feedback: DifficultyFeedback) : ActiveWorkoutAction
}

sealed interface ActiveWorkoutEvent {
    data object NavigateBack : ActiveWorkoutEvent
}
