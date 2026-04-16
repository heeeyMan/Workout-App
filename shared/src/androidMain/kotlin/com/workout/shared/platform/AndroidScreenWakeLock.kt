package com.workout.shared.platform

import android.app.Activity
import android.view.WindowManager
import java.lang.ref.WeakReference

class AndroidScreenWakeLock : ScreenWakeLock {

    private var activityRef: WeakReference<Activity>? = null

    fun attachActivity(activity: Activity) {
        activityRef = WeakReference(activity)
    }

    override fun acquire() {
        activityRef?.get()?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun release() {
        activityRef?.get()?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
