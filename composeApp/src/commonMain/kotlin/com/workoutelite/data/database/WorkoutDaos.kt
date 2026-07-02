package com.workoutelite.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY name")
    suspend fun getExercises(): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE isActive = 1 ORDER BY name")
    suspend fun getActiveExercises(): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE id IN (:ids)")
    suspend fun getExercisesByIds(ids: List<String>): List<ExerciseEntity>

    @Upsert
    suspend fun upsertAll(exercises: List<ExerciseEntity>)
}

@Dao
interface ExercisePreferenceDao {
    @Query("SELECT * FROM exercise_preferences")
    suspend fun getAll(): List<ExercisePreferenceEntity>

    @Upsert
    suspend fun upsert(preference: ExercisePreferenceEntity)
}

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM daily_workouts WHERE localDate = :localDate AND sequence = 0")
    suspend fun getAutoWorkout(localDate: String): DailyWorkoutEntity?

    @Query("SELECT * FROM daily_workouts WHERE id = :id")
    suspend fun getWorkoutById(id: String): DailyWorkoutEntity?

    @Query("SELECT * FROM daily_workouts WHERE id IN (:ids)")
    suspend fun getWorkoutsByIds(ids: List<String>): List<DailyWorkoutEntity>

    @Query("SELECT COALESCE(MAX(sequence) + 1, 0) FROM daily_workouts WHERE localDate = :localDate")
    suspend fun getNextSequence(localDate: String): Int

    @Query("SELECT * FROM daily_workout_items WHERE workoutId = :workoutId ORDER BY `order`")
    suspend fun getItems(workoutId: String): List<DailyWorkoutItemEntity>

    @Query("SELECT * FROM daily_workout_items WHERE workoutId IN (:workoutIds) ORDER BY `order`")
    suspend fun getItemsForWorkouts(workoutIds: List<String>): List<DailyWorkoutItemEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertWorkout(workout: DailyWorkoutEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<DailyWorkoutItemEntity>)

    @Transaction
    suspend fun insertWorkoutWithItems(
        workout: DailyWorkoutEntity,
        items: List<DailyWorkoutItemEntity>,
    ) {
        insertWorkout(workout)
        insertItems(items)
    }
}

@Dao
interface DifficultyDao {
    @Query("SELECT * FROM weekly_or_global_difficulty_profile WHERE id = 'global'")
    suspend fun getProfile(): DifficultyProfileEntity?

    @Upsert
    suspend fun upsert(profile: DifficultyProfileEntity)
}

@Dao
interface CompletedWorkoutDao {
    @Query("SELECT * FROM completed_workouts ORDER BY completedAtEpochMillis DESC")
    suspend fun getCompletedWorkouts(): List<CompletedWorkoutEntity>

    @Query("SELECT * FROM completed_workouts WHERE localDate = :localDate ORDER BY completedAtEpochMillis")
    suspend fun getCompletedWorkoutsForDate(localDate: String): List<CompletedWorkoutEntity>

    @Upsert
    suspend fun upsert(completedWorkout: CompletedWorkoutEntity)
}

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 'settings'")
    suspend fun getSettings(): UserSettingsEntity?

    @Upsert
    suspend fun upsert(settings: UserSettingsEntity)
}

@Dao
interface ActiveWorkoutSessionDao {
    @Query("SELECT * FROM active_workout_sessions WHERE id = 'active'")
    suspend fun getActiveSession(): ActiveWorkoutSessionEntity?

    @Upsert
    suspend fun upsert(session: ActiveWorkoutSessionEntity)

    @Query("DELETE FROM active_workout_sessions WHERE id = 'active'")
    suspend fun clear()
}
