package com.workout.android.ui.workoutlist

import androidx.lifecycle.ViewModel
import com.workout.shared.feature.workoutlist.WorkoutListIntent
import com.workout.shared.feature.workoutlist.WorkoutListStore

class WorkoutListViewModel(private val store: WorkoutListStore) : ViewModel() {
    val state = store.state
    val effects = store.effects
    fun dispatch(intent: WorkoutListIntent) = store.dispatch(intent)
    override fun onCleared() = store.destroy()
}
