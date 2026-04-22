package com.workout.shared.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.workout.shared.platform.TimerSettings
import com.workout.shared.ui.createworkout.CreateWorkoutScreen
import com.workout.shared.ui.home.HomeScreen
import com.workout.shared.ui.onboarding.OnboardingScreen
import com.workout.shared.ui.settings.SettingsScreen
import com.workout.shared.ui.timer.TimerScreen
import org.koin.compose.koinInject

@Composable
fun AppNavigation(
    navController: NavHostController,
    startWorkoutId: Long? = null,
    openCreate: Boolean = false
) {
    val settings = koinInject<TimerSettings>()

    LaunchedEffect(startWorkoutId) {
        if (startWorkoutId != null) {
            navController.navigate(TimerRoute(startWorkoutId))
        }
    }
    LaunchedEffect(openCreate) {
        if (openCreate) {
            navController.navigate(CreateWorkoutRoute())
        }
    }

    val startDestination = if (!settings.onboardingCompleted) OnboardingRoute else HomeRoute

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable<OnboardingRoute> {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(HomeRoute) {
                        popUpTo(OnboardingRoute) { inclusive = true }
                    }
                }
            )
        }

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
