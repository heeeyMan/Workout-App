import Foundation

/// Те же ключи и значения по умолчанию, что у [TimerPreferences] на Android (`timer_settings`).
final class TimerUserSettings {
    static let shared = TimerUserSettings()

    private let defaults: UserDefaults

    private init() {
        defaults = UserDefaults(suiteName: "timer_settings") ?? .standard
    }

    private enum Key {
        static let blockPrep = "block_prep_seconds"
        static let sound = "sound_enabled"
        static let vibration = "vibration_enabled"
        static let workPhaseWarn = "work_phase_end_warning_seconds"
        static let quickAdjust = "timer_quick_adjust_enabled"
        static let workSound = "work_start_sound_preset"
        static let restSound = "rest_start_sound_preset"
        static let finishSound = "workout_finish_sound_preset"
    }

    static let defaultBlockPrepSeconds = 5
    static let defaultWorkPhaseWarnSeconds = 3
    static let defaultWorkPresetId = "custom_mp3_start"
    static let defaultRestPresetId = "custom_mp3_end"
    static let defaultFinishPresetId = "custom_mp3_end"

    var blockPrepDurationSeconds: Int {
        get {
            guard defaults.object(forKey: Key.blockPrep) != nil else { return Self.defaultBlockPrepSeconds }
            let v = defaults.integer(forKey: Key.blockPrep)
            return min(max(v, 0), 120)
        }
        set { defaults.set(min(max(newValue, 0), 120), forKey: Key.blockPrep) }
    }

    var soundEnabled: Bool {
        get {
            if defaults.object(forKey: Key.sound) == nil { return true }
            return defaults.bool(forKey: Key.sound)
        }
        set { defaults.set(newValue, forKey: Key.sound) }
    }

    var vibrationEnabled: Bool {
        get {
            if defaults.object(forKey: Key.vibration) == nil { return true }
            return defaults.bool(forKey: Key.vibration)
        }
        set { defaults.set(newValue, forKey: Key.vibration) }
    }

    var workPhaseEndWarningSeconds: Int {
        get {
            guard defaults.object(forKey: Key.workPhaseWarn) != nil else { return Self.defaultWorkPhaseWarnSeconds }
            let v = defaults.integer(forKey: Key.workPhaseWarn)
            return min(max(v, 0), 120)
        }
        set { defaults.set(min(max(newValue, 0), 120), forKey: Key.workPhaseWarn) }
    }

    var timerQuickAdjustEnabled: Bool {
        get { defaults.bool(forKey: Key.quickAdjust) }
        set { defaults.set(newValue, forKey: Key.quickAdjust) }
    }

    var workStartSoundPresetId: String {
        get { normalizedPreset(defaults.string(forKey: Key.workSound), defaultId: Self.defaultWorkPresetId) }
        set { defaults.set(normalizedPreset(newValue, defaultId: Self.defaultWorkPresetId), forKey: Key.workSound) }
    }

    var restStartSoundPresetId: String {
        get { normalizedPreset(defaults.string(forKey: Key.restSound), defaultId: Self.defaultRestPresetId) }
        set { defaults.set(normalizedPreset(newValue, defaultId: Self.defaultRestPresetId), forKey: Key.restSound) }
    }

    var workoutFinishSoundPresetId: String {
        get { normalizedPreset(defaults.string(forKey: Key.finishSound), defaultId: Self.defaultFinishPresetId) }
        set { defaults.set(normalizedPreset(newValue, defaultId: Self.defaultFinishPresetId), forKey: Key.finishSound) }
    }

    private let validPresetIds: Set<String> = [
        "custom_mp3_start", "custom_mp3_end", "custom_mp3_sound_1", "custom_mp3_sound_2", "custom_mp3_sound_3",
        "beep_standard", "beep_soft", "ack", "cdma_incall", "cdma_guard", "dtmf_1", "dtmf_3",
        "cdma_high", "finish_default",
    ]

    private func normalizedPreset(_ raw: String?, defaultId: String) -> String {
        guard let id = raw, validPresetIds.contains(id) else { return defaultId }
        return id
    }
}
