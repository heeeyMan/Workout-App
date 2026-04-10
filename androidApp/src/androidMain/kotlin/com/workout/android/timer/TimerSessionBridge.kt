package com.workout.android.timer

import android.content.Context
import com.workout.shared.feature.timer.TimerIntent
import com.workout.shared.feature.timer.TimerStore

/**
 * Связь между [TimerStore] активного экрана таймера и внешними событиями (кнопки уведомления).
 * На связанных часах Wear OS те же действия часто дублируются из сопряжённого уведомления.
 */
class TimerSessionBridge {
    private var activeStore: TimerStore? = null

    fun attach(store: TimerStore) {
        activeStore = store
    }

    fun detach(store: TimerStore) {
        if (activeStore === store) activeStore = null
    }

    fun dispatch(intent: TimerIntent): Boolean {
        val s = activeStore ?: return false
        s.dispatch(intent)
        return true
    }

    fun refreshNotificationIfActive(context: Context) {
        val state = activeStore?.state?.value ?: return
        refreshWorkoutTimerNotification(context, state)
    }
}
