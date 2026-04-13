package com.workout.android.feedback

import android.media.ToneGenerator
import androidx.annotation.StringRes
import com.workout.android.R

data class TimerSoundPreset(
    val id: String,
    @StringRes val labelRes: Int,
    val toneType: Int,
    /** Длительность для короткого сигнала (ToneGenerator); для raw не используется */
    val shortDurationMs: Int,
    /** Если задан — короткий клип из [res/raw] вместо [toneType] */
    val rawResId: Int? = null
)

object TimerSoundPresets {

    /** Звук начала подхода по умолчанию — [R.raw.timer_custom_start]. */
    const val DEFAULT_WORK_ID = "custom_mp3_start"
    const val DEFAULT_REST_ID = "custom_mp3_end"
    const val DEFAULT_FINISH_ID = "custom_mp3_end"
    const val DEFAULT_WARNING_ID = "beep_standard"

    val all: List<TimerSoundPreset> = listOf(
        TimerSoundPreset(
            id = "custom_mp3_start",
            labelRes = R.string.preset_custom_mp3_start,
            toneType = ToneGenerator.TONE_PROP_BEEP,
            shortDurationMs = 0,
            rawResId = R.raw.timer_custom_start
        ),
        TimerSoundPreset(
            id = "custom_mp3_end",
            labelRes = R.string.preset_custom_mp3_end,
            toneType = ToneGenerator.TONE_PROP_BEEP,
            shortDurationMs = 0,
            rawResId = R.raw.timer_custom_end
        ),
        TimerSoundPreset(
            id = "custom_mp3_sound_1",
            labelRes = R.string.preset_custom_sound_1,
            toneType = ToneGenerator.TONE_PROP_BEEP,
            shortDurationMs = 0,
            rawResId = R.raw.timer_custom_sound_1
        ),
        TimerSoundPreset(
            id = "custom_mp3_sound_2",
            labelRes = R.string.preset_custom_sound_2,
            toneType = ToneGenerator.TONE_PROP_BEEP,
            shortDurationMs = 0,
            rawResId = R.raw.timer_custom_sound_2
        ),
        TimerSoundPreset(
            id = "custom_mp3_sound_3",
            labelRes = R.string.preset_custom_sound_3,
            toneType = ToneGenerator.TONE_PROP_BEEP,
            shortDurationMs = 0,
            rawResId = R.raw.timer_custom_sound_3
        ),
        TimerSoundPreset(
            id = "beep_standard",
            labelRes = R.string.preset_beep_standard,
            toneType = ToneGenerator.TONE_PROP_BEEP,
            shortDurationMs = 130
        ),
        TimerSoundPreset(
            id = "beep_soft",
            labelRes = R.string.preset_beep_soft,
            toneType = ToneGenerator.TONE_PROP_BEEP2,
            shortDurationMs = 130
        ),
        TimerSoundPreset(
            id = "ack",
            labelRes = R.string.preset_ack,
            toneType = ToneGenerator.TONE_PROP_ACK,
            shortDurationMs = 70
        ),
        TimerSoundPreset(
            id = "cdma_incall",
            labelRes = R.string.preset_cdma_incall,
            toneType = ToneGenerator.TONE_CDMA_ALERT_INCALL_LITE,
            shortDurationMs = 220
        ),
        TimerSoundPreset(
            id = "cdma_guard",
            labelRes = R.string.preset_cdma_guard,
            toneType = ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,
            shortDurationMs = 180
        ),
        TimerSoundPreset(
            id = "dtmf_1",
            labelRes = R.string.preset_dtmf_1,
            toneType = ToneGenerator.TONE_DTMF_1,
            shortDurationMs = 120
        ),
        TimerSoundPreset(
            id = "dtmf_3",
            labelRes = R.string.preset_dtmf_3,
            toneType = ToneGenerator.TONE_DTMF_3,
            shortDurationMs = 120
        ),
        TimerSoundPreset(
            id = "cdma_high",
            labelRes = R.string.preset_cdma_high,
            toneType = ToneGenerator.TONE_CDMA_HIGH_SS,
            shortDurationMs = 160
        ),
        TimerSoundPreset(
            id = "finish_default",
            labelRes = R.string.preset_finish_default,
            toneType = ToneGenerator.TONE_CDMA_ALERT_INCALL_LITE,
            shortDurationMs = 280
        )
    )

    fun byId(id: String): TimerSoundPreset =
        all.find { it.id == id } ?: all.first()

    fun isValidId(id: String): Boolean = all.any { it.id == id }
}
