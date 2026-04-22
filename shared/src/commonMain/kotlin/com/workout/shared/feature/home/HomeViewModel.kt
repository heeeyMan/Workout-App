package com.workout.shared.feature.home

import androidx.lifecycle.ViewModel
import com.workout.core.repository.WorkoutRepository

class HomeViewModel(repository: WorkoutRepository) : ViewModel() {
    val store = HomeStore(repository)

    override fun onCleared() {
        store.destroy()
    }
}
