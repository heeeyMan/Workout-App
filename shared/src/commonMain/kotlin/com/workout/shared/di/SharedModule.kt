package com.workout.shared.di

import com.workout.shared.feature.createworkout.CreateWorkoutStore
import com.workout.shared.feature.home.HomeStore
import com.workout.shared.feature.timer.TimerStore
import com.workout.shared.feature.workoutlist.WorkoutListStore
import org.koin.dsl.module

val sharedModule = module {
    factory { HomeStore(get()) }
    factory { WorkoutListStore(get()) }
    factory { CreateWorkoutStore(get()) }
    factory { TimerStore(get()) }
}
