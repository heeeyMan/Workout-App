package com.workout.shared.feature.workoutlist

import com.workout.core.repository.WorkoutRepository
import com.workout.shared.mvi.BaseStore
import kotlinx.coroutines.launch

class WorkoutListStore(
    private val workoutRepository: WorkoutRepository
) : BaseStore<WorkoutListState, WorkoutListIntent, WorkoutListEffect>(WorkoutListState()) {

    init {
        dispatch(WorkoutListIntent.LoadWorkouts)
    }

    override fun dispatch(intent: WorkoutListIntent) {
        when (intent) {
            is WorkoutListIntent.LoadWorkouts -> loadWorkouts()
            is WorkoutListIntent.SelectWorkout -> emitEffect(WorkoutListEffect.NavigateToTimer(intent.workoutId))
            is WorkoutListIntent.EditWorkout -> emitEffect(WorkoutListEffect.NavigateToEditWorkout(intent.workoutId))
            is WorkoutListIntent.RequestDelete -> setState { copy(pendingDeleteId = intent.workoutId) }
            is WorkoutListIntent.ConfirmDelete -> confirmDelete()
            is WorkoutListIntent.CancelDelete -> setState { copy(pendingDeleteId = null) }
            is WorkoutListIntent.CreateWorkout -> emitEffect(WorkoutListEffect.NavigateToCreateWorkout)
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
