package com.workout.android.ui.home

import androidx.lifecycle.ViewModel
import com.workout.shared.feature.home.HomeStore

class HomeViewModel(private val store: HomeStore) : ViewModel() {
    val state = store.state
    val effects = store.effects
    fun dispatch(intent: com.workout.shared.feature.home.HomeIntent) = store.dispatch(intent)
    override fun onCleared() = store.destroy()
}
