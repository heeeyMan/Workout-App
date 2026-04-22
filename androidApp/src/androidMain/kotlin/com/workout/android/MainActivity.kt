package com.workout.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import com.workout.android.ui.permissions.AppEntryPermissionHandler
import com.workout.android.widget.WorkoutWidget
import com.workout.core.repository.WorkoutRepository
import com.workout.shared.ui.navigation.WorkoutApp
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val repository: WorkoutRepository by inject()
    private var startWorkoutId: Long? by mutableStateOf(null)
    private var openCreate: Boolean by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        enableEdgeToEdge()
        setContent {
            AppEntryPermissionHandler()
            WorkoutApp(startWorkoutId = startWorkoutId, openCreate = openCreate)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val manager = GlanceAppWidgetManager(this@MainActivity)
            manager.getGlanceIds(WorkoutWidget::class.java)
                .forEach { id -> WorkoutWidget().update(this@MainActivity, id) }
        }
    }

    private fun handleIntent(intent: Intent) {
        val id = intent.getLongExtra(EXTRA_WORKOUT_ID, -1L).takeIf { it != -1L }
        startWorkoutId = id
        openCreate = intent.getBooleanExtra(EXTRA_OPEN_CREATE, false)
        if (id != null) {
            lifecycleScope.launch { repository.markWorkoutStarted(id) }
        }
    }

    companion object {
        const val EXTRA_WORKOUT_ID = "extra_workout_id"
        const val EXTRA_OPEN_CREATE = "extra_open_create"
    }
}
