package com.workout.shared.feature.home

import com.workout.core.repository.WorkoutRepository
import com.workout.shared.mvi.BaseStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class HomeStore(
    private val workoutRepository: WorkoutRepository
) : BaseStore<HomeState, HomeIntent, HomeEffect>(HomeState()) {

    private var isStartingWorkout = false
    private var workoutsJob: Job? = null

    init {
        loadWorkouts()
    }

    override fun dispatch(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.StartWorkout -> startWorkout(intent.workoutId)
            is HomeIntent.EditWorkout -> emitEffect(HomeEffect.NavigateToEditWorkout(intent.workoutId))
            is HomeIntent.CreateWorkout -> emitEffect(HomeEffect.NavigateToCreateWorkout)
            is HomeIntent.RequestDelete -> setState { copy(pendingDeleteId = intent.workoutId) }
            is HomeIntent.ConfirmDelete -> confirmDelete()
            is HomeIntent.CancelDelete -> setState { copy(pendingDeleteId = null) }
        }
    }

    private fun startWorkout(workoutId: Long) {
        if (isStartingWorkout) return
        isStartingWorkout = true
        scope.launch {
            try {
                workoutRepository.markWorkoutStarted(workoutId)
                emitEffect(HomeEffect.NavigateToTimer(workoutId))
            } finally {
                isStartingWorkout = false
            }
        }
    }

    private fun loadWorkouts() {
        if (workoutsJob?.isActive == true) return
        workoutsJob = scope.launch {
            workoutRepository.getWorkouts().collect { workouts ->
                setState { copy(workouts = workouts, isLoading = false) }
            }
        }
    }

    private fun confirmDelete() {
        val id = state.value.pendingDeleteId ?: return
        setState { copy(pendingDeleteId = null) }
        scope.launch {
            workoutRepository.deleteWorkout(id)
        }
    }
}
