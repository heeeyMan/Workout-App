package com.workout.shared.platform

import com.workout.shared.feature.timer.TimerState

/**
 * No-op implementation for iOS. iOS does not have foreground services
 * equivalent to Android; background execution is handled differently.
 */
class IosForegroundTimerService : ForegroundTimerService {

    override fun update(state: TimerState, workoutName: String) {
        // No-op: iOS does not use foreground services for timer notifications.
    }

    override fun stop() {
        // No-op.
    }
}
