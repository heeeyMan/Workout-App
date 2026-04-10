package com.workout.android.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.workout.android.ui.createworkout.CreateWorkoutScreen
import com.workout.android.ui.home.HomeScreen
import com.workout.android.ui.timer.TimerScreen
import com.workout.android.ui.workoutlist.WorkoutListScreen

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToTimer = { id -> navController.navigate(Screen.Timer.route(id)) },
                onNavigateToCreateWorkout = { navController.navigate(Screen.CreateWorkout.route()) },
                onNavigateToWorkoutList = { navController.navigate(Screen.WorkoutList.route) }
            )
        }

        composable(Screen.WorkoutList.route) {
            WorkoutListScreen(
                onNavigateToTimer = { id -> navController.navigate(Screen.Timer.route(id)) },
                onNavigateToCreateWorkout = { navController.navigate(Screen.CreateWorkout.route()) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.CreateWorkout.route,
            arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = 0L })
        ) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getLong("id") ?: 0L
            CreateWorkoutScreen(
                workoutId = workoutId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Timer.route,
            arguments = listOf(navArgument("workoutId") { type = NavType.LongType })
        ) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getLong("workoutId") ?: return@composable
            TimerScreen(
                workoutId = workoutId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
