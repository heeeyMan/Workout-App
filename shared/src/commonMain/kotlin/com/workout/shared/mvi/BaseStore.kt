package com.workout.shared.mvi

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class BaseStore<State, Intent, Effect>(
    initialState: State
) : MviStore<State, Intent, Effect> {

    protected val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<State> = _state.asStateFlow()

    /** Буфер побольше: подряд идущие эффекты таймера (например предупреждение каждую секунду) не должны теряться. */
    private val _effects = MutableSharedFlow<Effect>(extraBufferCapacity = 64)
    override val effects: SharedFlow<Effect> = _effects.asSharedFlow()

    protected fun setState(reducer: State.() -> State) {
        _state.update { it.reducer() }
    }

    protected fun emitEffect(effect: Effect) {
        if (!_effects.tryEmit(effect)) {
            scope.launch { _effects.emit(effect) }
        }
    }

    override fun destroy() {
        scope.cancel()
    }
}
