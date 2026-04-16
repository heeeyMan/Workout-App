package com.workout.shared.platform

import platform.Foundation.NSUserDefaults

class IosTimerSettings : TimerSettings {

    private val defaults: NSUserDefaults =
        NSUserDefaults(suiteName = SUITE_NAME) ?: NSUserDefaults.standardUserDefaults

    override var blockPrepDurationSeconds: Int
        get() {
            if (defaults.objectForKey(KEY_BLOCK_PREP) == null) return DEFAULT_BLOCK_PREP_SECONDS
            return defaults.integerForKey(KEY_BLOCK_PREP).toInt()
                .coerceIn(MIN_PREP_SECONDS, MAX_PREP_SECONDS)
        }
        set(value) {
            defaults.setInteger(value.coerceIn(MIN_PREP_SECONDS, MAX_PREP_SECONDS).toLong(), forKey = KEY_BLOCK_PREP)
        }

    override var soundEnabled: Boolean
        get() {
            if (defaults.objectForKey(KEY_SOUND) == null) return true
            return defaults.boolForKey(KEY_SOUND)
        }
        set(value) {
            defaults.setBool(value, forKey = KEY_SOUND)
        }

    override var vibrationEnabled: Boolean
        get() {
            if (defaults.objectForKey(KEY_VIBRATION) == null) return true
            return defaults.boolForKey(KEY_VIBRATION)
        }
        set(value) {
            defaults.setBool(value, forKey = KEY_VIBRATION)
        }

    override var workPhaseEndWarningSeconds: Int
        get() {
            if (defaults.objectForKey(KEY_WORK_PHASE_WARN) == null) return DEFAULT_WORK_PHASE_WARN_SECONDS
            return defaults.integerForKey(KEY_WORK_PHASE_WARN).toInt()
                .coerceIn(MIN_WORK_PHASE_WARN, MAX_WORK_PHASE_WARN)
        }
        set(value) {
            defaults.setInteger(
                value.coerceIn(MIN_WORK_PHASE_WARN, MAX_WORK_PHASE_WARN).toLong(),
                forKey = KEY_WORK_PHASE_WARN
            )
        }

    override var timerQuickAdjustEnabled: Boolean
        get() {
            if (defaults.objectForKey(KEY_QUICK_ADJUST) == null) return true
            return defaults.boolForKey(KEY_QUICK_ADJUST)
        }
        set(value) {
            defaults.setBool(value, forKey = KEY_QUICK_ADJUST)
        }

    override var workStartSoundPresetId: String
        get() = normalizePresetId(defaults.stringForKey(KEY_WORK_SOUND), SoundPresets.DEFAULT_WORK_ID)
        set(value) {
            defaults.setObject(normalizePresetId(value, SoundPresets.DEFAULT_WORK_ID), forKey = KEY_WORK_SOUND)
        }

    override var restStartSoundPresetId: String
        get() = normalizePresetId(defaults.stringForKey(KEY_REST_SOUND), SoundPresets.DEFAULT_REST_ID)
        set(value) {
            defaults.setObject(normalizePresetId(value, SoundPresets.DEFAULT_REST_ID), forKey = KEY_REST_SOUND)
        }

    override var workoutFinishSoundPresetId: String
        get() = normalizePresetId(defaults.stringForKey(KEY_FINISH_SOUND), SoundPresets.DEFAULT_FINISH_ID)
        set(value) {
            defaults.setObject(normalizePresetId(value, SoundPresets.DEFAULT_FINISH_ID), forKey = KEY_FINISH_SOUND)
        }

    override var workPhaseWarningSoundPresetId: String
        get() = normalizePresetId(defaults.stringForKey(KEY_WARNING_SOUND), SoundPresets.DEFAULT_WARNING_ID)
        set(value) {
            defaults.setObject(normalizePresetId(value, SoundPresets.DEFAULT_WARNING_ID), forKey = KEY_WARNING_SOUND)
        }

    private fun normalizePresetId(raw: String?, defaultId: String): String {
        val id = raw ?: defaultId
        return if (SoundPresets.isValidId(id)) id else defaultId
    }

    companion object {
        private const val SUITE_NAME = "timer_settings"
        private const val KEY_BLOCK_PREP = "block_prep_seconds"
        private const val KEY_SOUND = "sound_enabled"
        private const val KEY_VIBRATION = "vibration_enabled"
        private const val KEY_WORK_PHASE_WARN = "work_phase_end_warning_seconds"
        private const val KEY_QUICK_ADJUST = "timer_quick_adjust_enabled"
        private const val KEY_WORK_SOUND = "work_start_sound_preset"
        private const val KEY_REST_SOUND = "rest_start_sound_preset"
        private const val KEY_FINISH_SOUND = "workout_finish_sound_preset"
        private const val KEY_WARNING_SOUND = "work_phase_warning_sound_preset"
        const val DEFAULT_BLOCK_PREP_SECONDS = 5
        const val MIN_PREP_SECONDS = 0
        const val MAX_PREP_SECONDS = 120
        const val DEFAULT_WORK_PHASE_WARN_SECONDS = 3
        const val MIN_WORK_PHASE_WARN = 0
        const val MAX_WORK_PHASE_WARN = 120
    }
}
