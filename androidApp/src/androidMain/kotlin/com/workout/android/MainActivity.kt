package com.workout.android

import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.workout.android.navigation.AppNavigation
import com.workout.android.theme.WorkoutAppTheme
import com.workout.android.ui.settings.SettingsViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Sync launcher icon whenever theme changes
        lifecycleScope.launch {
            settingsViewModel.isDarkTheme.collect { isDark ->
                updateLauncherIcon(isDark)
            }
        }

        setContent {
            val isDark by settingsViewModel.isDarkTheme.collectAsState()
            WorkoutAppTheme(darkTheme = isDark) {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    isDarkTheme = isDark,
                    onSetDarkTheme = settingsViewModel::setDarkTheme
                )
            }
        }
    }

    private fun updateLauncherIcon(isDark: Boolean) {
        val pm = packageManager
        val lightAlias = ComponentName(this, "com.workout.android.MainActivityLight")
        val darkAlias = ComponentName(this, "com.workout.android.MainActivityDark")

        val (lightState, darkState) = if (isDark) {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED to
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED to
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }

        pm.setComponentEnabledSetting(lightAlias, lightState, PackageManager.DONT_KILL_APP)
        pm.setComponentEnabledSetting(darkAlias, darkState, PackageManager.DONT_KILL_APP)
    }
}
