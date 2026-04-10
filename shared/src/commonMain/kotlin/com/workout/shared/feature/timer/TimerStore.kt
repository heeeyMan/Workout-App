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
            is TimerIntent.Load -> load(intent.workoutId)
            is TimerIntent.TogglePause -> togglePause()
            is TimerIntent.SkipPhase -> skipPhase()
            is TimerIntent.Finish -> finish()
            is TimerIntent.ToggleSound -> setState { copy(soundEnabled = !soundEnabled) }
            is TimerIntent.ToggleVibration -> setState { copy(vibrationEnabled = !vibrationEnabled) }
            is TimerIntent.Tick -> tick()
        }
    }

    private fun load(workoutId: Long) {
        scope.launch {
            val workout = workoutRepository.getWorkoutById(workoutId) ?: return@launch
            val phases = workout.blocks.flatMap { it.toPhases() }
            setState {
                copy(
                    workoutName = workout.name,
                    phases = phases,
                    currentPhaseIndex = 0,
                    secondsRemaining = phases.firstOrNull()?.durationSeconds ?: 0,
                    isLoading = false
                )
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

        if (newSeconds == 10 && s.alertAt10Seconds) {
            emitEffect(TimerEffect.Alert10Seconds)
        }

        if (newSeconds <= 0) {
            advancePhase()
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
            emitEffect(TimerEffect.PlayFinishSound)
            return
        }

        val nextPhase = s.phases[nextIndex]
        setState {
            copy(
                currentPhaseIndex = nextIndex,
                secondsRemaining = nextPhase.durationSeconds
            )
        }

        if (state.value.soundEnabled) {
            emitEffect(if (nextPhase.type == PhaseType.Work) TimerEffect.PlayWorkSound else TimerEffect.PlayRestSound)
        }
        if (state.value.vibrationEnabled) {
            emitEffect(TimerEffect.Vibrate)
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
                        repeatLabel = "$rep / $repeats"
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
