package com.workout.android.ui.timer

import androidx.lifecycle.ViewModel
import com.workout.shared.feature.timer.TimerIntent
import com.workout.shared.feature.timer.TimerStore

class TimerViewModel(private val store: TimerStore, workoutId: Long) : ViewModel() {
    val state = store.state
    val effects = store.effects

    init {
        store.dispatch(TimerIntent.Load(workoutId))
    }

    fun dispatch(intent: TimerIntent) = store.dispatch(intent)
    override fun onCleared() = store.destroy()
}
