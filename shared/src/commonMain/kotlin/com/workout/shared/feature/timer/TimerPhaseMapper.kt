package com.workout.shared.feature.timer

import com.workout.core.model.Block

/**
 * Разворачивает блок тренировки в плоский список [TimerPhase].
 *
 * Для [Block.Exercise]: если [prepDurationSeconds] > 0, перед первым подходом
 * вставляется фаза [PhaseType.Prep]. Между повторами добавляется фаза [PhaseType.Rest],
 * если `restDurationSeconds > 0` и повтор не последний.
 *
 * Для [Block.Rest]: одна фаза [PhaseType.Rest].
 */
internal fun Block.toPhases(restPhaseDisplayName: String, prepDurationSeconds: Int): List<TimerPhase> =
    when (this) {
        is Block.Exercise -> buildList {
            if (prepDurationSeconds > 0) {
                add(TimerPhase(name = name, type = PhaseType.Prep, durationSeconds = prepDurationSeconds))
            }
            for (rep in 1..repeats) {
                add(
                    TimerPhase(
                        name = name,
                        type = PhaseType.Work,
                        durationSeconds = workDurationSeconds,
                        repeatLabel = "$rep / $repeats",
                    )
                )
                if (restDurationSeconds > 0 && rep < repeats) {
                    add(
                        TimerPhase(
                            name = restPhaseDisplayName,
                            type = PhaseType.Rest,
                            durationSeconds = restDurationSeconds,
                            repeatLabel = "$rep / $repeats",
                        )
                    )
                }
            }
        }
        is Block.Rest -> listOf(
            TimerPhase(name = restPhaseDisplayName, type = PhaseType.Rest, durationSeconds = durationSeconds)
        )
    }
