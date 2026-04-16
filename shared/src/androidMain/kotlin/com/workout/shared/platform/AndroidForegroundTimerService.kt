package com.workout.shared.platform

import com.workout.shared.feature.timer.TimerState

/**
 * Stub implementation. The real foreground service will be wired up later
 * from the androidApp module.
 */
class AndroidForegroundTimerService : ForegroundTimerService {

    override fun update(state: TimerState, workoutName: String) {
        // No-op stub — will delegate to the real Android foreground service later.
    }

    override fun stop() {
        // No-op stub.
    }
}
