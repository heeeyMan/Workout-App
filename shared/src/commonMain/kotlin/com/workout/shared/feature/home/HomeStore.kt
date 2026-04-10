package com.workout.shared.feature.home

import com.workout.core.repository.WorkoutRepository
import com.workout.shared.mvi.BaseStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HomeStore(
    private val workoutRepository: WorkoutRepository
) : BaseStore<HomeState, HomeIntent, HomeEffect>(HomeState()) {

    init {
        dispatch(HomeIntent.LoadLastWorkout)
    }

    override fun dispatch(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.LoadLastWorkout -> loadLastWorkout()
            is HomeIntent.StartWorkout -> emitEffect(HomeEffect.NavigateToTimer(intent.workoutId))
            is HomeIntent.CreateWorkout -> emitEffect(HomeEffect.NavigateToCreateWorkout)
            is HomeIntent.OpenWorkoutList -> emitEffect(HomeEffect.NavigateToWorkoutList)
        }
    }

    private fun loadLastWorkout() {
        scope.launch {
            setState { copy(isLoading = true) }
            val workouts = workoutRepository.getWorkouts().first()
            setState { copy(lastWorkout = workouts.firstOrNull(), isLoading = false) }
        }
    }
}
