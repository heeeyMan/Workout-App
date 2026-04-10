package com.workout.android.data

import android.content.Context

class TimerPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var blockPrepDurationSeconds: Int
        get() = prefs.getInt(KEY_BLOCK_PREP_SECONDS, DEFAULT_BLOCK_PREP_SECONDS)
        set(value) {
            prefs.edit().putInt(KEY_BLOCK_PREP_SECONDS, value.coerceIn(MIN_PREP_SECONDS, MAX_PREP_SECONDS)).apply()
        }

    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()
        }

    var vibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, value).apply()
        }

    /** Пользователь нажал «Позже» в диалоге разрешения уведомлений — не показывать снова автоматически. */
    var skipNotificationPermissionPrompt: Boolean
        get() = prefs.getBoolean(KEY_SKIP_NOTIF_PROMPT, false)
        set(value) {
            prefs.edit().putBoolean(KEY_SKIP_NOTIF_PROMPT, value).apply()
        }

    companion object {
        private const val PREFS_NAME = "timer_settings"
        private const val KEY_BLOCK_PREP_SECONDS = "block_prep_seconds"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        private const val KEY_SKIP_NOTIF_PROMPT = "skip_notification_permission_prompt"
        const val DEFAULT_BLOCK_PREP_SECONDS = 5
        const val MIN_PREP_SECONDS = 0
        const val MAX_PREP_SECONDS = 120
    }
}
