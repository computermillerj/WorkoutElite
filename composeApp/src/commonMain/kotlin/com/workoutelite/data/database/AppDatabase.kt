package com.workoutelite.data.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(
    entities = [
        ExerciseEntity::class,
        ExercisePreferenceEntity::class,
        DailyWorkoutEntity::class,
        DailyWorkoutItemEntity::class,
        DifficultyProfileEntity::class,
        ActiveWorkoutSessionEntity::class,
        CompletedWorkoutEntity::class,
        UserSettingsEntity::class,
    ],
    version = 1,
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun exercisePreferenceDao(): ExercisePreferenceDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun difficultyDao(): DifficultyDao
    abstract fun completedWorkoutDao(): CompletedWorkoutDao
    abstract fun activeWorkoutSessionDao(): ActiveWorkoutSessionDao
    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
        const val DATABASE_NAME = "workoutelite.db"
    }
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
