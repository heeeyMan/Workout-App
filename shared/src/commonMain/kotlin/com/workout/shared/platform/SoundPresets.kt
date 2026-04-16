package com.workout.shared.platform

data class SoundPresetInfo(
    val id: String,
    val labelKey: String
)

object SoundPresets {
    const val DEFAULT_WORK_ID = "custom_mp3_start"
    const val DEFAULT_REST_ID = "custom_mp3_end"
    const val DEFAULT_FINISH_ID = "custom_mp3_end"
    const val DEFAULT_WARNING_ID = "beep_standard"

    val all: List<SoundPresetInfo> = listOf(
        SoundPresetInfo("custom_mp3_start", "preset_custom_mp3_start"),
        SoundPresetInfo("custom_mp3_end", "preset_custom_mp3_end"),
        SoundPresetInfo("custom_mp3_sound_1", "preset_custom_sound_1"),
        SoundPresetInfo("custom_mp3_sound_2", "preset_custom_sound_2"),
        SoundPresetInfo("custom_mp3_sound_3", "preset_custom_sound_3"),
        SoundPresetInfo("beep_standard", "preset_beep_standard"),
        SoundPresetInfo("beep_soft", "preset_beep_soft"),
        SoundPresetInfo("ack", "preset_ack"),
        SoundPresetInfo("cdma_incall", "preset_cdma_incall"),
        SoundPresetInfo("cdma_guard", "preset_cdma_guard"),
        SoundPresetInfo("dtmf_1", "preset_dtmf_1"),
        SoundPresetInfo("dtmf_3", "preset_dtmf_3"),
        SoundPresetInfo("cdma_high", "preset_cdma_high"),
        SoundPresetInfo("finish_default", "preset_finish_default"),
    )

    fun isValidId(id: String): Boolean = all.any { it.id == id }
}
