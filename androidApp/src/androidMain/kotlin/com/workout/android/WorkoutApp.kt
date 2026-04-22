package com.workout.android

import android.app.Application
import com.workout.core.database.DatabaseDriverFactory
import com.workout.core.di.coreModule
import com.workout.shared.di.androidPlatformModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class WorkoutApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@WorkoutApp)
            modules(
                coreModule(driverFactory = DatabaseDriverFactory(this@WorkoutApp)),
                androidPlatformModule
            )
        }
    }
}
