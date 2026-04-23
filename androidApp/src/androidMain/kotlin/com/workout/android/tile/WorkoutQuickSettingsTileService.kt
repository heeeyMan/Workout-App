package com.workout.android.tile

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.workout.android.MainActivity
import com.workout.android.R
import com.workout.core.repository.WorkoutRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext

class WorkoutQuickSettingsTileService : TileService() {

    private var lastWorkoutId: Long? = null

    override fun onStartListening() {
        super.onStartListening()
        CoroutineScope(Dispatchers.IO).launch {
            val repository = runCatching {
                GlobalContext.get().get<WorkoutRepository>()
            }.getOrNull() ?: return@launch

            val lastWorkout = repository.getWorkouts().first()
                .filter { it.lastStartedAt != null }
                .maxByOrNull { it.lastStartedAt ?: 0L }

            lastWorkoutId = lastWorkout?.id

            withContext(Dispatchers.Main) {
                qsTile?.apply {
                    state = Tile.STATE_ACTIVE
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        subtitle = lastWorkout?.name ?: getString(R.string.tile_no_workout)
                    }
                    updateTile()
                }
            }
        }
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            val id = lastWorkoutId
            if (id != null) {
                putExtra(MainActivity.EXTRA_WORKOUT_ID, id)
            } else {
                putExtra(MainActivity.EXTRA_OPEN_CREATE, true)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pi = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pi)
        } else {
            @Suppress("DEPRECATION")
            @SuppressLint("StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(intent)
        }
    }
}
