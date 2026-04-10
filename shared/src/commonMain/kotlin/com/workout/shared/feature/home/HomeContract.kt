package com.workout.shared.feature.home

import com.workout.core.model.Workout

data class HomeState(
    val lastWorkout: Workout? = null,
    val isLoading: Boolean = true
)

sealed interface HomeIntent {
    data object LoadLastWorkout : HomeIntent
    data class StartWorkout(val workoutId: Long) : HomeIntent
    data object CreateWorkout : HomeIntent
    data object OpenWorkoutList : HomeIntent
}

sealed interface HomeEffect {
    data class NavigateToTimer(val workoutId: Long) : HomeEffect
    data object NavigateToCreateWorkout : HomeEffect
    data object NavigateToWorkoutList : HomeEffect
}
