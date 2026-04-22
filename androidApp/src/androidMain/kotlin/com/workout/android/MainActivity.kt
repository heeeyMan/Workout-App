package com.workout.android

import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import com.workout.android.ui.permissions.AppEntryPermissionHandler
import com.workout.android.widget.WorkoutWidget
import com.workout.core.repository.WorkoutRepository
import com.workout.shared.ui.navigation.WorkoutApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
            Box(Modifier.fillMaxSize()) {
                WorkoutApp(startWorkoutId = startWorkoutId, openCreate = openCreate)
                AppEntryPermissionHandler()
            }
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
            updateWidget()
            updateShortcuts()
        }
    }

    private suspend fun updateWidget() {
        val manager = GlanceAppWidgetManager(this@MainActivity)
        manager.getGlanceIds(WorkoutWidget::class.java)
            .forEach { id -> WorkoutWidget().update(this@MainActivity, id) }
    }

    private suspend fun updateShortcuts() {
        val shortcutManager = getSystemService(ShortcutManager::class.java) ?: return
        if (shortcutManager.isRateLimitingActive) return

        val shortcuts = mutableListOf<ShortcutInfo>()

        val lastWorkout = withContext(Dispatchers.IO) {
            repository.getWorkouts().first()
                .filter { it.lastStartedAt != null }
                .maxByOrNull { it.lastStartedAt ?: 0L }
        }

        if (lastWorkout != null) {
            val lastIntent = Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra(EXTRA_WORKOUT_ID, lastWorkout.id)
            }
            shortcuts.add(
                ShortcutInfo.Builder(this, SHORTCUT_LAST_WORKOUT)
                    .setShortLabel(lastWorkout.name.take(10))
                    .setLongLabel(lastWorkout.name.take(25))
                    .setIcon(Icon.createWithResource(this, R.drawable.ic_tile_workout))
                    .setIntent(lastIntent)
                    .setRank(0)
                    .build()
            )
        }

        val createIntent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_OPEN_CREATE
        }
        shortcuts.add(
            ShortcutInfo.Builder(this, SHORTCUT_OPEN_CREATE)
                .setShortLabel(getString(R.string.shortcut_create_short))
                .setLongLabel(getString(R.string.shortcut_create_long))
                .setIcon(Icon.createWithResource(this, R.drawable.ic_shortcut_add))
                .setIntent(createIntent)
                .setRank(1)
                .build()
        )

        shortcutManager.setDynamicShortcuts(shortcuts)
    }

    private fun handleIntent(intent: Intent) {
        val id = intent.getLongExtra(EXTRA_WORKOUT_ID, -1L).takeIf { it != -1L }
        startWorkoutId = id
        openCreate = intent.getBooleanExtra(EXTRA_OPEN_CREATE, false)
            || intent.action == ACTION_OPEN_CREATE
        if (id != null) {
            lifecycleScope.launch { repository.markWorkoutStarted(id) }
        }
    }

    companion object {
        const val EXTRA_WORKOUT_ID = "extra_workout_id"
        const val EXTRA_OPEN_CREATE = "extra_open_create"
        const val ACTION_OPEN_CREATE = "com.workout.android.action.OPEN_CREATE"
        private const val SHORTCUT_LAST_WORKOUT = "last_workout"
        private const val SHORTCUT_OPEN_CREATE = "open_create"
    }
}
