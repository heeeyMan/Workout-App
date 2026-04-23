package com.workout.shared.feature.timer

import com.workout.core.repository.WorkoutRepository
import com.workout.shared.mvi.BaseStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TimerStore(
    private val workoutRepository: WorkoutRepository
) : BaseStore<TimerState, TimerIntent, TimerEffect>(TimerState()) {

    private var timerJob: Job? = null
    private var phaseTransitionJob: Job? = null

    override fun dispatch(intent: TimerIntent) {
        when (intent) {
            is TimerIntent.Load -> load(intent.workoutId, intent.settings, intent.restPhaseDisplayName)
            is TimerIntent.TogglePause -> togglePause()
            is TimerIntent.SkipPhase -> skipPhase()
            is TimerIntent.PreviousPhase -> previousPhase()
            is TimerIntent.AdjustRemainingSeconds -> adjustRemainingSeconds(intent.delta)
            is TimerIntent.Finish -> finish()
            is TimerIntent.Tick -> tick()
        }
    }

    private fun load(workoutId: Long, settings: TimerLoadSettings, restPhaseDisplayName: String) {
        scope.launch {
            val workout = workoutRepository.getWorkoutById(workoutId) ?: return@launch
            val prepLen = settings.blockPrepDurationSeconds.coerceAtLeast(0)
            val phases = workout.blocks.flatMap { it.toPhases(restPhaseDisplayName, prepLen) }
            val first = phases.firstOrNull()
            setState {
                copy(
                    workoutName = workout.name,
                    phases = phases,
                    currentPhaseIndex = 0,
                    blockPrepDurationSeconds = prepLen,
                    secondsRemaining = first?.durationSeconds ?: 0,
                    soundEnabled = settings.soundEnabled,
                    vibrationEnabled = settings.vibrationEnabled,
                    workStartSoundPresetId = settings.workStartSoundPresetId,
                    restStartSoundPresetId = settings.restStartSoundPresetId,
                    finishSoundPresetId = settings.finishSoundPresetId,
                    workPhaseWarningSoundPresetId = settings.workPhaseWarningSoundPresetId,
                    workPhaseEndWarningSeconds = settings.workPhaseEndWarningSeconds.coerceAtLeast(0),
                    isLoading = false
                )
            }
            if (first?.type == PhaseType.Prep && settings.soundEnabled) {
                emitEffect(TimerEffect.PlayPrepTickSound)
            }
            startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (true) {
                delay(1000L)
                dispatch(TimerIntent.Tick)
            }
        }
    }

    // Показываем полностью заполненный прогресс (secondsRemaining=0), затем через небольшую
    // задержку переходим к следующей фазе и перезапускаем тикер.
    private fun scheduleTransition() {
        phaseTransitionJob?.cancel()
        phaseTransitionJob = scope.launch {
            delay(100L)
            phaseTransitionJob = null
            advancePhase()
            if (!state.value.isFinished && !state.value.isPaused) {
                startTimer()
            }
        }
    }

    private fun tick() {
        val s = state.value
        if (s.isPaused || s.isFinished || s.isLoading) return
        if (phaseTransitionJob?.isActive == true) return  // ждём паузу отображения полного прогресса

        val newSeconds = s.secondsRemaining - 1

        if (s.currentPhase?.type == PhaseType.Prep && s.soundEnabled && newSeconds > 0) {
            emitEffect(TimerEffect.PlayPrepTickSound)
        }

        if (s.isInWarningWindow(newSeconds)) {
            val vibOnce = s.vibrationEnabled && newSeconds == s.workPhaseEndWarningSeconds
            if (s.soundEnabled || vibOnce) {
                emitEffect(TimerEffect.WorkPhaseEndAlert(
                    secondsRemainingAfterTick = newSeconds,
                    withVibration = vibOnce,
                ))
            }
        }

        if (newSeconds <= 0) {
            // Сначала показываем заполненный прогресс (100%), затем через паузу переходим
            setState { copy(secondsRemaining = 0) }
            timerJob?.cancel()
            scheduleTransition()
        } else {
            setState { copy(secondsRemaining = newSeconds) }
        }
    }

    private fun advancePhase() {
        val s = state.value
        val nextIndex = s.currentPhaseIndex + 1

        if (nextIndex >= s.phases.size) {
            timerJob?.cancel()
            setState { copy(isFinished = true, secondsRemaining = 0) }
            if (s.soundEnabled) emitEffect(TimerEffect.PlayFinishSound)
            if (s.vibrationEnabled) emitEffect(TimerEffect.VibrateFinish)
            return
        }

        val nextPhase = s.phases[nextIndex]
        setState { copy(currentPhaseIndex = nextIndex, secondsRemaining = nextPhase.durationSeconds) }

        // s.currentPhase — фаза, которую покидаем (захвачена до setState)
        when (nextPhase.type) {
            PhaseType.Prep -> {
                if (s.soundEnabled) emitEffect(TimerEffect.PlayPrepTickSound)
            }
            PhaseType.Work -> {
                if (s.currentPhase?.type == PhaseType.Prep) {
                    // Переход подготовка → работа: особый звук и вибрация
                    if (s.soundEnabled) emitEffect(TimerEffect.PlayPrepEndSound)
                    if (s.vibrationEnabled) emitEffect(TimerEffect.VibratePrepEnd)
                } else {
                    if (s.soundEnabled) emitEffect(TimerEffect.PlayWorkSound)
                    if (s.vibrationEnabled) emitEffect(TimerEffect.VibrateWork)
                }
            }
            PhaseType.Rest -> {
                if (s.soundEnabled) emitEffect(TimerEffect.PlayRestSound)
                if (s.vibrationEnabled) emitEffect(TimerEffect.VibrateRest)
            }
        }
    }

    private fun skipPhase() {
        phaseTransitionJob?.cancel()
        phaseTransitionJob = null
        // Отменяем и перезапускаем timerJob, чтобы новая фаза начиналась ровно с 1 с
        timerJob?.cancel()
        advancePhase()
        if (!state.value.isFinished && !state.value.isPaused) {
            startTimer()
        }
    }

    private fun enterPhaseAtIndex(index: Int) {
        val s = state.value
        if (index < 0 || index >= s.phases.size) return
        val p = s.phases[index]
        setState { copy(currentPhaseIndex = index, secondsRemaining = p.durationSeconds) }
    }

    private fun previousPhase() {
        // Если тикер был остановлен из-за отображения полного прогресса — отменяем переход и перезапускаем
        if (phaseTransitionJob?.isActive == true) {
            phaseTransitionJob?.cancel()
            phaseTransitionJob = null
            if (!state.value.isPaused) startTimer()
        }

        val s = state.value
        if (s.isFinished || s.isLoading) return

        // На первой фазе — перезапустить её с начала
        if (s.currentPhaseIndex == 0) {
            enterPhaseAtIndex(0)
            return
        }

        // Иначе — перейти к предыдущей фазе (может быть Prep перед текущим подходом)
        enterPhaseAtIndex(s.currentPhaseIndex - 1)
    }

    private fun adjustRemainingSeconds(delta: Int) {
        if (delta == 0) return
        val s = state.value
        if (s.isFinished || s.isLoading) return
        val maxCap = s.currentPhase?.durationSeconds ?: return
        if (maxCap <= 0) return
        val newVal = (s.secondsRemaining + delta).coerceIn(1, maxCap)
        setState { copy(secondsRemaining = newVal) }
    }

    private fun togglePause() {
        val paused = !state.value.isPaused
        setState { copy(isPaused = paused) }
        if (!paused) {
            // Возобновление: если secondsRemaining==0, мы были на этапе отображения полного прогресса
            if (state.value.secondsRemaining == 0 && !state.value.isFinished) {
                scheduleTransition()
            } else {
                startTimer()
            }
        } else {
            timerJob?.cancel()
            phaseTransitionJob?.cancel()
            phaseTransitionJob = null
        }
    }

    private fun finish() {
        timerJob?.cancel()
        emitEffect(TimerEffect.NavigateBack)
    }

    override fun destroy() {
        timerJob?.cancel()
        phaseTransitionJob?.cancel()
        super.destroy()
    }
}
