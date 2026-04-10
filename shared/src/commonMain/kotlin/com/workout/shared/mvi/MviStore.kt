package com.workout.shared.mvi

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface MviStore<State, Intent, Effect> {
    val state: StateFlow<State>
    val effects: SharedFlow<Effect>
    fun dispatch(intent: Intent)
    fun destroy()
}
