package com.workout.shared.platform

interface ScreenWakeLock {
    fun acquire()
    fun release()
}
