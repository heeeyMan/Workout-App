package com.workout.shared.platform

/**
 * Stub implementation. Will be wired up to the real [TimerFeedback] in the androidApp module later.
 */
class AndroidAudioFeedback : AudioFeedback {

    override fun playPrepTickTone() {
        // No-op stub.
    }

    override fun playPrepEndTone(workPresetId: String) {
        // No-op stub.
    }

    override fun playWorkTone(workPresetId: String) {
        // No-op stub.
    }

    override fun playRestTone(restPresetId: String) {
        // No-op stub.
    }

    override fun playFinishTone(presetId: String) {
        // No-op stub.
    }

    override fun playWarningTone(presetId: String) {
        // No-op stub.
    }

    override fun previewPreset(presetId: String) {
        // No-op stub.
    }
}
