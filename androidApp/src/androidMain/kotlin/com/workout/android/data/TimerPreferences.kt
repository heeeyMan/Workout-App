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

    /**
     * За сколько секунд до конца фазы «Работа» включать предупреждение (звук каждую секунду).
     * 0 — выключено. По умолчанию 3 с.
     */
    var workPhaseEndWarningSeconds: Int
        get() {
            if (!prefs.contains(KEY_WORK_PHASE_END_WARN)) {
                if (prefs.contains(KEY_ALERT_10_SECONDS_LEGACY)) {
                    val legacyOn = prefs.getBoolean(KEY_ALERT_10_SECONDS_LEGACY, true)
                    val migrated = if (legacyOn) 10 else 0
                    prefs.edit()
                        .putInt(KEY_WORK_PHASE_END_WARN, migrated)
                        .remove(KEY_ALERT_10_SECONDS_LEGACY)
                        .apply()
                    return migrated.coerceIn(MIN_WORK_PHASE_WARN, MAX_WORK_PHASE_WARN)
                }
            }
            return prefs.getInt(KEY_WORK_PHASE_END_WARN, DEFAULT_WORK_PHASE_WARN_SECONDS)
                .coerceIn(MIN_WORK_PHASE_WARN, MAX_WORK_PHASE_WARN)
        }
        set(value) {
            prefs.edit().putInt(
                KEY_WORK_PHASE_END_WARN,
                value.coerceIn(MIN_WORK_PHASE_WARN, MAX_WORK_PHASE_WARN)
            ).apply()
        }

    /**
     * Кнопки «+10 с / −10 с» на экране таймера для текущей фазы.
     * По умолчанию выключено — включается в настройках.
     */
    var timerQuickAdjustEnabled: Boolean
        get() = prefs.getBoolean(KEY_TIMER_QUICK_ADJUST, false)
        set(value) {
            prefs.edit().putBoolean(KEY_TIMER_QUICK_ADJUST, value).apply()
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
        private const val KEY_WORK_PHASE_END_WARN = "work_phase_end_warning_seconds"
        private const val KEY_ALERT_10_SECONDS_LEGACY = "alert_at_10_seconds"
        private const val KEY_TIMER_QUICK_ADJUST = "timer_quick_adjust_enabled"
        private const val KEY_SKIP_NOTIF_PROMPT = "skip_notification_permission_prompt"
        private const val KEY_WORK_SOUND_PRESET = "work_start_sound_preset"
        private const val KEY_REST_SOUND_PRESET = "rest_start_sound_preset"
        private const val KEY_FINISH_SOUND_PRESET = "workout_finish_sound_preset"
        const val DEFAULT_BLOCK_PREP_SECONDS = 5
        const val MIN_PREP_SECONDS = 0
        const val MAX_PREP_SECONDS = 120
        const val DEFAULT_WORK_PHASE_WARN_SECONDS = 3
        const val MIN_WORK_PHASE_WARN = 0
        const val MAX_WORK_PHASE_WARN = 120
    }
}
