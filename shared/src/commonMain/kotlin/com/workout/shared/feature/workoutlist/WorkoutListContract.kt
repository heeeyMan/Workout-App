package com.workout.shared.feature.workoutlist

import com.workout.core.model.Workout

data class WorkoutListState(
    val workouts: List<Workout> = emptyList(),
    val isLoading: Boolean = true,
    val pendingDeleteId: Long? = null
)

sealed interface WorkoutListIntent {
    data object LoadWorkouts : WorkoutListIntent
    data class SelectWorkout(val workoutId: Long) : WorkoutListIntent
    data class RequestDelete(val workoutId: Long) : WorkoutListIntent
    data object ConfirmDelete : WorkoutListIntent
    data object CancelDelete : WorkoutListIntent
    data object CreateWorkout : WorkoutListIntent
}

sealed interface WorkoutListEffect {
    data class NavigateToTimer(val workoutId: Long) : WorkoutListEffect
    data object NavigateToCreateWorkout : WorkoutListEffect
}
