package com.workout.shared.platform

interface AudioFeedback {
    fun playPrepTickTone()
    fun playPrepEndTone(workPresetId: String)
    fun playWorkTone(workPresetId: String)
    fun playRestTone(restPresetId: String)
    fun playFinishTone(presetId: String)
    fun playWarningTone(presetId: String)
    fun previewPreset(presetId: String)
}
