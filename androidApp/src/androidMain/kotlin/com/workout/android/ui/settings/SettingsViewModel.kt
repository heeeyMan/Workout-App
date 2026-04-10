package com.workout.android.ui.settings

import androidx.lifecycle.ViewModel
import com.workout.android.data.TimerPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class TimerSoundPickerTarget { WORK, REST }

class SettingsViewModel(private val timerPreferences: TimerPreferences) : ViewModel() {

    private val _blockPrepSeconds = MutableStateFlow(timerPreferences.blockPrepDurationSeconds)
    val blockPrepSeconds: StateFlow<Int> = _blockPrepSeconds.asStateFlow()

    private val _soundEnabled = MutableStateFlow(timerPreferences.soundEnabled)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(timerPreferences.vibrationEnabled)
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    private val _workSoundPresetId = MutableStateFlow(timerPreferences.workStartSoundPresetId)
    val workSoundPresetId: StateFlow<String> = _workSoundPresetId.asStateFlow()

    private val _restSoundPresetId = MutableStateFlow(timerPreferences.restStartSoundPresetId)
    val restSoundPresetId: StateFlow<String> = _restSoundPresetId.asStateFlow()

    private val _soundPickerTarget = MutableStateFlow<TimerSoundPickerTarget?>(null)
    val soundPickerTarget: StateFlow<TimerSoundPickerTarget?> = _soundPickerTarget.asStateFlow()

    private val _pendingSoundPresetId = MutableStateFlow(timerPreferences.workStartSoundPresetId)
    val pendingSoundPresetId: StateFlow<String> = _pendingSoundPresetId.asStateFlow()

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

    fun openWorkSoundPicker() {
        _pendingSoundPresetId.value = _workSoundPresetId.value
        _soundPickerTarget.value = TimerSoundPickerTarget.WORK
    }

    fun openRestSoundPicker() {
        _pendingSoundPresetId.value = _restSoundPresetId.value
        _soundPickerTarget.value = TimerSoundPickerTarget.REST
    }

    fun setPendingSoundPresetId(id: String) {
        _pendingSoundPresetId.value = id
    }

    fun confirmSoundPicker() {
        when (val t = _soundPickerTarget.value ?: return) {
            TimerSoundPickerTarget.WORK -> {
                timerPreferences.workStartSoundPresetId = _pendingSoundPresetId.value
                _workSoundPresetId.value = timerPreferences.workStartSoundPresetId
            }
            TimerSoundPickerTarget.REST -> {
                timerPreferences.restStartSoundPresetId = _pendingSoundPresetId.value
                _restSoundPresetId.value = timerPreferences.restStartSoundPresetId
            }
        }
        _soundPickerTarget.value = null
    }

    fun dismissSoundPicker() {
        _soundPickerTarget.value = null
    }
}
