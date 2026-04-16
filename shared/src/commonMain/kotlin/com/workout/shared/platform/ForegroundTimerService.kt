package com.workout.shared.platform

import com.workout.shared.feature.timer.TimerIntent
import com.workout.shared.feature.timer.TimerState

interface ForegroundTimerService {
    /** Запустить фоновой сервис. [onDispatch] — callback для интентов из внешних контролов. */
    fun start(workoutName: String, onDispatch: (TimerIntent) -> Unit)
    /** Обновить информацию (уведомление / Now Playing). */
    fun update(state: TimerState, workoutName: String)
    /** Остановить фоновой сервис. */
    fun stop()
}
