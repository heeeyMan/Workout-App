package com.workout.android.ui.createworkout

import androidx.lifecycle.ViewModel
import com.workout.shared.feature.createworkout.CreateWorkoutIntent
import com.workout.shared.feature.createworkout.CreateWorkoutStore

class CreateWorkoutViewModel(private val store: CreateWorkoutStore) : ViewModel() {
    val state = store.state
    val effects = store.effects
    fun dispatch(intent: CreateWorkoutIntent) = store.dispatch(intent)
    override fun onCleared() = store.destroy()
}
