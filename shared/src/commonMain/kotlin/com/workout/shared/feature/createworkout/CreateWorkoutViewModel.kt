package com.workout.shared.feature.createworkout

import androidx.lifecycle.ViewModel
import com.workout.core.repository.WorkoutRepository

class CreateWorkoutViewModel(repository: WorkoutRepository) : ViewModel() {
    val store = CreateWorkoutStore(repository)

    override fun onCleared() {
        store.destroy()
    }
}
