package com.workout.android.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object WorkoutList : Screen("workout_list")
    data object CreateWorkout : Screen("create_workout?id={id}") {
        fun route(id: Long = 0L) = "create_workout?id=$id"
    }
    data object Timer : Screen("timer/{workoutId}") {
        fun route(workoutId: Long) = "timer/$workoutId"
    }
}
