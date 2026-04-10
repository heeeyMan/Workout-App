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

    override fun dispatch(intent: TimerIntent) {
        when (intent) {
            is TimerIntent.Load -> load(
                intent.workoutId,
                intent.blockPrepDurationSeconds,
                intent.soundEnabled,
                intent.vibrationEnabled
            )
            is TimerIntent.TogglePause -> togglePause()
            is TimerIntent.SkipPhase -> skipPhase()
            is TimerIntent.Finish -> finish()
            is TimerIntent.Tick -> tick()
        }
    }

    private fun load(
        workoutId: Long,
        blockPrepDurationSeconds: Int,
        soundEnabled: Boolean,
        vibrationEnabled: Boolean
    ) {
        scope.launch {
            val workout = workoutRepository.getWorkoutById(workoutId) ?: return@launch
            val phases = workout.blocks.flatMap { it.toPhases() }
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

        val newSeconds = s.secondsRemaining - 1

        if (s.isPrepBeforeWork && s.soundEnabled && newSeconds > 0) {
            emitEffect(TimerEffect.PlayPrepTickSound)
        }

        if (!s.isPrepBeforeWork && newSeconds == 10 && s.alertAt10Seconds &&
            (s.soundEnabled || s.vibrationEnabled)
        ) {
            emitEffect(TimerEffect.Alert10Seconds)
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
                advancePhase()
            }
        } else {
            setState { copy(secondsRemaining = newSeconds) }
        }
    }

    private fun advancePhase() {
        val s = state.value
        val nextIndex = s.currentPhaseIndex + 1

        if (nextIndex >= s.phases.size) {
            timerJob?.cancel()
            setState { copy(isFinished = true, secondsRemaining = 0, isPrepBeforeWork = false) }
            if (s.soundEnabled) {
                emitEffect(TimerEffect.PlayFinishSound)
            }
            if (s.vibrationEnabled) {
                emitEffect(TimerEffect.VibrateFinish)
            }
            return
        }

        val nextPhase = s.phases[nextIndex]
        val prepLen = s.blockPrepDurationSeconds
        when {
            nextPhase.type == PhaseType.Work && prepLen > 0 && nextPhase.needsBlockPrepStart -> {
                setState {
                    copy(
                        currentPhaseIndex = nextIndex,
                        isPrepBeforeWork = true,
                        secondsRemaining = prepLen
                    )
                }
                if (state.value.soundEnabled) {
                    emitEffect(TimerEffect.PlayPrepTickSound)
                }
            }
            nextPhase.type == PhaseType.Work -> {
                setState {
                    copy(
                        currentPhaseIndex = nextIndex,
                        isPrepBeforeWork = false,
                        secondsRemaining = nextPhase.durationSeconds
                    )
                }
                if (state.value.soundEnabled) {
                    emitEffect(TimerEffect.PlayWorkSound)
                }
                if (state.value.vibrationEnabled) {
                    emitEffect(TimerEffect.Vibrate)
                }
            }
            else -> {
                setState {
                    copy(
                        currentPhaseIndex = nextIndex,
                        isPrepBeforeWork = false,
                        secondsRemaining = nextPhase.durationSeconds
                    )
                }
                if (state.value.soundEnabled) {
                    emitEffect(TimerEffect.PlayRestSound)
                }
                if (state.value.vibrationEnabled) {
                    emitEffect(TimerEffect.Vibrate)
                }
            }
        }
    }

    private fun skipPhase() {
        advancePhase()
    }

    private fun togglePause() {
        val paused = !state.value.isPaused
        setState { copy(isPaused = paused) }
        if (!paused) startTimer() else timerJob?.cancel()
    }

    private fun finish() {
        timerJob?.cancel()
        emitEffect(TimerEffect.NavigateBack)
    }

    override fun destroy() {
        timerJob?.cancel()
        super.destroy()
    }

    // Expand a Block into flat list of timer phases
    private fun Block.toPhases(): List<TimerPhase> = when (this) {
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
                            name = "Отдых",
                            type = PhaseType.Rest,
                            durationSeconds = restDurationSeconds,
                            repeatLabel = "$rep / $repeats"
                        )
                    )
                }
            }
        }
        is Block.Rest -> listOf(
            TimerPhase(name = "Отдых", type = PhaseType.Rest, durationSeconds = durationSeconds)
        )
    }
}
