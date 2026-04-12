import AudioToolbox
import AVFoundation
import Shared
import UIKit

/// Реакция на эффекты таймера: MP3 как `res/raw` на Android, тоны — через системные звуки (аналог `ToneGenerator`).
enum TimerFeedback {

    static func handle(_ effect: TimerEffect, store: TimerStore) {
        switch effect {
        case is TimerEffectPlayPrepTickSound:
            AudioServicesPlaySystemSound(1104)
        case is TimerEffectPlayPrepEndSound:
            let id = (store.state.value as? TimerState)?.workStartSoundPresetId ?? ""
            playPrepEnd(workPresetId: id)
        case is TimerEffectVibratePrepEnd:
            UIImpactFeedbackGenerator(style: .heavy).impactOccurred()
        case is TimerEffectPlayWorkSound:
            let id = (store.state.value as? TimerState)?.workStartSoundPresetId ?? ""
            playPreset(id, fallback: 1111)
        case is TimerEffectPlayRestSound:
            let id = (store.state.value as? TimerState)?.restStartSoundPresetId ?? ""
            playPreset(id, fallback: 1112)
        case is TimerEffectPlayFinishSound:
            let id = (store.state.value as? TimerState)?.finishSoundPresetId ?? ""
            playPreset(id, fallback: 1025)
        case is TimerEffectVibrate:
            UIImpactFeedbackGenerator(style: .medium).impactOccurred()
        case is TimerEffectVibrateFinish:
            UINotificationFeedbackGenerator().notificationOccurred(.success)
        case let alert as TimerEffectAlert10Seconds:
            // Отдельный цикл runloop на каждый тик — иначе одинаковые системные звуки подряд могут сливаться.
            DispatchQueue.main.async {
                AudioServicesPlaySystemSound(1005)
                if alert.withVibration {
                    UIImpactFeedbackGenerator(style: .rigid).impactOccurred()
                }
            }
        default:
            break
        }
    }

    static func previewPreset(_ presetId: String, fallback: SystemSoundID = 1103) {
        playPreset(presetId, fallback: fallback)
    }

    private static func playPrepEnd(workPresetId: String) {
        if rawAssetName(forPreset: workPresetId) != nil {
            playPreset(workPresetId, fallback: 1057)
            return
        }
        playToneSystemSound(forPreset: workPresetId, fallback: 1057)
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.24) {
            playToneSystemSound(forPreset: workPresetId, fallback: 1057)
        }
    }

    private static func playPreset(_ presetId: String, fallback: SystemSoundID) {
        if let name = rawAssetName(forPreset: presetId) {
            playBundledMp3(named: name)
        } else {
            playToneSystemSound(forPreset: presetId, fallback: fallback)
        }
    }

    private static func rawAssetName(forPreset presetId: String) -> String? {
        switch presetId {
        case "custom_mp3_start": return "timer_custom_start"
        case "custom_mp3_end": return "timer_custom_end"
        case "custom_mp3_sound_1": return "timer_custom_sound_1"
        case "custom_mp3_sound_2": return "timer_custom_sound_2"
        case "custom_mp3_sound_3": return "timer_custom_sound_3"
        default: return nil
        }
    }

    private static var mp3Player: AVAudioPlayer?

    private static func playBundledMp3(named baseName: String) {
        guard let url = Bundle.main.url(forResource: baseName, withExtension: "mp3") else {
            AudioServicesPlaySystemSound(1103)
            return
        }
        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, mode: .default, options: [.mixWithOthers])
            try AVAudioSession.sharedInstance().setActive(true)
            mp3Player?.stop()
            let p = try AVAudioPlayer(contentsOf: url)
            p.prepareToPlay()
            mp3Player = p
            p.play()
        } catch {
            AudioServicesPlaySystemSound(1103)
        }
    }

    /// Соответствие пресетам из `TimerSoundPresets.kt` (без raw) — разные `SystemSoundID`, ближе к различимости тонов.
    private static func playToneSystemSound(forPreset presetId: String, fallback: SystemSoundID) {
        let sound: SystemSoundID
        switch presetId {
        case "beep_standard": sound = 1103
        case "beep_soft": sound = 1104
        case "ack": sound = 1057
        case "cdma_incall": sound = 1002
        case "cdma_guard": sound = 1074
        case "dtmf_1": sound = 1073
        case "dtmf_3": sound = 1075
        case "cdma_high": sound = 1003
        case "finish_default": sound = 1025
        default: sound = fallback
        }
        AudioServicesPlaySystemSound(sound)
    }
}
