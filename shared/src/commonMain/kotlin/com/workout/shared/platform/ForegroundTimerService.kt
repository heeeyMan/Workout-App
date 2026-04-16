package com.workout.shared.platform

import com.workout.shared.feature.timer.TimerState

interface ForegroundTimerService {
    fun update(state: TimerState, workoutName: String)
    fun stop()
}
