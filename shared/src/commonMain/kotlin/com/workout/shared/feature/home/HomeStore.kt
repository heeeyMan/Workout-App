package com.workout.shared.feature.home

import com.workout.core.repository.WorkoutRepository
import com.workout.shared.mvi.BaseStore
import kotlinx.coroutines.launch

class HomeStore(
    private val workoutRepository: WorkoutRepository
) : BaseStore<HomeState, HomeIntent, HomeEffect>(HomeState()) {

    init {
        dispatch(HomeIntent.LoadWorkouts)
    }

    override fun dispatch(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.LoadWorkouts -> loadWorkouts()
            is HomeIntent.StartWorkout -> startWorkout(intent.workoutId)
            is HomeIntent.EditWorkout -> emitEffect(HomeEffect.NavigateToEditWorkout(intent.workoutId))
            is HomeIntent.CreateWorkout -> emitEffect(HomeEffect.NavigateToCreateWorkout)
            is HomeIntent.RequestDelete -> setState { copy(pendingDeleteId = intent.workoutId) }
            is HomeIntent.ConfirmDelete -> confirmDelete()
            is HomeIntent.CancelDelete -> setState { copy(pendingDeleteId = null) }
        }
    }

    private fun startWorkout(workoutId: Long) {
        scope.launch {
            workoutRepository.markWorkoutStarted(workoutId)
            emitEffect(HomeEffect.NavigateToTimer(workoutId))
        }
    }

    private fun loadWorkouts() {
        scope.launch {
            workoutRepository.getWorkouts().collect { workouts ->
                setState { copy(workouts = workouts, isLoading = false) }
            }
        }
    }

    private fun confirmDelete() {
        val id = state.value.pendingDeleteId ?: return
        scope.launch {
            workoutRepository.deleteWorkout(id)
            setState { copy(pendingDeleteId = null) }
        }
    }
}
