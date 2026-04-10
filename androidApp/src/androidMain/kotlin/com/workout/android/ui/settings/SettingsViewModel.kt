package com.workout.android.ui.settings

import androidx.lifecycle.ViewModel
import com.workout.android.data.TimerPreferences
import com.workout.android.feedback.TimerSoundPresets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class TimerSoundPickerTarget { WORK, REST, FINISH }

class SettingsViewModel(private val timerPreferences: TimerPreferences) : ViewModel() {

    private val _blockPrepSeconds = MutableStateFlow(timerPreferences.blockPrepDurationSeconds)
    val blockPrepSeconds: StateFlow<Int> = _blockPrepSeconds.asStateFlow()

    private val _soundEnabled = MutableStateFlow(timerPreferences.soundEnabled)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(timerPreferences.vibrationEnabled)
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    private val _alertAt10Seconds = MutableStateFlow(timerPreferences.alertAt10Seconds)
    val alertAt10Seconds: StateFlow<Boolean> = _alertAt10Seconds.asStateFlow()

    private val _workSoundPresetId = MutableStateFlow(timerPreferences.workStartSoundPresetId)
    val workSoundPresetId: StateFlow<String> = _workSoundPresetId.asStateFlow()

    private val _restSoundPresetId = MutableStateFlow(timerPreferences.restStartSoundPresetId)
    val restSoundPresetId: StateFlow<String> = _restSoundPresetId.asStateFlow()

    private val _finishSoundPresetId = MutableStateFlow(timerPreferences.workoutFinishSoundPresetId)
    val finishSoundPresetId: StateFlow<String> = _finishSoundPresetId.asStateFlow()

    private val _soundPickerTarget = MutableStateFlow<TimerSoundPickerTarget?>(null)
    val soundPickerTarget: StateFlow<TimerSoundPickerTarget?> = _soundPickerTarget.asStateFlow()

    private val _pendingSoundPresetId = MutableStateFlow(TimerSoundPresets.DEFAULT_WORK_ID)
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

    fun setAlertAt10Seconds(enabled: Boolean) {
        timerPreferences.alertAt10Seconds = enabled
        _alertAt10Seconds.value = enabled
    }

    fun openWorkSoundPicker() {
        _pendingSoundPresetId.value = _workSoundPresetId.value
        _soundPickerTarget.value = TimerSoundPickerTarget.WORK
    }

    fun openRestSoundPicker() {
        _pendingSoundPresetId.value = _restSoundPresetId.value
        _soundPickerTarget.value = TimerSoundPickerTarget.REST
    }

    fun openFinishSoundPicker() {
        _pendingSoundPresetId.value = _finishSoundPresetId.value
        _soundPickerTarget.value = TimerSoundPickerTarget.FINISH
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
            TimerSoundPickerTarget.FINISH -> {
                timerPreferences.workoutFinishSoundPresetId = _pendingSoundPresetId.value
                _finishSoundPresetId.value = timerPreferences.workoutFinishSoundPresetId
            }
        }
        _soundPickerTarget.value = null
    }

    fun dismissSoundPicker() {
        _soundPickerTarget.value = null
    }
}
