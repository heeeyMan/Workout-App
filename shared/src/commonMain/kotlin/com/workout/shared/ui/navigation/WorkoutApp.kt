package com.workout.shared.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.workout.shared.ui.theme.WorkoutAppTheme

@Composable
fun WorkoutApp() {
    WorkoutAppTheme {
        AppNavigation(rememberNavController())
    }
}
