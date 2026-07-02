package com.workoutelite.data.workout

import com.workoutelite.data.database.ActiveWorkoutSessionEntity
import com.workoutelite.data.database.CompletedWorkoutEntity
import com.workoutelite.data.database.DailyWorkoutEntity
import com.workoutelite.data.database.DailyWorkoutItemEntity
import com.workoutelite.data.database.ExerciseEntity
import com.workoutelite.domain.exercise.EquipmentRequirement
import com.workoutelite.domain.exercise.Exercise
import com.workoutelite.domain.exercise.ExerciseCategory
import com.workoutelite.domain.exercise.MovementPattern
import com.workoutelite.domain.workout.ActiveWorkoutSession
import com.workoutelite.domain.workout.CompletedWorkout
import com.workoutelite.domain.workout.DailyWorkout
import com.workoutelite.domain.workout.DifficultyFeedback
import com.workoutelite.domain.workout.TimerPhase
import com.workoutelite.domain.workout.WorkoutItem
import com.workoutelite.domain.workout.WorkoutOrigin

fun ExerciseEntity.toExercise() = Exercise(
    id = id,
    name = name,
    description = description,
    category = ExerciseCategory.valueOf(category),
    movementPattern = MovementPattern.valueOf(movementPattern),
    equipment = EquipmentRequirement.valueOf(equipment),
    difficulty = difficulty,
    defaultDurationSeconds = defaultDurationSeconds,
    isUnilateral = isUnilateral,
    demoAssetPath = demoAssetPath,
    assetSourceUrl = assetSourceUrl,
    assetLicense = assetLicense,
    assetAttribution = assetAttribution,
    isActive = isActive,
)

fun Exercise.toEntity() = ExerciseEntity(
    id = id,
    name = name,
    description = description,
    category = category.name,
    movementPattern = movementPattern.name,
    equipment = equipment.name,
    difficulty = difficulty,
    defaultDurationSeconds = defaultDurationSeconds,
    isUnilateral = isUnilateral,
    demoAssetPath = demoAssetPath,
    assetSourceUrl = assetSourceUrl,
    assetLicense = assetLicense,
    assetAttribution = assetAttribution,
    isActive = isActive,
)

fun DailyWorkout.toEntity() = DailyWorkoutEntity(
    id = id,
    localDate = localDate,
    sequence = sequence,
    origin = origin.name,
    targetDifficultyScore = targetDifficultyScore,
    estimatedDurationSeconds = estimatedDurationSeconds,
    createdAtEpochMillis = createdAtEpochMillis,
)

fun WorkoutItem.toEntity(workoutId: String) = DailyWorkoutItemEntity(
    workoutId = workoutId,
    order = order,
    exerciseId = exerciseId,
    workSeconds = workSeconds,
    restAfterSeconds = restAfterSeconds,
    isUnilateral = isUnilateral,
)

fun DailyWorkoutEntity.toWorkout(items: List<DailyWorkoutItemEntity>) = DailyWorkout(
    id = id,
    localDate = localDate,
    sequence = sequence,
    origin = WorkoutOrigin.valueOf(origin),
    targetDifficultyScore = targetDifficultyScore,
    estimatedDurationSeconds = estimatedDurationSeconds,
    items = items.map { it.toWorkoutItem() },
    createdAtEpochMillis = createdAtEpochMillis,
)

fun DailyWorkoutItemEntity.toWorkoutItem() = WorkoutItem(
    exerciseId = exerciseId,
    order = order,
    workSeconds = workSeconds,
    restAfterSeconds = restAfterSeconds,
    isUnilateral = isUnilateral,
)

fun CompletedWorkout.toEntity() = CompletedWorkoutEntity(
    workoutId = workoutId,
    localDate = localDate,
    completedAtEpochMillis = completedAtEpochMillis,
    feedback = feedback.name,
    durationSeconds = durationSeconds,
)

fun CompletedWorkoutEntity.toCompletedWorkout() = CompletedWorkout(
    workoutId = workoutId,
    localDate = localDate,
    completedAtEpochMillis = completedAtEpochMillis,
    feedback = DifficultyFeedback.valueOf(feedback),
    durationSeconds = durationSeconds,
)

fun ActiveWorkoutSession.toEntity() = ActiveWorkoutSessionEntity(
    workoutId = workoutId,
    currentItemIndex = currentItemIndex,
    phase = phase.name,
    elapsedSecondsInPhase = elapsedSecondsInPhase,
    isPaused = isPaused,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

fun ActiveWorkoutSessionEntity.toSession() = ActiveWorkoutSession(
    workoutId = workoutId,
    currentItemIndex = currentItemIndex,
    phase = TimerPhase.valueOf(phase),
    elapsedSecondsInPhase = elapsedSecondsInPhase,
    isPaused = isPaused,
    updatedAtEpochMillis = updatedAtEpochMillis,
)
