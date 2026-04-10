package com.workout.android.feedback

import android.media.ToneGenerator
import com.workout.android.R

data class TimerSoundPreset(
    val id: String,
    /** Подпись в настройках */
    val label: String,
    val toneType: Int,
    /** Длительность для короткого сигнала (ToneGenerator); для raw не используется */
    val shortDurationMs: Int,
    /** Если задан — короткий клип из [res/raw] вместо [toneType] */
    val rawResId: Int? = null
)

object TimerSoundPresets {

    const val DEFAULT_WORK_ID = "beep_standard"
    const val DEFAULT_REST_ID = "beep_soft"

    val all: List<TimerSoundPreset> = listOf(
        TimerSoundPreset(
            id = "custom_mp3_start",
            label = "Старт (MP3)",
            toneType = ToneGenerator.TONE_PROP_BEEP,
            shortDurationMs = 0,
            rawResId = R.raw.timer_custom_start
        ),
        TimerSoundPreset(
            id = "custom_mp3_end",
            label = "Конец (MP3)",
            toneType = ToneGenerator.TONE_PROP_BEEP,
            shortDurationMs = 0,
            rawResId = R.raw.timer_custom_end
        ),
        TimerSoundPreset(
            id = "beep_standard",
            label = "Короткий сигнал",
            toneType = ToneGenerator.TONE_PROP_BEEP,
            shortDurationMs = 130
        ),
        TimerSoundPreset(
            id = "beep_soft",
            label = "Мягкий сигнал",
            toneType = ToneGenerator.TONE_PROP_BEEP2,
            shortDurationMs = 130
        ),
        TimerSoundPreset(
            id = "ack",
            label = "Щелчок",
            toneType = ToneGenerator.TONE_PROP_ACK,
            shortDurationMs = 70
        ),
        TimerSoundPreset(
            id = "cdma_incall",
            label = "Напоминание",
            toneType = ToneGenerator.TONE_CDMA_ALERT_INCALL_LITE,
            shortDurationMs = 220
        ),
        TimerSoundPreset(
            id = "cdma_guard",
            label = "Сигнал вызова",
            toneType = ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,
            shortDurationMs = 180
        ),
        TimerSoundPreset(
            id = "dtmf_1",
            label = "Тон 1",
            toneType = ToneGenerator.TONE_DTMF_1,
            shortDurationMs = 120
        ),
        TimerSoundPreset(
            id = "dtmf_3",
            label = "Тон 3",
            toneType = ToneGenerator.TONE_DTMF_3,
            shortDurationMs = 120
        ),
        TimerSoundPreset(
            id = "cdma_high",
            label = "Высокий тон",
            toneType = ToneGenerator.TONE_CDMA_HIGH_SS,
            shortDurationMs = 160
        )
    )

    fun byId(id: String): TimerSoundPreset =
        all.find { it.id == id } ?: all.first()

    fun isValidId(id: String): Boolean = all.any { it.id == id }
}
