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
    val isPaused: Boolean = false,
    val isFinished: Boolean = false,
    val isLoading: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    /** Идентификаторы пресетов звука, задаются при [TimerIntent.Load]. */
    val workStartSoundPresetId: String = "",
    val restStartSoundPresetId: String = "",
    val finishSoundPresetId: String = "",
    /** Звук каждую секунду в окне предупреждения перед концом фазы «Работа». */
    val workPhaseWarningSoundPresetId: String = "",
    /** 0 — без предупреждения; иначе в последние N секунд фазы «Работа» — сигнал каждую секунду. */
    val workPhaseEndWarningSeconds: Int = 0
) {
    val currentPhase: TimerPhase? get() = phases.getOrNull(currentPhaseIndex)
    val nextPhase: TimerPhase? get() = phases.getOrNull(currentPhaseIndex + 1)
    val totalPhases: Int get() = phases.size

    /** True когда текущий сегмент — обратный отсчёт подготовки перед блоком. */
    val isPrepBeforeWork: Boolean get() = currentPhase?.type == PhaseType.Prep

    /**
     * Следующая значимая фаза для отображения в UI: пропускает Prep-фазы,
     * показывая сразу предстоящую Work-фазу.
     */
    val nextSignificantPhase: TimerPhase?
        get() {
            val next = phases.getOrNull(currentPhaseIndex + 1) ?: return null
            return if (next.type == PhaseType.Prep) phases.getOrNull(currentPhaseIndex + 2) else next
        }

    val phaseProgress: Float
        get() {
            val duration = currentPhase?.durationSeconds ?: return 0f
            return if (duration == 0) 1f
            else (1f - secondsRemaining.toFloat() / duration).coerceIn(0f, 1f)
        }

    val overallProgress: Float
        get() = if (phases.isEmpty()) 0f else (currentPhaseIndex + phaseProgress) / phases.size

    val isInWorkEndWarning: Boolean
        get() = isInWarningWindow(secondsRemaining)

    /**
     * Проверяет, попадает ли [secondsRemaining] в окно предупреждения конца Work-фазы.
     * Используется как в [isInWorkEndWarning] (для UI), так и в [TimerStore.tick]
     * (для значения после тика), чтобы избежать дублирования условия.
     */
    fun isInWarningWindow(secondsRemaining: Int): Boolean =
        workPhaseEndWarningSeconds > 0 &&
            currentPhase?.type == PhaseType.Work &&
            secondsRemaining in 1..workPhaseEndWarningSeconds
}

data class TimerPhase(
    val name: String,
    val type: PhaseType,
    val durationSeconds: Int,
    val repeatLabel: String? = null,
)

enum class PhaseType { Prep, Work, Rest }

/**
 * Настройки таймера, передаваемые при загрузке тренировки.
 * Группирует параметры [TimerIntent.Load], не связанные с конкретной тренировкой.
 */
data class TimerLoadSettings(
    val blockPrepDurationSeconds: Int,
    val soundEnabled: Boolean,
    val vibrationEnabled: Boolean,
    val workStartSoundPresetId: String,
    val restStartSoundPresetId: String,
    val finishSoundPresetId: String,
    val workPhaseWarningSoundPresetId: String,
    val workPhaseEndWarningSeconds: Int,
)

sealed interface TimerIntent {
    data class Load(
        val workoutId: Long,
        val settings: TimerLoadSettings,
        /** Локализованная подпись фазы отдыха (для таймера и блоков). */
        val restPhaseDisplayName: String,
    ) : TimerIntent
    data object TogglePause : TimerIntent
    data object SkipPhase : TimerIntent
    /** Вернуться к предыдущей фазе или к подготовке перед текущим подходом. */
    data object PreviousPhase : TimerIntent
    /** Сдвинуть оставшееся время текущего сегмента (подготовка / работа / отдых). */
    data class AdjustRemainingSeconds(val delta: Int) : TimerIntent
    data object Finish : TimerIntent
    data object Tick : TimerIntent  // called every second by platform timer
}

sealed interface TimerEffect {
    /** Короткий сигнал каждую секунду на этапе подготовки перед блоком. */
    data object PlayPrepTickSound : TimerEffect
    /** Конец подготовки — переход к работе: более протяжный звук. */
    data class PlayPrepEndSound(val presetId: String) : TimerEffect
    /** Конец подготовки — выраженная вибрация. */
    data object VibratePrepEnd : TimerEffect
    data class PlayWorkSound(val presetId: String) : TimerEffect
    data class PlayRestSound(val presetId: String) : TimerEffect
    data class PlayFinishSound(val presetId: String) : TimerEffect
    /** Вибрация при старте фазы «Работа» или «Отдых». */
    data object Vibrate : TimerEffect
    data object VibrateFinish : TimerEffect
    /**
     * Предупреждение в конце фазы «Работа».
     * [secondsRemainingAfterTick] — сколько секунд осталось после тика (1..N в окне предупреждения).
     * [withVibration] — однократная вибрация в первую секунду окна предупреждения.
     * [withSound] — воспроизвести звук предупреждения.
     * [presetId] — идентификатор звукового пресета.
     */
    data class WorkPhaseEndAlert(
        val secondsRemainingAfterTick: Int,
        val withVibration: Boolean,
        val withSound: Boolean,
        val presetId: String,
    ) : TimerEffect
    data object NavigateBack : TimerEffect
}
