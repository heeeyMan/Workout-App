package com.workout.shared.backup

import com.workout.core.model.Block
import com.workout.core.model.Workout

fun Workout.formatForSharing(
    exerciseLabel: String,
    restLabel: String,
    minLabel: String,
    secLabel: String
): String {
    val totalMin = totalDurationSeconds / 60
    val totalSec = totalDurationSeconds % 60
    val duration = when {
        totalMin > 0 && totalSec > 0 -> "$totalMin $minLabel $totalSec $secLabel"
        totalMin > 0 -> "$totalMin $minLabel"
        else -> "$totalSec $secLabel"
    }
    return buildString {
        appendLine(name)
        appendLine(duration)
        if (blocks.isNotEmpty()) {
            appendLine()
            blocks.forEachIndexed { i, block ->
                append("${i + 1}. ")
                when (block) {
                    is Block.Exercise -> {
                        val exerciseName = block.name.ifEmpty { exerciseLabel }
                        append("$exerciseName — ${block.repeats}× ${block.workDurationSeconds}$secLabel")
                        if (block.restDurationSeconds > 0) append(" / ${block.restDurationSeconds}$secLabel")
                    }
                    is Block.Rest -> append("$restLabel — ${block.durationSeconds}$secLabel")
                }
                appendLine()
            }
        }
    }.trimEnd()
}
