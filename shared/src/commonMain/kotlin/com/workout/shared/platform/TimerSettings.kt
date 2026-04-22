package com.workout.shared.platform

interface TimerSettings {
    var blockPrepDurationSeconds: Int
    var soundEnabled: Boolean
    var vibrationEnabled: Boolean
    var workPhaseEndWarningSeconds: Int
    var timerQuickAdjustEnabled: Boolean
    var workStartSoundPresetId: String
    var restStartSoundPresetId: String
    var workoutFinishSoundPresetId: String
    var workPhaseWarningSoundPresetId: String
    var onboardingCompleted: Boolean
    var workoutsCompletedCount: Int
}
