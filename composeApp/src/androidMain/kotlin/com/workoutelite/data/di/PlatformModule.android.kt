package com.workoutelite.data.di

import com.workoutelite.data.database.DatabaseFactory
import com.workoutelite.platform.AndroidWorkoutCuePlayer
import com.workoutelite.platform.WorkoutCuePlayer
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule = module {
    single { DatabaseFactory(androidContext()) }
    single<WorkoutCuePlayer> { AndroidWorkoutCuePlayer(androidContext()) }
}
