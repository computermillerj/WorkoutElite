package com.workoutelite.data.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.workoutelite.data.database.AppDatabase
import com.workoutelite.data.database.DatabaseFactory
import com.workoutelite.data.workout.RoomExerciseRepository
import com.workoutelite.data.workout.RoomSettingsRepository
import com.workoutelite.data.workout.RoomWorkoutRepository
import com.workoutelite.domain.exercise.ExerciseRepository
import com.workoutelite.domain.settings.SettingsRepository
import com.workoutelite.domain.workout.CompleteWorkoutUseCase
import com.workoutelite.domain.workout.CreateOnDemandWorkoutUseCase
import com.workoutelite.domain.workout.GetOrCreateDailyWorkoutUseCase
import com.workoutelite.domain.workout.GetTodayOverviewUseCase
import com.workoutelite.domain.workout.StartWorkoutSessionUseCase
import com.workoutelite.domain.workout.WorkoutGenerator
import com.workoutelite.domain.workout.WorkoutRepository
import com.workoutelite.platform.ClockProvider
import com.workoutelite.presentation.calendar.CalendarViewModel
import com.workoutelite.presentation.exercises.ExerciseListViewModel
import com.workoutelite.presentation.workout.ActiveWorkoutViewModel
import com.workoutelite.presentation.workout.TodayWorkoutViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

expect val platformModule: Module

val appModule = module {
    includes(platformModule)

    single<AppDatabase> {
        get<DatabaseFactory>()
            .create()
            .setDriver(BundledSQLiteDriver())
            .build()
    }
    single { get<AppDatabase>().exerciseDao() }
    single { get<AppDatabase>().exercisePreferenceDao() }
    single { get<AppDatabase>().workoutDao() }
    single { get<AppDatabase>().difficultyDao() }
    single { get<AppDatabase>().completedWorkoutDao() }
    single { get<AppDatabase>().activeWorkoutSessionDao() }
    single { get<AppDatabase>().userSettingsDao() }

    single<ExerciseRepository> { RoomExerciseRepository(get(), get()) }
    single<WorkoutRepository> { RoomWorkoutRepository(get(), get(), get(), get()) }
    single<SettingsRepository> { RoomSettingsRepository(get()) }
    single { WorkoutGenerator() }
    single { ClockProvider() }
    single { GetOrCreateDailyWorkoutUseCase(get(), get(), get(), get(), get()) }
    single { CreateOnDemandWorkoutUseCase(get(), get(), get(), get(), get()) }
    single { GetTodayOverviewUseCase(get(), get()) }
    single { StartWorkoutSessionUseCase(get(), get()) }
    single { CompleteWorkoutUseCase(get(), get()) }
    viewModelOf(::TodayWorkoutViewModel)
    viewModelOf(::ActiveWorkoutViewModel)
    viewModelOf(::ExerciseListViewModel)
    viewModelOf(::CalendarViewModel)
}
