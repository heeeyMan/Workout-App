package com.workout.shared.platform

import platform.UIKit.UIApplication

class IosScreenWakeLock : ScreenWakeLock {

    override fun acquire() {
        UIApplication.sharedApplication.setIdleTimerDisabled(true)
    }

    override fun release() {
        UIApplication.sharedApplication.setIdleTimerDisabled(false)
    }
}
