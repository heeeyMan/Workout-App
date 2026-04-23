package com.workout.shared.platform

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.workout.shared.feature.timer.PhaseType
import com.workout.shared.feature.timer.TimerIntent
import com.workout.shared.feature.timer.TimerState

class AndroidForegroundTimerService(private val context: Context) : ForegroundTimerService {

    companion object {
        @Volatile
        var dispatchCallback: ((TimerIntent) -> Unit)? = null

        const val ACTION_RUN = "com.workout.android.timer.RUN"
        const val EXTRA_WORKOUT_NAME = "workout_name"
        const val EXTRA_PHASE_LINE = "phase_line"
        const val EXTRA_DETAIL_LINE = "detail_line"
        const val EXTRA_IS_PAUSED = "is_paused"
        const val EXTRA_PHASE_TYPE = "phase_type"

        private const val SERVICE_CLASS = "com.workout.android.timer.WorkoutTimerForegroundService"

        /** ID уведомления — совпадает с WorkoutTimerForegroundService.NOTIFICATION_ID */
        const val NOTIFICATION_ID = 71001
    }

    private var serviceStarted = false

    override fun start(workoutName: String, onDispatch: (TimerIntent) -> Unit) {
        dispatchCallback = onDispatch
        serviceStarted = false
        // Стартуем foreground service через intent (первый раз — startForeground)
        startService(workoutName.ifBlank { "Workout" }, "", "", false, "prep")
    }

    override fun update(state: TimerState, workoutName: String, displayStrings: NotifDisplayStrings) {
        if (state.isLoading) return
        if (state.isFinished) { stop(); return }

        val phaseLine = when {
            state.isPrepBeforeWork -> displayStrings.phasePrep
            state.currentPhase?.type == PhaseType.Work -> {
                val n = state.currentPhase?.name.orEmpty()
                if (n.isNotBlank()) "${displayStrings.phaseWork} · $n" else displayStrings.phaseWork
            }
            else -> displayStrings.phaseRest
        }
        val detailLine = state.currentPhase?.repeatLabel
            ?.let { displayStrings.setFormat.format(it) }
            .orEmpty()
        val phaseType = when {
            state.isPrepBeforeWork -> "prep"
            state.currentPhase?.type == PhaseType.Work -> "work"
            else -> "rest"
        }
        val timeLine = buildString {
            append(formatMmSs(state.secondsRemaining))
            if (state.isPaused) append(" · ${displayStrings.paused}")
        }

        if (!serviceStarted) {
            // Сервис ещё не запущен — отправляем через startForegroundService
            startService(workoutName, phaseLine, detailLine, state.isPaused, phaseType)
        }

        // Обновляем уведомление напрямую через NotificationManager (без startForegroundService)
        // Это не пересоздаёт сервис и не вызывает мигание
        updateNotificationDirectly(workoutName, phaseLine, detailLine, timeLine, state.isPaused, phaseType)
    }

    override fun stop() {
        dispatchCallback = null
        serviceStarted = false
        val app = context.applicationContext
        try {
            val cls = Class.forName(SERVICE_CLASS)
            app.stopService(Intent(app, cls))
        } catch (_: Exception) { }
    }

    private fun startService(
        workoutName: String,
        phaseLine: String,
        detailLine: String,
        isPaused: Boolean,
        phaseType: String
    ) {
        val app = context.applicationContext
        try {
            val cls = Class.forName(SERVICE_CLASS)
            val intent = Intent(app, cls).apply {
                action = ACTION_RUN
                putExtra(EXTRA_WORKOUT_NAME, workoutName)
                putExtra(EXTRA_PHASE_LINE, phaseLine)
                putExtra(EXTRA_DETAIL_LINE, detailLine)
                putExtra(EXTRA_IS_PAUSED, isPaused)
                putExtra(EXTRA_PHASE_TYPE, phaseType)
            }
            ContextCompat.startForegroundService(app, intent)
            serviceStarted = true
        } catch (_: Exception) { }
    }

    /**
     * Обновить уведомление напрямую через NotificationManager.notify() —
     * без повторного startForegroundService. Сервис не пересоздаётся,
     * уведомление обновляется на месте без мигания.
     */
    private fun updateNotificationDirectly(
        workoutName: String,
        phaseLine: String,
        detailLine: String,
        timeLine: String,
        isPaused: Boolean,
        phaseType: String
    ) {
        try {
            val cls = Class.forName(SERVICE_CLASS)
            val method = cls.getMethod(
                "buildNotificationStatic",
                Context::class.java,
                String::class.java, String::class.java, String::class.java,
                String::class.java, Boolean::class.javaPrimitiveType, String::class.java
            )
            val notification = method.invoke(
                null, context, workoutName, phaseLine, detailLine, timeLine, isPaused, phaseType
            ) as android.app.Notification

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.notify(NOTIFICATION_ID, notification)
        } catch (_: Exception) { }
    }
}

private fun formatMmSs(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%02d:%02d".format(m, s)
}
