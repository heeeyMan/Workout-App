package com.workout.shared.feature.timer

data class TimerState(
    val workoutName: String = "",
    val phases: List<TimerPhase> = emptyList(),
    val currentPhaseIndex: Int = 0,
    val secondsRemaining: Int = 0,
    val isPaused: Boolean = false,
    val isFinished: Boolean = false,
    val isLoading: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val alertAt10Seconds: Boolean = true
) {
    val currentPhase: TimerPhase? get() = phases.getOrNull(currentPhaseIndex)
    val nextPhase: TimerPhase? get() = phases.getOrNull(currentPhaseIndex + 1)
    val totalPhases: Int get() = phases.size
    val overallProgress: Float
        get() = if (phases.isEmpty()) 0f else currentPhaseIndex.toFloat() / phases.size
    val phaseProgress: Float
        get() {
            val duration = currentPhase?.durationSeconds ?: return 0f
            return if (duration == 0) 1f else 1f - (secondsRemaining.toFloat() / duration)
        }
}

data class TimerPhase(
    val name: String,
    val type: PhaseType,
    val durationSeconds: Int,
    val repeatLabel: String? = null  // e.g. "2 / 5"
)

enum class PhaseType { Work, Rest }

sealed interface TimerIntent {
    data class Load(val workoutId: Long) : TimerIntent
    data object TogglePause : TimerIntent
    data object SkipPhase : TimerIntent
    data object Finish : TimerIntent
    data object ToggleSound : TimerIntent
    data object ToggleVibration : TimerIntent
    data object Tick : TimerIntent  // called every second by platform timer
}

sealed interface TimerEffect {
    data object PlayWorkSound : TimerEffect
    data object PlayRestSound : TimerEffect
    data object PlayFinishSound : TimerEffect
    data object Vibrate : TimerEffect
    data object Alert10Seconds : TimerEffect
    data object NavigateBack : TimerEffect
}
