package com.workout.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.workout.android.ui.permissions.AppEntryPermissionHandler
import com.workout.shared.ui.navigation.WorkoutApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppEntryPermissionHandler()
            WorkoutApp()
        }
    }
}
