package com.workoutelite.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val category: String,
    val movementPattern: String,
    val equipment: String,
    val difficulty: Int,
    val defaultDurationSeconds: Int,
    val isUnilateral: Boolean,
    val demoAssetPath: String?,
    val assetSourceUrl: String?,
    val assetLicense: String?,
    val assetAttribution: String?,
    val isActive: Boolean,
)

@Entity(
    tableName = "exercise_preferences",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
        ),
    ],
    indices = [Index("exerciseId")],
)
data class ExercisePreferenceEntity(
    @PrimaryKey val exerciseId: String,
    val frequency: Int,
)

@Entity(
    tableName = "daily_workouts",
    indices = [Index(value = ["localDate", "sequence"], unique = true)],
)
data class DailyWorkoutEntity(
    @PrimaryKey val id: String,
    val localDate: String,
    val sequence: Int,
    val origin: String,
    val targetDifficultyScore: Double,
    val estimatedDurationSeconds: Int,
    val createdAtEpochMillis: Long,
)

@Entity(
    tableName = "daily_workout_items",
    foreignKeys = [
        ForeignKey(
            entity = DailyWorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("workoutId")],
    primaryKeys = ["workoutId", "order"],
)
data class DailyWorkoutItemEntity(
    val workoutId: String,
    val order: Int,
    val exerciseId: String,
    val workSeconds: Int,
    val restAfterSeconds: Int,
    val isUnilateral: Boolean,
)

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: String = "settings",
    val targetDurationMinutes: Int,
)

@Entity(tableName = "weekly_or_global_difficulty_profile")
data class DifficultyProfileEntity(
    @PrimaryKey val id: String = "global",
    val rollingScore: Double,
    val updatedAtEpochMillis: Long,
)

@Entity(tableName = "active_workout_sessions")
data class ActiveWorkoutSessionEntity(
    @PrimaryKey val id: String = "active",
    val workoutId: String,
    val currentItemIndex: Int,
    val phase: String,
    val elapsedSecondsInPhase: Int,
    val isPaused: Boolean,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "completed_workouts",
    foreignKeys = [
        ForeignKey(
            entity = DailyWorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
        ),
    ],
    indices = [Index("workoutId")],
)
data class CompletedWorkoutEntity(
    @PrimaryKey val workoutId: String,
    val localDate: String,
    val completedAtEpochMillis: Long,
    val feedback: String,
    val durationSeconds: Int,
)
