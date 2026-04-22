package com.workout.shared.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.workout.shared.ui.theme.WorkoutAppTheme

@Composable
fun WorkoutApp(startWorkoutId: Long? = null, openCreate: Boolean = false) {
    WorkoutAppTheme {
        AppNavigation(rememberNavController(), startWorkoutId, openCreate)
    }
}
