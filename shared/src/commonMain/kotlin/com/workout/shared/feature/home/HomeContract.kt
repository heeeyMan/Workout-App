package com.workout.shared.feature.home

import com.workout.core.model.Workout

data class HomeState(
    val workouts: List<Workout> = emptyList(),
    val isLoading: Boolean = true,
    val pendingDeleteId: Long? = null
)

sealed interface HomeIntent {
    data object LoadWorkouts : HomeIntent
    data class StartWorkout(val workoutId: Long) : HomeIntent
    data object CreateWorkout : HomeIntent
    data class RequestDelete(val workoutId: Long) : HomeIntent
    data object ConfirmDelete : HomeIntent
    data object CancelDelete : HomeIntent
}

sealed interface HomeEffect {
    data class NavigateToTimer(val workoutId: Long) : HomeEffect
    data object NavigateToCreateWorkout : HomeEffect
}
