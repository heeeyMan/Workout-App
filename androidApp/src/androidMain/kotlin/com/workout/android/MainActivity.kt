package com.workout.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.workout.android.navigation.AppNavigation
import com.workout.android.theme.WorkoutAppTheme
import com.workout.android.ui.permissions.AppEntryPermissionHandler

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WorkoutAppTheme {
                AppEntryPermissionHandler()
                val navController = rememberNavController()
                AppNavigation(navController = navController)
            }
        }
    }
}
