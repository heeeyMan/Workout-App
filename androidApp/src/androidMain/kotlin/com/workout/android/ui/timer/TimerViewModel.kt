package com.workout.android.ui.timer

import androidx.lifecycle.ViewModel
import com.workout.android.data.TimerPreferences
import com.workout.shared.feature.timer.TimerIntent
import com.workout.shared.feature.timer.TimerStore

class TimerViewModel(
    private val store: TimerStore,
    private val timerPreferences: TimerPreferences,
    workoutId: Long
) : ViewModel() {
    val state = store.state
    val effects = store.effects

    init {
        store.dispatch(
            TimerIntent.Load(
                workoutId = workoutId,
                blockPrepDurationSeconds = timerPreferences.blockPrepDurationSeconds,
                soundEnabled = timerPreferences.soundEnabled,
                vibrationEnabled = timerPreferences.vibrationEnabled,
                workStartSoundPresetId = timerPreferences.workStartSoundPresetId,
                restStartSoundPresetId = timerPreferences.restStartSoundPresetId
            )
        )
    }

    fun dispatch(intent: TimerIntent) = store.dispatch(intent)
    override fun onCleared() = store.destroy()
}
