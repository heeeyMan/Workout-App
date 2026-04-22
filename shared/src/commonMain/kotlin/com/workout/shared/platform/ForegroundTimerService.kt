package com.workout.shared.platform

import com.workout.shared.feature.timer.TimerIntent
import com.workout.shared.feature.timer.TimerState

data class NotifDisplayStrings(
    val phasePrep: String = "Prep",
    val phaseWork: String = "Work",
    val phaseRest: String = "Rest",
    val paused: String = "Paused",
    val setFormat: String = "Set %s"
)

interface ForegroundTimerService {
    /** Запустить фоновой сервис. [onDispatch] — callback для интентов из внешних контролов. */
    fun start(workoutName: String, onDispatch: (TimerIntent) -> Unit)
    /** Обновить информацию (уведомление / Now Playing). */
    fun update(state: TimerState, workoutName: String, displayStrings: NotifDisplayStrings = NotifDisplayStrings())
    /** Остановить фоновой сервис. */
    fun stop()
}
