package com.workout.android.ui.timer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workout.android.R
import com.workout.android.data.TimerPreferences
import com.workout.android.feedback.TimerFeedback
import com.workout.android.timer.TimerSessionBridge
import com.workout.android.timer.WorkoutTimerForegroundService
import com.workout.android.timer.refreshWorkoutTimerNotification
import com.workout.shared.feature.timer.TimerEffect
import com.workout.shared.feature.timer.TimerIntent
import com.workout.shared.feature.timer.TimerStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TimerViewModel(
    private val store: TimerStore,
    private val timerPreferences: TimerPreferences,
    private val appContext: Context,
    private val timerSessionBridge: TimerSessionBridge,
    workoutId: Long
) : ViewModel() {

    val state = store.state
    val effects = store.effects

    private val _quickAdjustEnabled = MutableStateFlow(timerPreferences.timerQuickAdjustEnabled)
    val quickAdjustEnabled: StateFlow<Boolean> = _quickAdjustEnabled

    fun refreshUiPrefs() {
        _quickAdjustEnabled.value = timerPreferences.timerQuickAdjustEnabled
    }

    init {
        timerSessionBridge.attach(store)
        store.dispatch(
            TimerIntent.Load(
                workoutId = workoutId,
                blockPrepDurationSeconds = timerPreferences.blockPrepDurationSeconds,
                soundEnabled = timerPreferences.soundEnabled,
                vibrationEnabled = timerPreferences.vibrationEnabled,
                workStartSoundPresetId = timerPreferences.workStartSoundPresetId,
                restStartSoundPresetId = timerPreferences.restStartSoundPresetId,
                finishSoundPresetId = timerPreferences.workoutFinishSoundPresetId,
                workPhaseEndWarningSeconds = timerPreferences.workPhaseEndWarningSeconds,
                restPhaseDisplayName = appContext.getString(R.string.phase_rest_name)
            )
        )

        viewModelScope.launch {
            store.state.collect { s ->
                refreshWorkoutTimerNotification(appContext, s)
            }
        }

        viewModelScope.launch {
            store.effects.collect { effect ->
                when (effect) {
                    is TimerEffect.NavigateBack -> Unit
                    is TimerEffect.PlayPrepTickSound -> TimerFeedback.playPrepTickTone(appContext)
                    is TimerEffect.PlayPrepEndSound -> {
                        val st = store.state.value
                        TimerFeedback.playPrepEndTone(appContext, st.workStartSoundPresetId)
                    }
                    is TimerEffect.VibratePrepEnd -> TimerFeedback.vibratePrepEnd(appContext)
                    is TimerEffect.PlayWorkSound -> {
                        val st = store.state.value
                        TimerFeedback.playWorkTone(appContext, st.workStartSoundPresetId)
                    }
                    is TimerEffect.PlayRestSound -> {
                        val st = store.state.value
                        TimerFeedback.playRestTone(appContext, st.restStartSoundPresetId)
                    }
                    is TimerEffect.PlayFinishSound -> {
                        val st = store.state.value
                        TimerFeedback.playFinishTone(appContext, st.finishSoundPresetId)
                    }
                    is TimerEffect.Vibrate -> TimerFeedback.vibrateShort(appContext)
                    is TimerEffect.VibrateFinish -> TimerFeedback.vibrateFinish(appContext)
                    is TimerEffect.Alert10Seconds -> {
                        val st = store.state.value
                        if (st.soundEnabled) TimerFeedback.playAlertTone(appContext)
                        if (effect.withVibration && st.vibrationEnabled) {
                            TimerFeedback.vibrateAlert(appContext)
                        }
                    }
                }
            }
        }
    }

    fun dispatch(intent: TimerIntent) = store.dispatch(intent)

    override fun onCleared() {
        timerSessionBridge.detach(store)
        WorkoutTimerForegroundService.stop(appContext)
        store.destroy()
    }
}
