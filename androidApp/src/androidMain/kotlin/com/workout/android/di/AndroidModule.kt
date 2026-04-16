package com.workout.android.di

import com.workout.android.data.TimerPreferences
import com.workout.android.timer.TimerSessionBridge
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidModule = module {
    single { TimerPreferences(androidContext()) }
    single { TimerSessionBridge() }
}
