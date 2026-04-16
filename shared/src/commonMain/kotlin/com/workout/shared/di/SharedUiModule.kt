package com.workout.shared.di

import org.koin.dsl.module

/**
 * Koin module that aggregates shared UI dependencies.
 * Includes [sharedModule] (MVI stores) so that Android only needs a single import.
 * WorkoutRepository is expected to come from coreModule.
 */
val sharedUiModule = module {
    includes(sharedModule)
}
