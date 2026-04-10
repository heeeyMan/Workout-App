package com.workout.android.data

import android.content.Context
import com.workout.android.feedback.TimerSoundPresets

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

    /** В фазе «Работа» в последние 10 с — короткий звук каждую секунду. */
    var alertAt10Seconds: Boolean
        get() = prefs.getBoolean(KEY_ALERT_10_SECONDS, true)
        set(value) {
            prefs.edit().putBoolean(KEY_ALERT_10_SECONDS, value).apply()
        }

    /** Пользователь нажал «Позже» в диалоге разрешения уведомлений — не показывать снова автоматически. */
    var skipNotificationPermissionPrompt: Boolean
        get() = prefs.getBoolean(KEY_SKIP_NOTIF_PROMPT, false)
        set(value) {
            prefs.edit().putBoolean(KEY_SKIP_NOTIF_PROMPT, value).apply()
        }

    var workStartSoundPresetId: String
        get() = normalizePresetId(prefs.getString(KEY_WORK_SOUND_PRESET, null), TimerSoundPresets.DEFAULT_WORK_ID)
        set(value) {
            prefs.edit().putString(
                KEY_WORK_SOUND_PRESET,
                normalizePresetId(value, TimerSoundPresets.DEFAULT_WORK_ID)
            ).apply()
        }

    var restStartSoundPresetId: String
        get() = normalizePresetId(prefs.getString(KEY_REST_SOUND_PRESET, null), TimerSoundPresets.DEFAULT_REST_ID)
        set(value) {
            prefs.edit().putString(
                KEY_REST_SOUND_PRESET,
                normalizePresetId(value, TimerSoundPresets.DEFAULT_REST_ID)
            ).apply()
        }

    var workoutFinishSoundPresetId: String
        get() = normalizePresetId(prefs.getString(KEY_FINISH_SOUND_PRESET, null), TimerSoundPresets.DEFAULT_FINISH_ID)
        set(value) {
            prefs.edit().putString(
                KEY_FINISH_SOUND_PRESET,
                normalizePresetId(value, TimerSoundPresets.DEFAULT_FINISH_ID)
            ).apply()
        }

    private fun normalizePresetId(raw: String?, defaultId: String): String {
        val id = raw ?: defaultId
        return if (TimerSoundPresets.isValidId(id)) id else defaultId
    }

    companion object {
        private const val PREFS_NAME = "timer_settings"
        private const val KEY_BLOCK_PREP_SECONDS = "block_prep_seconds"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        private const val KEY_ALERT_10_SECONDS = "alert_at_10_seconds"
        private const val KEY_SKIP_NOTIF_PROMPT = "skip_notification_permission_prompt"
        private const val KEY_WORK_SOUND_PRESET = "work_start_sound_preset"
        private const val KEY_REST_SOUND_PRESET = "rest_start_sound_preset"
        private const val KEY_FINISH_SOUND_PRESET = "workout_finish_sound_preset"
        const val DEFAULT_BLOCK_PREP_SECONDS = 5
        const val MIN_PREP_SECONDS = 0
        const val MAX_PREP_SECONDS = 120
    }
}
