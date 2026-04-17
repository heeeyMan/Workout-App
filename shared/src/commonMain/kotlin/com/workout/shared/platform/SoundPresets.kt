package com.workout.shared.platform

import workoutapp.shared.generated.resources.Res
import workoutapp.shared.generated.resources.preset_ack
import workoutapp.shared.generated.resources.preset_beep_soft
import workoutapp.shared.generated.resources.preset_beep_standard
import workoutapp.shared.generated.resources.preset_cdma_guard
import workoutapp.shared.generated.resources.preset_cdma_high
import workoutapp.shared.generated.resources.preset_cdma_incall
import workoutapp.shared.generated.resources.preset_custom_mp3_end
import workoutapp.shared.generated.resources.preset_custom_mp3_start
import workoutapp.shared.generated.resources.preset_custom_sound_1
import workoutapp.shared.generated.resources.preset_custom_sound_2
import workoutapp.shared.generated.resources.preset_custom_sound_3
import workoutapp.shared.generated.resources.preset_dtmf_1
import workoutapp.shared.generated.resources.preset_dtmf_3
import workoutapp.shared.generated.resources.preset_finish_default
import org.jetbrains.compose.resources.StringResource

data class SoundPresetInfo(
    val id: String,
    val label: StringResource
)

object SoundPresets {
    const val DEFAULT_WORK_ID = "custom_mp3_start"
    const val DEFAULT_REST_ID = "custom_mp3_end"
    const val DEFAULT_FINISH_ID = "custom_mp3_end"
    const val DEFAULT_WARNING_ID = "beep_standard"

    val all: List<SoundPresetInfo> = listOf(
        SoundPresetInfo("custom_mp3_start", Res.string.preset_custom_mp3_start),
        SoundPresetInfo("custom_mp3_end", Res.string.preset_custom_mp3_end),
        SoundPresetInfo("custom_mp3_sound_1", Res.string.preset_custom_sound_1),
        SoundPresetInfo("custom_mp3_sound_2", Res.string.preset_custom_sound_2),
        SoundPresetInfo("custom_mp3_sound_3", Res.string.preset_custom_sound_3),
        SoundPresetInfo("beep_standard", Res.string.preset_beep_standard),
        SoundPresetInfo("beep_soft", Res.string.preset_beep_soft),
        SoundPresetInfo("ack", Res.string.preset_ack),
        SoundPresetInfo("cdma_incall", Res.string.preset_cdma_incall),
        SoundPresetInfo("cdma_guard", Res.string.preset_cdma_guard),
        SoundPresetInfo("dtmf_1", Res.string.preset_dtmf_1),
        SoundPresetInfo("dtmf_3", Res.string.preset_dtmf_3),
        SoundPresetInfo("cdma_high", Res.string.preset_cdma_high),
        SoundPresetInfo("finish_default", Res.string.preset_finish_default),
    )

    fun isValidId(id: String): Boolean = all.any { it.id == id }
}
