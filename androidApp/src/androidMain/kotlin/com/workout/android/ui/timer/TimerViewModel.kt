package com.workout.android.ui.timer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workout.android.R
import com.workout.android.data.TimerPreferences
import com.workout.android.feedback.TimerFeedback
import com.workout.android.timer.WorkoutTimerForegroundService
import com.workout.android.timer.toForegroundNotificationLines
import com.workout.shared.feature.timer.TimerEffect
import com.workout.shared.feature.timer.TimerIntent
import com.workout.shared.feature.timer.TimerStore
import kotlinx.coroutines.launch

class TimerViewModel(
    private val store: TimerStore,
    private val timerPreferences: TimerPreferences,
    private val appContext: Context,
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
                restStartSoundPresetId = timerPreferences.restStartSoundPresetId,
                finishSoundPresetId = timerPreferences.workoutFinishSoundPresetId,
                workPhaseEndWarningSeconds = timerPreferences.workPhaseEndWarningSeconds,
                restPhaseDisplayName = appContext.getString(R.string.phase_rest_name)
            )
        )

        viewModelScope.launch {
            store.state.collect { s ->
                when {
                    s.isFinished -> WorkoutTimerForegroundService.stop(appContext)
                    s.isLoading -> Unit
                    else -> {
                        val lines = s.toForegroundNotificationLines(appContext) ?: return@collect
                        WorkoutTimerForegroundService.update(
                            appContext,
                            lines.workoutName,
                            lines.phaseLine,
                            lines.detailLine,
                            lines.timeLine
                        )
                    }
                }
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
        WorkoutTimerForegroundService.stop(appContext)
        store.destroy()
    }
}
