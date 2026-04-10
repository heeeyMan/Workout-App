package com.workout.android.di

import com.workout.android.data.TimerPreferences
import com.workout.android.timer.TimerSessionBridge
import com.workout.android.ui.createworkout.CreateWorkoutViewModel
import com.workout.android.ui.home.HomeViewModel
import com.workout.android.ui.settings.SettingsViewModel
import com.workout.android.ui.timer.TimerViewModel
import com.workout.android.ui.workoutlist.WorkoutListViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val androidModule = module {
    single { TimerPreferences(androidContext()) }
    single { TimerSessionBridge() }
    viewModel { HomeViewModel(get()) }
    viewModel { WorkoutListViewModel(get()) }
    viewModel { CreateWorkoutViewModel(get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { params -> TimerViewModel(get(), get(), androidContext(), get(), params.get()) }
}
