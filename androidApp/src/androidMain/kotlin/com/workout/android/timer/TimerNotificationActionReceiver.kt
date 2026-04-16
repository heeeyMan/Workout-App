package com.workout.android.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.workout.shared.feature.timer.TimerIntent
import com.workout.shared.platform.AndroidForegroundTimerService

class TimerNotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val callback = AndroidForegroundTimerService.dispatchCallback ?: return
        when (intent?.action) {
            ACTION_TOGGLE_PAUSE -> callback(TimerIntent.TogglePause)
            ACTION_SKIP_PHASE -> callback(TimerIntent.SkipPhase)
            ACTION_PREVIOUS_PHASE -> callback(TimerIntent.PreviousPhase)
        }
    }

    companion object {
        const val ACTION_TOGGLE_PAUSE = "com.workout.android.timer.TOGGLE_PAUSE"
        const val ACTION_SKIP_PHASE = "com.workout.android.timer.SKIP_PHASE"
        const val ACTION_PREVIOUS_PHASE = "com.workout.android.timer.PREVIOUS_PHASE"
    }
}
