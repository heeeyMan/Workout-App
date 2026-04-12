import SwiftUI

struct SettingsView: View {
    @State private var blockPrep = 5
    @State private var workWarn = 3
    @State private var soundOn = true
    @State private var vibrationOn = true
    @State private var quickAdjust = false
    @State private var workPreset = TimerUserSettings.defaultWorkPresetId
    @State private var restPreset = TimerUserSettings.defaultRestPresetId
    @State private var finishPreset = TimerUserSettings.defaultFinishPresetId

    /// Порядок как `TimerSoundPresets.all` на Android.
    private var presetChoices: [(id: String, titleKey: String)] {
        [
            ("custom_mp3_start", "preset_custom_mp3_start"),
            ("custom_mp3_end", "preset_custom_mp3_end"),
            ("custom_mp3_sound_1", "preset_custom_sound_1"),
            ("custom_mp3_sound_2", "preset_custom_sound_2"),
            ("custom_mp3_sound_3", "preset_custom_sound_3"),
            ("beep_standard", "preset_beep_standard"),
            ("beep_soft", "preset_beep_soft"),
            ("ack", "preset_ack"),
            ("cdma_incall", "preset_cdma_incall"),
            ("cdma_guard", "preset_cdma_guard"),
            ("dtmf_1", "preset_dtmf_1"),
            ("dtmf_3", "preset_dtmf_3"),
            ("cdma_high", "preset_cdma_high"),
            ("finish_default", "preset_finish_default"),
        ]
    }

    var body: some View {
        Form {
            Section(L10n.tr("prep_time_title")) {
                Stepper(L10n.tr("seconds_unit_suffix", blockPrep), value: $blockPrep, in: 0...120)
                    .onChange(of: blockPrep) { newValue in
                        TimerUserSettings.shared.blockPrepDurationSeconds = newValue
                    }
            }
            Section(L10n.tr("work_phase_warn_title")) {
                Stepper(L10n.tr("seconds_unit_suffix", workWarn), value: $workWarn, in: 0...120)
                    .onChange(of: workWarn) { newValue in
                        TimerUserSettings.shared.workPhaseEndWarningSeconds = newValue
                    }
                Picker(L10n.tr("sound_before_work"), selection: $workPreset) {
                    ForEach(presetChoices, id: \.id) { row in
                        Text(L10n.tr(row.titleKey)).tag(row.id)
                    }
                }
                .pickerStyle(.menu)
                .onChange(of: workPreset) { newValue in
                    TimerUserSettings.shared.workStartSoundPresetId = newValue
                    TimerFeedback.previewPreset(newValue, fallback: 1111)
                }
            }
            Section(L10n.tr("sound_before_rest")) {
                Picker("", selection: $restPreset) {
                    ForEach(presetChoices, id: \.id) { row in
                        Text(L10n.tr(row.titleKey)).tag(row.id)
                    }
                }
                .labelsHidden()
                .pickerStyle(.menu)
                .onChange(of: restPreset) { newValue in
                    TimerUserSettings.shared.restStartSoundPresetId = newValue
                    TimerFeedback.previewPreset(newValue, fallback: 1112)
                }
            }
            Section(L10n.tr("sound_workout_finish")) {
                Picker("", selection: $finishPreset) {
                    ForEach(presetChoices, id: \.id) { row in
                        Text(L10n.tr(row.titleKey)).tag(row.id)
                    }
                }
                .labelsHidden()
                .pickerStyle(.menu)
                .onChange(of: finishPreset) { newValue in
                    TimerUserSettings.shared.workoutFinishSoundPresetId = newValue
                    TimerFeedback.previewPreset(newValue, fallback: 1025)
                }
            }
            Section(L10n.tr("settings_title")) {
                Toggle(L10n.tr("timer_sound_enabled"), isOn: $soundOn)
                    .onChange(of: soundOn) { newValue in
                        TimerUserSettings.shared.soundEnabled = newValue
                    }
                Toggle(L10n.tr("vibration"), isOn: $vibrationOn)
                    .onChange(of: vibrationOn) { newValue in
                        TimerUserSettings.shared.vibrationEnabled = newValue
                    }
            }
            Section {
                Toggle(L10n.tr("timer_quick_adjust_title"), isOn: $quickAdjust)
                    .onChange(of: quickAdjust) { newValue in
                        TimerUserSettings.shared.timerQuickAdjustEnabled = newValue
                    }
            }
        }
        .scrollContentBackground(.hidden)
        .background(WorkoutPalette.background)
        .navigationTitle(L10n.tr("settings_title"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbarColorScheme(.dark, for: .navigationBar)
        .tint(WorkoutPalette.primary)
        .onAppear {
            let p = TimerUserSettings.shared
            blockPrep = p.blockPrepDurationSeconds
            workWarn = p.workPhaseEndWarningSeconds
            soundOn = p.soundEnabled
            vibrationOn = p.vibrationEnabled
            quickAdjust = p.timerQuickAdjustEnabled
            workPreset = p.workStartSoundPresetId
            restPreset = p.restStartSoundPresetId
            finishPreset = p.workoutFinishSoundPresetId
        }
    }
}
