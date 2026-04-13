package com.workout.shared.feature.timer

import com.workout.core.model.Block
import com.workout.core.repository.WorkoutRepository
import com.workout.shared.mvi.BaseStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TimerStore(
    private val workoutRepository: WorkoutRepository
) : BaseStore<TimerState, TimerIntent, TimerEffect>(TimerState()) {

    private var timerJob: Job? = null
    private var phaseEndWrapUpJob: Job? = null

    /** После показа «0» и полной полоски — пауза, затем звук и смена фазы. */
    private companion object {
        private const val PHASE_END_UI_DELAY_MS = 150L
    }

    override fun dispatch(intent: TimerIntent) {
        when (intent) {
            is TimerIntent.Load -> load(
                intent.workoutId,
                intent.blockPrepDurationSeconds,
                intent.soundEnabled,
                intent.vibrationEnabled,
                intent.workStartSoundPresetId,
                intent.restStartSoundPresetId,
                intent.finishSoundPresetId,
                intent.workPhaseEndWarningSeconds,
                intent.restPhaseDisplayName
            )
            is TimerIntent.TogglePause -> togglePause()
            is TimerIntent.SkipPhase -> skipPhase()
            is TimerIntent.PreviousPhase -> previousPhase()
            is TimerIntent.AdjustRemainingSeconds -> adjustRemainingSeconds(intent.delta)
            is TimerIntent.Finish -> finish()
            is TimerIntent.Tick -> tick()
        }
    }

    private fun load(
        workoutId: Long,
        blockPrepDurationSeconds: Int,
        soundEnabled: Boolean,
        vibrationEnabled: Boolean,
        workStartSoundPresetId: String,
        restStartSoundPresetId: String,
        finishSoundPresetId: String,
        workPhaseEndWarningSeconds: Int,
        restPhaseDisplayName: String
    ) {
        scope.launch {
            cancelPhaseEndWrapUp(restoreLastSecond = false)
            val workout = workoutRepository.getWorkoutById(workoutId) ?: return@launch
            val phases = workout.blocks.flatMap { it.toPhases(restPhaseDisplayName) }
            val prepLen = blockPrepDurationSeconds.coerceAtLeast(0)
            val first = phases.firstOrNull()
            val startWithPrep = prepLen > 0 &&
                first?.type == PhaseType.Work &&
                first.needsBlockPrepStart
            setState {
                copy(
                    workoutName = workout.name,
                    phases = phases,
                    currentPhaseIndex = 0,
                    blockPrepDurationSeconds = prepLen,
                    isPrepBeforeWork = startWithPrep,
                    secondsRemaining = when {
                        startWithPrep -> prepLen
                        first != null -> first.durationSeconds
                        else -> 0
                    },
                    soundEnabled = soundEnabled,
                    vibrationEnabled = vibrationEnabled,
                    workStartSoundPresetId = workStartSoundPresetId,
                    restStartSoundPresetId = restStartSoundPresetId,
                    finishSoundPresetId = finishSoundPresetId,
                    workPhaseEndWarningSeconds = workPhaseEndWarningSeconds.coerceAtLeast(0),
                    isLoading = false
                )
            }
            if (startWithPrep && soundEnabled) {
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

    private fun tick() {
        val s = state.value
        if (s.isPaused || s.isFinished || s.isLoading) return
        // Ждём отложенный переход фазы (полоска «0» → звук → новая стадия); повторный Tick не планирует снова.
        if (!s.isPrepBeforeWork && s.secondsRemaining == 0 && s.currentPhase != null) return

        val newSeconds = s.secondsRemaining - 1

        if (s.isPrepBeforeWork && s.soundEnabled && newSeconds > 0) {
            emitEffect(TimerEffect.PlayPrepTickSound)
        }

        val warnWindow = s.workPhaseEndWarningSeconds
        if (warnWindow > 0 && !s.isPrepBeforeWork &&
            s.currentPhase?.type == PhaseType.Work &&
            newSeconds in 1..warnWindow
        ) {
            val playSound = s.soundEnabled
            val vibOnce = s.vibrationEnabled && newSeconds == warnWindow
            if (playSound || vibOnce) {
                emitEffect(
                    TimerEffect.Alert10Seconds(
                        secondsRemainingAfterTick = newSeconds,
                        withVibration = vibOnce,
                    ),
                )
            }
        }

        if (newSeconds <= 0) {
            if (s.isPrepBeforeWork) {
                val phase = s.currentPhase ?: return
                setState {
                    copy(
                        isPrepBeforeWork = false,
                        secondsRemaining = phase.durationSeconds
                    )
                }
                if (state.value.soundEnabled) {
                    emitEffect(TimerEffect.PlayPrepEndSound)
                }
                if (state.value.vibrationEnabled) {
                    emitEffect(TimerEffect.VibratePrepEnd)
                }
            } else {
                schedulePhaseEndThenAdvance()
            }
        } else {
            setState { copy(secondsRemaining = newSeconds) }
        }
    }

    /**
     * Показать конец фазы (полоска 1f при `secondsRemaining == 0`), затем после паузы —
     * звук и только потом смена стадии ([feedbackBeforeUi] = true).
     */
    private fun schedulePhaseEndThenAdvance() {
        phaseEndWrapUpJob?.cancel()
        setState { copy(secondsRemaining = 0) }
        phaseEndWrapUpJob = scope.launch {
            delay(PHASE_END_UI_DELAY_MS)
            val cur = state.value
            if (cur.isFinished || cur.isLoading || cur.isPaused) return@launch
            if (cur.isPrepBeforeWork) return@launch
            if (cur.secondsRemaining != 0) return@launch
            advancePhase(feedbackBeforeUi = true)
        }
    }

    private fun cancelPhaseEndWrapUp(restoreLastSecond: Boolean) {
        phaseEndWrapUpJob?.cancel()
        phaseEndWrapUpJob = null
        if (restoreLastSecond) {
            val cur = state.value
            if (!cur.isPrepBeforeWork && !cur.isFinished && !cur.isLoading &&
                cur.secondsRemaining == 0 && cur.currentPhase != null
            ) {
                setState { copy(secondsRemaining = 1) }
            }
        }
    }

    /**
     * @param feedbackBeforeUi если true — сначала звук/вибрация следующей фазы, затем состояние
     * (после паузы на «0» в конце текущей фазы).
     */
    private fun advancePhase(feedbackBeforeUi: Boolean = false) {
        val s = state.value
        val soundOn = s.soundEnabled
        val vibOn = s.vibrationEnabled
        val nextIndex = s.currentPhaseIndex + 1

        if (nextIndex >= s.phases.size) {
            timerJob?.cancel()
            if (feedbackBeforeUi) {
                if (soundOn) emitEffect(TimerEffect.PlayFinishSound)
                if (vibOn) emitEffect(TimerEffect.VibrateFinish)
                setState { copy(isFinished = true, secondsRemaining = 0, isPrepBeforeWork = false) }
            } else {
                setState { copy(isFinished = true, secondsRemaining = 0, isPrepBeforeWork = false) }
                if (soundOn) emitEffect(TimerEffect.PlayFinishSound)
                if (vibOn) emitEffect(TimerEffect.VibrateFinish)
            }
            return
        }

        val nextPhase = s.phases[nextIndex]
        val prepLen = s.blockPrepDurationSeconds
        when {
            nextPhase.type == PhaseType.Work && prepLen > 0 && nextPhase.needsBlockPrepStart -> {
                val apply = {
                    setState {
                        copy(
                            currentPhaseIndex = nextIndex,
                            isPrepBeforeWork = true,
                            secondsRemaining = prepLen
                        )
                    }
                }
                val play = {
                    if (soundOn) emitEffect(TimerEffect.PlayPrepTickSound)
                }
                if (feedbackBeforeUi) {
                    play()
                    apply()
                } else {
                    apply()
                    play()
                }
            }
            nextPhase.type == PhaseType.Work -> {
                val apply = {
                    setState {
                        copy(
                            currentPhaseIndex = nextIndex,
                            isPrepBeforeWork = false,
                            secondsRemaining = nextPhase.durationSeconds
                        )
                    }
                }
                val play = {
                    if (soundOn) emitEffect(TimerEffect.PlayWorkSound)
                    if (vibOn) emitEffect(TimerEffect.Vibrate)
                }
                if (feedbackBeforeUi) {
                    play()
                    apply()
                } else {
                    apply()
                    play()
                }
            }
            else -> {
                val apply = {
                    setState {
                        copy(
                            currentPhaseIndex = nextIndex,
                            isPrepBeforeWork = false,
                            secondsRemaining = nextPhase.durationSeconds
                        )
                    }
                }
                val play = {
                    if (soundOn) emitEffect(TimerEffect.PlayRestSound)
                    if (vibOn) emitEffect(TimerEffect.Vibrate)
                }
                if (feedbackBeforeUi) {
                    play()
                    apply()
                } else {
                    apply()
                    play()
                }
            }
        }
    }

    private fun skipPhase() {
        cancelPhaseEndWrapUp(restoreLastSecond = false)
        advancePhase(feedbackBeforeUi = false)
    }

    private fun enterPhaseAtIndex(index: Int) {
        val s = state.value
        if (index < 0 || index >= s.phases.size) return
        val p = s.phases[index]
        val prep = s.blockPrepDurationSeconds
        val startWithPrep = p.type == PhaseType.Work && prep > 0 && p.needsBlockPrepStart
        setState {
            copy(
                currentPhaseIndex = index,
                isPrepBeforeWork = startWithPrep,
                secondsRemaining = if (startWithPrep) prep else p.durationSeconds
            )
        }
    }

    private fun previousPhase() {
        cancelPhaseEndWrapUp(restoreLastSecond = true)
        val s = state.value
        if (s.isFinished || s.isLoading) return

        if (s.currentPhaseIndex == 0 && s.isPrepBeforeWork) return

        if (s.isPrepBeforeWork) {
            enterPhaseAtIndex(s.currentPhaseIndex - 1)
            return
        }

        val phase = s.currentPhase ?: return
        if (phase.type == PhaseType.Work && phase.needsBlockPrepStart && s.blockPrepDurationSeconds > 0) {
            setState {
                copy(
                    isPrepBeforeWork = true,
                    secondsRemaining = blockPrepDurationSeconds
                )
            }
            return
        }

        if (s.currentPhaseIndex == 0) {
            setState { copy(secondsRemaining = phase.durationSeconds) }
            return
        }

        enterPhaseAtIndex(s.currentPhaseIndex - 1)
    }

    private fun adjustRemainingSeconds(delta: Int) {
        if (delta == 0) return
        cancelPhaseEndWrapUp(restoreLastSecond = false)
        val s = state.value
        if (s.isFinished || s.isLoading) return
        val maxCap = if (s.isPrepBeforeWork) {
            s.blockPrepDurationSeconds
        } else {
            s.currentPhase?.durationSeconds ?: return
        }
        if (maxCap <= 0) return
        val newVal = (s.secondsRemaining + delta).coerceIn(1, maxCap)
        setState { copy(secondsRemaining = newVal) }
    }

    private fun togglePause() {
        val paused = !state.value.isPaused
        if (paused) {
            cancelPhaseEndWrapUp(restoreLastSecond = true)
        }
        setState { copy(isPaused = paused) }
        if (!paused) startTimer() else timerJob?.cancel()
    }

    private fun finish() {
        cancelPhaseEndWrapUp(restoreLastSecond = false)
        timerJob?.cancel()
        emitEffect(TimerEffect.NavigateBack)
    }

    override fun destroy() {
        cancelPhaseEndWrapUp(restoreLastSecond = false)
        timerJob?.cancel()
        super.destroy()
    }

    // Expand a Block into flat list of timer phases
    private fun Block.toPhases(restPhaseDisplayName: String): List<TimerPhase> = when (this) {
        is Block.Exercise -> (1..repeats).flatMap { rep ->
            buildList {
                add(
                    TimerPhase(
                        name = name,
                        type = PhaseType.Work,
                        durationSeconds = workDurationSeconds,
                        repeatLabel = "$rep / $repeats",
                        needsBlockPrepStart = rep == 1
                    )
                )
                if (restDurationSeconds > 0) {
                    add(
                        TimerPhase(
                            name = restPhaseDisplayName,
                            type = PhaseType.Rest,
                            durationSeconds = restDurationSeconds,
                            repeatLabel = "$rep / $repeats"
                        )
                    )
                }
            }
        }
        is Block.Rest -> listOf(
            TimerPhase(name = restPhaseDisplayName, type = PhaseType.Rest, durationSeconds = durationSeconds)
        )
    }
}
