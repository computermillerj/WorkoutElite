package com.workoutelite

import android.app.Application
import com.workoutelite.data.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class WorkoutEliteApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@WorkoutEliteApplication)
            modules(appModule)
        }
    }
}
