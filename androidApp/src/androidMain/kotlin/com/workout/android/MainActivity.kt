package com.workout.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import com.workout.android.navigation.AppNavigation
import com.workout.android.theme.WorkoutAppTheme
import com.workout.android.ui.settings.SettingsViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDark by settingsViewModel.isDarkTheme.collectAsState()
            WorkoutAppTheme(darkTheme = isDark) {
                val navController = rememberNavController()
                AppNavigation(navController = navController)
            }
        }
    }
}
