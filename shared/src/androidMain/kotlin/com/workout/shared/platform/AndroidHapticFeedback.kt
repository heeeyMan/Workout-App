package com.workout.shared.platform

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat

class AndroidHapticFeedback(private val context: Context) : HapticFeedback {

    override fun vibrateShort() = vibrate(40)

    override fun vibratePrepEnd() = vibratePattern(longArrayOf(0, 140, 90, 160, 90, 120))

    override fun vibrateAlert() = vibratePattern(longArrayOf(0, 60, 80, 60))

    override fun vibrateFinish() {
        val v = getVibrator() ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(1200L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(1200L)
            }
        } catch (_: Exception) { }
    }

    private fun vibrate(ms: Int) {
        val v = getVibrator() ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(ms.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(ms.toLong())
            }
        } catch (_: Exception) { }
    }

    private fun vibratePattern(pattern: LongArray) {
        val v = getVibrator() ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(pattern, -1)
            }
        } catch (_: Exception) { }
    }

    private fun getVibrator(): Vibrator? {
        val app = context.applicationContext
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.getSystemService(app, VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ContextCompat.getSystemService(app, Vibrator::class.java)
        }
    }
}
