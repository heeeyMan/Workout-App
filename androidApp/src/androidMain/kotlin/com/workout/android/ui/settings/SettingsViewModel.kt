package com.workout.android.ui.settings

import androidx.lifecycle.ViewModel
import com.workout.android.data.TimerPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(private val timerPreferences: TimerPreferences) : ViewModel() {

    private val _blockPrepSeconds = MutableStateFlow(timerPreferences.blockPrepDurationSeconds)
    val blockPrepSeconds: StateFlow<Int> = _blockPrepSeconds.asStateFlow()

    private val _soundEnabled = MutableStateFlow(timerPreferences.soundEnabled)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(timerPreferences.vibrationEnabled)
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    fun setBlockPrepSeconds(seconds: Int) {
        val v = seconds.coerceIn(TimerPreferences.MIN_PREP_SECONDS, TimerPreferences.MAX_PREP_SECONDS)
        timerPreferences.blockPrepDurationSeconds = v
        _blockPrepSeconds.value = v
    }

    fun setSoundEnabled(enabled: Boolean) {
        timerPreferences.soundEnabled = enabled
        _soundEnabled.value = enabled
    }

    fun setVibrationEnabled(enabled: Boolean) {
        timerPreferences.vibrationEnabled = enabled
        _vibrationEnabled.value = enabled
    }
}
