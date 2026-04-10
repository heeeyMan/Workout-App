package com.workout.android.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.workout.shared.feature.timer.TimerIntent
import org.koin.core.context.GlobalContext

class TimerNotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val bridge = try {
            GlobalContext.get().get<TimerSessionBridge>()
        } catch (_: Exception) {
            return
        }
        when (intent?.action) {
            ACTION_TOGGLE_PAUSE -> bridge.dispatch(TimerIntent.TogglePause)
            ACTION_SKIP_PHASE -> bridge.dispatch(TimerIntent.SkipPhase)
            else -> return
        }
        bridge.refreshNotificationIfActive(context)
    }

    companion object {
        const val ACTION_TOGGLE_PAUSE = "com.workout.android.timer.TOGGLE_PAUSE"
        const val ACTION_SKIP_PHASE = "com.workout.android.timer.SKIP_PHASE"
    }
}
