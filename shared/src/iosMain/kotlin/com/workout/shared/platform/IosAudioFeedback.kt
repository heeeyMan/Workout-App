package com.workout.shared.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation
import platform.AVFAudio.setActive
import platform.AudioToolbox.AudioServicesPlaySystemSound
import platform.AudioToolbox.SystemSoundID
import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.darwin.dispatch_after
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time
import platform.darwin.DISPATCH_TIME_NOW

@OptIn(ExperimentalForeignApi::class)
class IosAudioFeedback : AudioFeedback {

    private var mp3Player: AVAudioPlayer? = null

    override fun playPrepTickTone() {
        AudioServicesPlaySystemSound(1104u)
    }

    override fun playPrepEndTone(workPresetId: String) {
        val assetName = rawAssetName(workPresetId)
        if (assetName != null) {
            playPreset(workPresetId, fallback = 1057u)
            return
        }
        playToneSystemSound(workPresetId, fallback = 1057u)
        dispatch_after(
            dispatch_time(DISPATCH_TIME_NOW, (0.24 * 1_000_000_000).toLong()),
            dispatch_get_main_queue()
        ) {
            playToneSystemSound(workPresetId, fallback = 1057u)
        }
    }

    override fun playWorkTone(workPresetId: String) {
        playPreset(workPresetId, fallback = 1111u)
    }

    override fun playRestTone(restPresetId: String) {
        playPreset(restPresetId, fallback = 1112u)
    }

    override fun playFinishTone(presetId: String) {
        playPreset(presetId, fallback = 1025u)
    }

    override fun playWarningTone(presetId: String) {
        dispatch_after(
            dispatch_time(DISPATCH_TIME_NOW, 0L),
            dispatch_get_main_queue()
        ) {
            playPreset(presetId, fallback = 1005u)
        }
    }

    override fun previewPreset(presetId: String) {
        playPreset(presetId, fallback = 1103u)
    }

    private fun playPreset(presetId: String, fallback: SystemSoundID) {
        val name = rawAssetName(presetId)
        if (name != null) {
            playBundledMp3(name, fallback)
        } else {
            playToneSystemSound(presetId, fallback)
        }
    }

    private fun rawAssetName(presetId: String): String? = when (presetId) {
        "custom_mp3_start" -> "timer_custom_start"
        "custom_mp3_end" -> "timer_custom_end"
        "custom_mp3_sound_1" -> "timer_custom_sound_1"
        "custom_mp3_sound_2" -> "timer_custom_sound_2"
        "custom_mp3_sound_3" -> "timer_custom_sound_3"
        else -> null
    }

    private fun playBundledMp3(baseName: String, fallback: SystemSoundID) {
        val url = NSBundle.mainBundle.URLForResource(baseName, withExtension = "mp3")
            ?: NSBundle.mainBundle.URLForResource(baseName, withExtension = "mp3", subdirectory = "TimerRaw")
        if (url == null) {
            AudioServicesPlaySystemSound(fallback)
            return
        }
        try {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayback, error = null)
            session.setActive(true, error = null)
            mp3Player?.stop()
            val player = AVAudioPlayer(contentsOfURL = url, error = null)
            if (player != null) {
                player.prepareToPlay()
                mp3Player = player
                player.play()
            } else {
                AudioServicesPlaySystemSound(fallback)
            }
        } catch (_: Exception) {
            AudioServicesPlaySystemSound(fallback)
        }
    }

    private fun playToneSystemSound(presetId: String, fallback: SystemSoundID) {
        val sound: SystemSoundID = when (presetId) {
            "beep_standard" -> 1103u
            "beep_soft" -> 1104u
            "ack" -> 1057u
            "cdma_incall" -> 1002u
            "cdma_guard" -> 1074u
            "dtmf_1" -> 1073u
            "dtmf_3" -> 1075u
            "cdma_high" -> 1003u
            "finish_default" -> 1025u
            else -> fallback
        }
        AudioServicesPlaySystemSound(sound)
    }
}
