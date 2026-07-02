package com.workoutelite.domain.workout

import com.workoutelite.domain.common.AppError

data class DailyWorkout(
    val id: String,
    val localDate: String,
    val sequence: Int,
    val origin: WorkoutOrigin,
    val targetDifficultyScore: Double,
    val estimatedDurationSeconds: Int,
    val items: List<WorkoutItem>,
    val createdAtEpochMillis: Long,
)

data class WorkoutItem(
    val exerciseId: String,
    val order: Int,
    val workSeconds: Int,
    val restAfterSeconds: Int,
    val isUnilateral: Boolean,
)

data class CompletedWorkout(
    val workoutId: String,
    val localDate: String,
    val completedAtEpochMillis: Long,
    val feedback: DifficultyFeedback,
    val durationSeconds: Int,
)

data class ActiveWorkoutSession(
    val workoutId: String,
    val currentItemIndex: Int,
    val phase: TimerPhase,
    val elapsedSecondsInPhase: Int,
    val isPaused: Boolean,
    val updatedAtEpochMillis: Long,
)

enum class WorkoutOrigin {
    AUTO_DAILY,
    ON_DEMAND,
}

enum class DifficultyFeedback {
    EASY,
    MEDIUM,
    HARD,
}

enum class TimerPhase {
    WORK,
    REST,
}

enum class WorkoutError : AppError {
    NO_ELIGIBLE_EXERCISES,
    DATABASE_UNAVAILABLE,
}
