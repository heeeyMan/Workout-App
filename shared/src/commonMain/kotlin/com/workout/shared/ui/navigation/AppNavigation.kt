package com.workout.shared.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.workout.shared.ui.createworkout.CreateWorkoutScreen
import com.workout.shared.ui.home.HomeScreen
import com.workout.shared.ui.settings.SettingsScreen
import com.workout.shared.ui.timer.TimerScreen

@Composable
fun AppNavigation(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute
    ) {
        composable<HomeRoute> {
            HomeScreen(
                onNavigateToTimer = { id -> navController.navigate(TimerRoute(id)) },
                onNavigateToCreateWorkout = { navController.navigate(CreateWorkoutRoute()) },
                onNavigateToEditWorkout = { id -> navController.navigate(CreateWorkoutRoute(id)) },
                onNavigateToSettings = { navController.navigate(SettingsRoute) }
            )
        }

        composable<SettingsRoute> {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<CreateWorkoutRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<CreateWorkoutRoute>()
            CreateWorkoutScreen(
                workoutId = route.id,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<TimerRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<TimerRoute>()
            TimerScreen(
                workoutId = route.workoutId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
