package com.workout.android.timer

import android.content.Context
import com.workout.android.R
import com.workout.shared.feature.timer.PhaseType
import com.workout.shared.feature.timer.TimerState

internal fun refreshWorkoutTimerNotification(context: Context, state: TimerState) {
    val app = context.applicationContext
    when {
        state.isFinished || state.isLoading -> WorkoutTimerForegroundService.stop(app)
        else -> {
            val lines = state.toForegroundNotificationLines(context) ?: return
            WorkoutTimerForegroundService.update(
                app,
                lines.workoutName,
                lines.phaseLine,
                lines.detailLine,
                lines.timeLine,
                state.isPaused
            )
        }
    }
}

internal data class TimerForegroundNotificationLines(
    val workoutName: String,
    val phaseLine: String,
    val detailLine: String,
    val timeLine: String
)

internal fun TimerState.toForegroundNotificationLines(context: Context): TimerForegroundNotificationLines? {
    if (isLoading || isFinished) return null
    val phaseLine = when {
        isPrepBeforeWork -> context.getString(R.string.notif_phase_prep)
        currentPhase?.type == PhaseType.Work -> {
            val n = currentPhase?.name.orEmpty()
            if (n.isNotBlank()) {
                context.getString(R.string.notif_phase_work_with_name, n)
            } else {
                context.getString(R.string.notif_phase_work)
            }
        }
        else -> context.getString(R.string.notif_phase_rest)
    }
    val detailLine = currentPhase?.repeatLabel?.let {
        context.getString(R.string.notif_repeat_format, it)
    }.orEmpty()
    val timeLine = buildString {
        append(formatMmSs(secondsRemaining))
        if (isPaused) {
            append(" · ")
            append(context.getString(R.string.notif_paused))
        }
    }
    return TimerForegroundNotificationLines(workoutName, phaseLine, detailLine, timeLine)
}

private fun formatMmSs(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%02d:%02d".format(m, s)
}
