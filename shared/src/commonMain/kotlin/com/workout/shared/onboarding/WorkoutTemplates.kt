package com.workout.shared.onboarding

import com.workout.core.model.Block
import org.jetbrains.compose.resources.StringResource
import workoutapp.shared.generated.resources.Res
import workoutapp.shared.generated.resources.template_hiit_desc
import workoutapp.shared.generated.resources.template_hiit_name
import workoutapp.shared.generated.resources.template_strength_desc
import workoutapp.shared.generated.resources.template_strength_name
import workoutapp.shared.generated.resources.template_tabata_desc
import workoutapp.shared.generated.resources.template_tabata_name
import workoutapp.shared.generated.resources.template_warmup_desc
import workoutapp.shared.generated.resources.template_warmup_name

data class WorkoutTemplate(
    val nameRes: StringResource,
    val descriptionRes: StringResource,
    val makeBlocks: (exerciseLabel: String) -> List<Block>
) {
    val totalDurationSeconds: Int get() = makeBlocks("").sumOf { it.totalDurationSeconds }
}

object WorkoutTemplates {
    val all: List<WorkoutTemplate> = listOf(
        WorkoutTemplate(
            nameRes = Res.string.template_tabata_name,
            descriptionRes = Res.string.template_tabata_desc
        ) { label ->
            listOf(
                Block.Exercise(orderIndex = 0, name = label, workDurationSeconds = 20, restDurationSeconds = 10, repeats = 8)
            )
        },
        WorkoutTemplate(
            nameRes = Res.string.template_hiit_name,
            descriptionRes = Res.string.template_hiit_desc
        ) { label ->
            listOf(
                Block.Exercise(orderIndex = 0, name = "$label 1", workDurationSeconds = 40, restDurationSeconds = 20, repeats = 3),
                Block.Rest(orderIndex = 1, durationSeconds = 60),
                Block.Exercise(orderIndex = 2, name = "$label 2", workDurationSeconds = 40, restDurationSeconds = 20, repeats = 3),
                Block.Rest(orderIndex = 3, durationSeconds = 60),
                Block.Exercise(orderIndex = 4, name = "$label 3", workDurationSeconds = 40, restDurationSeconds = 20, repeats = 3)
            )
        },
        WorkoutTemplate(
            nameRes = Res.string.template_strength_name,
            descriptionRes = Res.string.template_strength_desc
        ) { label ->
            (1..4).map { i ->
                Block.Exercise(orderIndex = i - 1, name = "$label $i", workDurationSeconds = 30, restDurationSeconds = 30, repeats = 5)
            }
        },
        WorkoutTemplate(
            nameRes = Res.string.template_warmup_name,
            descriptionRes = Res.string.template_warmup_desc
        ) { label ->
            (1..3).map { i ->
                Block.Exercise(orderIndex = i - 1, name = "$label $i", workDurationSeconds = 30, restDurationSeconds = 15, repeats = 2)
            }
        }
    )
}
