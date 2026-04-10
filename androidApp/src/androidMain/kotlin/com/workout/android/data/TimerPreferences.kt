package com.workout.android.data

import android.content.Context

class TimerPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var blockPrepDurationSeconds: Int
        get() = prefs.getInt(KEY_BLOCK_PREP_SECONDS, DEFAULT_BLOCK_PREP_SECONDS)
        set(value) {
            prefs.edit().putInt(KEY_BLOCK_PREP_SECONDS, value.coerceIn(MIN_PREP_SECONDS, MAX_PREP_SECONDS)).apply()
        }

    companion object {
        private const val PREFS_NAME = "timer_settings"
        private const val KEY_BLOCK_PREP_SECONDS = "block_prep_seconds"
        const val DEFAULT_BLOCK_PREP_SECONDS = 5
        const val MIN_PREP_SECONDS = 0
        const val MAX_PREP_SECONDS = 120
    }
}
