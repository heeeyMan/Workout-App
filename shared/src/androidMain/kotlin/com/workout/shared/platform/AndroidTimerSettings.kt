package com.workout.shared.platform

import android.content.Context

class AndroidTimerSettings(context: Context) : TimerSettings {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override var blockPrepDurationSeconds: Int
        get() = prefs.getInt(KEY_BLOCK_PREP_SECONDS, DEFAULT_BLOCK_PREP_SECONDS)
            .coerceIn(MIN_PREP_SECONDS, MAX_PREP_SECONDS)
        set(value) {
            prefs.edit().putInt(KEY_BLOCK_PREP_SECONDS, value.coerceIn(MIN_PREP_SECONDS, MAX_PREP_SECONDS)).apply()
        }

    override var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()
        }

    override var vibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, value).apply()
        }

    override var workPhaseEndWarningSeconds: Int
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

    override var timerQuickAdjustEnabled: Boolean
        get() = prefs.getBoolean(KEY_TIMER_QUICK_ADJUST, true)
        set(value) {
            prefs.edit().putBoolean(KEY_TIMER_QUICK_ADJUST, value).apply()
        }

    override var workStartSoundPresetId: String
        get() = normalizePresetId(prefs.getString(KEY_WORK_SOUND_PRESET, null), SoundPresets.DEFAULT_WORK_ID)
        set(value) {
            prefs.edit().putString(
                KEY_WORK_SOUND_PRESET,
                normalizePresetId(value, SoundPresets.DEFAULT_WORK_ID)
            ).apply()
        }

    override var restStartSoundPresetId: String
        get() = normalizePresetId(prefs.getString(KEY_REST_SOUND_PRESET, null), SoundPresets.DEFAULT_REST_ID)
        set(value) {
            prefs.edit().putString(
                KEY_REST_SOUND_PRESET,
                normalizePresetId(value, SoundPresets.DEFAULT_REST_ID)
            ).apply()
        }

    override var workoutFinishSoundPresetId: String
        get() = normalizePresetId(prefs.getString(KEY_FINISH_SOUND_PRESET, null), SoundPresets.DEFAULT_FINISH_ID)
        set(value) {
            prefs.edit().putString(
                KEY_FINISH_SOUND_PRESET,
                normalizePresetId(value, SoundPresets.DEFAULT_FINISH_ID)
            ).apply()
        }

    override var workPhaseWarningSoundPresetId: String
        get() = normalizePresetId(
            prefs.getString(KEY_WORK_PHASE_WARN_SOUND_PRESET, null),
            SoundPresets.DEFAULT_WARNING_ID
        )
        set(value) {
            prefs.edit().putString(
                KEY_WORK_PHASE_WARN_SOUND_PRESET,
                normalizePresetId(value, SoundPresets.DEFAULT_WARNING_ID)
            ).apply()
        }

    private fun normalizePresetId(raw: String?, defaultId: String): String {
        val id = raw ?: defaultId
        return if (SoundPresets.isValidId(id)) id else defaultId
    }

    companion object {
        private const val PREFS_NAME = "timer_settings"
        private const val KEY_BLOCK_PREP_SECONDS = "block_prep_seconds"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        private const val KEY_WORK_PHASE_END_WARN = "work_phase_end_warning_seconds"
        private const val KEY_ALERT_10_SECONDS_LEGACY = "alert_at_10_seconds"
        private const val KEY_TIMER_QUICK_ADJUST = "timer_quick_adjust_enabled"
        private const val KEY_WORK_SOUND_PRESET = "work_start_sound_preset"
        private const val KEY_REST_SOUND_PRESET = "rest_start_sound_preset"
        private const val KEY_FINISH_SOUND_PRESET = "workout_finish_sound_preset"
        private const val KEY_WORK_PHASE_WARN_SOUND_PRESET = "work_phase_warning_sound_preset"
        const val DEFAULT_BLOCK_PREP_SECONDS = 5
        const val MIN_PREP_SECONDS = 0
        const val MAX_PREP_SECONDS = 120
        const val DEFAULT_WORK_PHASE_WARN_SECONDS = 3
        const val MIN_WORK_PHASE_WARN = 0
        const val MAX_WORK_PHASE_WARN = 120
    }
}
