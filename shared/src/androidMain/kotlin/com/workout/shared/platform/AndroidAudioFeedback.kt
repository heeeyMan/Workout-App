package com.workout.shared.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

class AndroidAudioFeedback(private val context: Context) : AudioFeedback {

    @Volatile
    private var activeRawPlayer: MediaPlayer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun playPrepTickTone() {
        playTone(ToneGenerator.TONE_PROP_ACK, 55)
    }

    override fun playPrepEndTone(workPresetId: String) {
        val p = AndroidSoundPresetMapping.byId(workPresetId)
        val rawId = p.rawResName?.let { resolveRawId(it) }
        if (rawId != null) {
            playRaw(rawId)
            return
        }
        playTone(p.toneType, 200)
        mainHandler.postDelayed({ playTone(p.toneType, 280) }, 240L)
    }

    override fun playWorkTone(workPresetId: String) {
        playPreset(AndroidSoundPresetMapping.byId(workPresetId))
    }

    override fun playRestTone(restPresetId: String) {
        playPreset(AndroidSoundPresetMapping.byId(restPresetId))
    }

    override fun playFinishTone(presetId: String) {
        playPreset(AndroidSoundPresetMapping.byId(presetId))
    }

    override fun playWarningTone(presetId: String) {
        mainHandler.post {
            try {
                playPreset(AndroidSoundPresetMapping.byId(presetId))
            } catch (_: Exception) { }
        }
    }

    override fun previewPreset(presetId: String) {
        playPreset(AndroidSoundPresetMapping.byId(presetId))
    }

    private fun playPreset(preset: AndroidSoundPreset) {
        val rawId = preset.rawResName?.let { resolveRawId(it) }
        if (rawId != null) playRaw(rawId) else playTone(preset.toneType, preset.durationMs)
    }

    private fun resolveRawId(name: String): Int? {
        val id = context.resources.getIdentifier(name, "raw", context.packageName)
        return if (id != 0) id else null
    }

    private fun playTone(toneType: Int, durationMs: Int) {
        try {
            val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
            tg.startTone(toneType, durationMs)
            mainHandler.postDelayed({
                try { tg.release() } catch (_: Exception) { }
            }, (durationMs + 80L).coerceAtLeast(100L))
        } catch (_: Exception) { }
    }

    private fun playRaw(resId: Int) {
        val app = context.applicationContext
        try {
            synchronized(this) {
                try { activeRawPlayer?.release() } catch (_: Exception) { }
                activeRawPlayer = null
            }
            val afd = app.resources.openRawResourceFd(resId) ?: return
            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            mp.setOnCompletionListener {
                try { it.release() } catch (_: Exception) { }
                synchronized(this) { if (activeRawPlayer === it) activeRawPlayer = null }
            }
            mp.setOnErrorListener { _, _, _ ->
                try { mp.release() } catch (_: Exception) { }
                synchronized(this) { if (activeRawPlayer === mp) activeRawPlayer = null }
                true
            }
            mp.prepare()
            synchronized(this) { activeRawPlayer = mp }
            mp.start()
        } catch (_: Exception) {
            synchronized(this) {
                try { activeRawPlayer?.release() } catch (_: Exception) { }
                activeRawPlayer = null
            }
        }
    }
}

/** Маппинг общих preset ID на Android-специфичные ресурсы и тоны. */
private data class AndroidSoundPreset(
    val id: String,
    val toneType: Int,
    val durationMs: Int,
    val rawResName: String? = null
)

private object AndroidSoundPresetMapping {
    private val presets = listOf(
        AndroidSoundPreset("custom_mp3_start", ToneGenerator.TONE_PROP_BEEP, 0, "timer_custom_start"),
        AndroidSoundPreset("custom_mp3_end", ToneGenerator.TONE_PROP_BEEP, 0, "timer_custom_end"),
        AndroidSoundPreset("custom_mp3_sound_1", ToneGenerator.TONE_PROP_BEEP, 0, "timer_custom_sound_1"),
        AndroidSoundPreset("custom_mp3_sound_2", ToneGenerator.TONE_PROP_BEEP, 0, "timer_custom_sound_2"),
        AndroidSoundPreset("custom_mp3_sound_3", ToneGenerator.TONE_PROP_BEEP, 0, "timer_custom_sound_3"),
        AndroidSoundPreset("beep_standard", ToneGenerator.TONE_PROP_BEEP, 130),
        AndroidSoundPreset("beep_soft", ToneGenerator.TONE_PROP_BEEP2, 130),
        AndroidSoundPreset("ack", ToneGenerator.TONE_PROP_ACK, 70),
        AndroidSoundPreset("cdma_incall", ToneGenerator.TONE_CDMA_ALERT_INCALL_LITE, 220),
        AndroidSoundPreset("cdma_guard", ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 180),
        AndroidSoundPreset("dtmf_1", ToneGenerator.TONE_DTMF_1, 120),
        AndroidSoundPreset("dtmf_3", ToneGenerator.TONE_DTMF_3, 120),
        AndroidSoundPreset("cdma_high", ToneGenerator.TONE_CDMA_HIGH_SS, 160),
        AndroidSoundPreset("finish_default", ToneGenerator.TONE_CDMA_ALERT_INCALL_LITE, 280),
    )

    fun byId(id: String): AndroidSoundPreset = presets.find { it.id == id } ?: presets.first()
}
