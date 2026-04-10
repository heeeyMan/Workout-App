package com.workout.core.di

import com.workout.core.database.DatabaseDriverFactory
import com.workout.core.database.WorkoutDatabase
import com.workout.core.repository.WorkoutRepository
import com.workout.core.repository.WorkoutRepositoryImpl
import org.koin.dsl.module

fun coreModule(driverFactory: DatabaseDriverFactory) = module {
    single<WorkoutDatabase> {
        WorkoutDatabase(driverFactory.createDriver())
    }
    single<WorkoutRepository> {
        WorkoutRepositoryImpl(get())
    }
}
