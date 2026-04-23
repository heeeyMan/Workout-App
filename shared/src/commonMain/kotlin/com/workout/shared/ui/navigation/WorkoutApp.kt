package com.workout.shared.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.workout.shared.ui.theme.WorkoutAppTheme

@Composable
fun WorkoutApp(
    startWorkoutId: Long? = null,
    startWorkoutToken: Int = 0,
    openCreateToken: Int = 0
) {
    WorkoutAppTheme {
        AppNavigation(rememberNavController(), startWorkoutId, startWorkoutToken, openCreateToken)
    }
}
