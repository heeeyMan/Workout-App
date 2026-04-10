package com.workout.android.di

import com.workout.android.ui.createworkout.CreateWorkoutViewModel
import com.workout.android.ui.home.HomeViewModel
import com.workout.android.ui.timer.TimerViewModel
import com.workout.android.ui.workoutlist.WorkoutListViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val androidModule = module {
    viewModel { HomeViewModel(get()) }
    viewModel { WorkoutListViewModel(get()) }
    viewModel { CreateWorkoutViewModel(get()) }
    viewModel { params -> TimerViewModel(get(), params.get()) }
}
