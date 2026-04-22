package com.workout.shared.feature.timer

import androidx.lifecycle.ViewModel
import com.workout.core.repository.WorkoutRepository

class TimerViewModel(repository: WorkoutRepository) : ViewModel() {
    val store = TimerStore(repository)

    override fun onCleared() {
        store.destroy()
    }
}
