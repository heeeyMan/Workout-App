package com.workout.shared.feature.timer

data class TimerState(
    val workoutName: String = "",
    val phases: List<TimerPhase> = emptyList(),
    val currentPhaseIndex: Int = 0,
    val secondsRemaining: Int = 0,
    /**
     * Длительность паузы перед стартом блока (из настроек). 0 — без отсчёта.
     * Хранится в состоянии, чтобы прогресс и таймер были согласованы на всю сессию.
     */
    val blockPrepDurationSeconds: Int = 0,
    /** Обратный отсчёт перед первой фазой «Работа» в блоке (см. [TimerPhase.needsBlockPrepStart]). */
    val isPrepBeforeWork: Boolean = false,
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
            val phase = currentPhase ?: return 0f
            if (isPrepBeforeWork && phase.type == PhaseType.Work) {
                val prep = blockPrepDurationSeconds
                return if (prep <= 0) 1f else 1f - (secondsRemaining.toFloat() / prep)
            }
            val duration = phase.durationSeconds
            return if (duration == 0) 1f else 1f - (secondsRemaining.toFloat() / duration)
        }
}

data class TimerPhase(
    val name: String,
    val type: PhaseType,
    val durationSeconds: Int,
    val repeatLabel: String? = null,  // e.g. "2 / 5"
    /** True только для первого подхода в блоке упражнения — перед ним показывается пауза из настроек. */
    val needsBlockPrepStart: Boolean = false
)

enum class PhaseType { Work, Rest }

sealed interface TimerIntent {
    data class Load(
        val workoutId: Long,
        val blockPrepDurationSeconds: Int,
        val soundEnabled: Boolean,
        val vibrationEnabled: Boolean
    ) : TimerIntent
    data object TogglePause : TimerIntent
    data object SkipPhase : TimerIntent
    data object Finish : TimerIntent
    data object Tick : TimerIntent  // called every second by platform timer
}

sealed interface TimerEffect {
    /** Короткий сигнал каждую секунду на этапе подготовки перед блоком. */
    data object PlayPrepTickSound : TimerEffect
    /** Конец подготовки — переход к работе: более протяжный звук. */
    data object PlayPrepEndSound : TimerEffect
    /** Конец подготовки — выраженная вибрация. */
    data object VibratePrepEnd : TimerEffect
    data object PlayWorkSound : TimerEffect
    data object PlayRestSound : TimerEffect
    data object PlayFinishSound : TimerEffect
    data object Vibrate : TimerEffect
    data object VibrateFinish : TimerEffect
    data object Alert10Seconds : TimerEffect
    data object NavigateBack : TimerEffect
}
