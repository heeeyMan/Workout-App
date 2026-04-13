package com.workout.android.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat

object TimerFeedback {

    @Volatile
    private var activeRawPlayer: MediaPlayer? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    fun previewPreset(context: Context, presetId: String) {
        playPreset(context, TimerSoundPresets.byId(presetId))
    }

    fun playPrepTickTone(context: Context) {
        playTone(context, ToneGenerator.TONE_PROP_ACK, 55)
    }

    /** Конец подготовки — два сигнала выбранным тоном «перед работой»; для MP3 — один проигрыш. */
    fun playPrepEndTone(context: Context, workPresetId: String) {
        val p = TimerSoundPresets.byId(workPresetId)
        if (p.rawResId != null) {
            playRaw(context, p.rawResId)
            return
        }
        val h = Handler(Looper.getMainLooper())
        playTone(context, p.toneType, 200)
        h.postDelayed({
            playTone(context, p.toneType, 280)
        }, 240L)
    }

    fun playWorkTone(context: Context, workPresetId: String) {
        playPreset(context, TimerSoundPresets.byId(workPresetId))
    }

    fun playRestTone(context: Context, restPresetId: String) {
        playPreset(context, TimerSoundPresets.byId(restPresetId))
    }

    private fun playPreset(context: Context, preset: TimerSoundPreset) {
        val raw = preset.rawResId
        if (raw != null) {
            playRaw(context, raw)
        } else {
            playTone(context, preset.toneType, preset.shortDurationMs)
        }
    }

    private fun playRaw(context: Context, resId: Int) {
        val app = context.applicationContext
        try {
            synchronized(this) {
                try {
                    activeRawPlayer?.release()
                } catch (_: Exception) { }
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
                try {
                    it.release()
                } catch (_: Exception) { }
                synchronized(this) {
                    if (activeRawPlayer === it) activeRawPlayer = null
                }
            }
            mp.setOnErrorListener { _, _, _ ->
                try {
                    mp.release()
                } catch (_: Exception) { }
                synchronized(this) {
                    if (activeRawPlayer === mp) activeRawPlayer = null
                }
                true
            }
            mp.prepare()
            synchronized(this) {
                activeRawPlayer = mp
            }
            mp.start()
        } catch (_: Exception) {
            synchronized(this) {
                try {
                    activeRawPlayer?.release()
                } catch (_: Exception) { }
                activeRawPlayer = null
            }
        }
    }

    fun playFinishTone(context: Context, presetId: String) {
        playPreset(context, TimerSoundPresets.byId(presetId))
    }

    /**
     * Сигнал в окне «конец работы» — с главного потока, чтобы ежесекундные вызовы
     * не терялись из‑за ToneGenerator/потока коллектора эффектов.
     */
    fun playWarningTone(context: Context, presetId: String) {
        mainHandler.post {
            try {
                playPreset(context, TimerSoundPresets.byId(presetId))
            } catch (_: Exception) { }
        }
    }

    private fun playTone(context: Context, toneType: Int, durationMs: Int) {
        try {
            val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
            tg.startTone(toneType, durationMs)
            mainHandler.postDelayed({
                try {
                    tg.release()
                } catch (_: Exception) { }
            }, (durationMs + 80L).coerceAtLeast(100L))
        } catch (_: Exception) { }
    }

    fun vibrateShort(context: Context) {
        vibrate(context, 40)
    }

    fun vibratePrepEnd(context: Context) {
        vibratePattern(context, longArrayOf(0, 140, 90, 160, 90, 120))
    }

    fun vibrateAlert(context: Context) {
        vibratePattern(context, longArrayOf(0, 60, 80, 60))
    }

    /** Окончание тренировки — одна длинная вибрация (~1.2 с). */
    fun vibrateFinish(context: Context) {
        val v = getVibrator(context) ?: return
        val durationMs = 1200L
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(durationMs)
            }
        } catch (_: Exception) { }
    }

    private fun vibrate(context: Context, ms: Int) {
        val v = getVibrator(context) ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(ms.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(ms.toLong())
            }
        } catch (_: Exception) { }
    }

    private fun vibratePattern(context: Context, pattern: LongArray) {
        val v = getVibrator(context) ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(pattern, -1)
            }
        } catch (_: Exception) { }
    }

    private fun getVibrator(context: Context): Vibrator? {
        val app = context.applicationContext
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = ContextCompat.getSystemService(app, VibratorManager::class.java)
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ContextCompat.getSystemService(app, Vibrator::class.java)
        }
    }
}
