package com.workout.shared.backup

import com.workout.core.model.Block
import com.workout.core.model.BLOCK_TYPE_EXERCISE
import com.workout.core.model.BLOCK_TYPE_REST
import com.workout.core.model.Workout
import com.workout.core.repository.WorkoutRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class WorkoutBackupManager(private val repository: WorkoutRepository) {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun exportToJson(): String {
        val workouts = repository.getWorkouts().first()
        val dto = WorkoutBackupDto(workouts = workouts.map { it.toDto() })
        return json.encodeToString(dto)
    }

    suspend fun importFromJson(jsonString: String): Int {
        val backup = json.decodeFromString<WorkoutBackupDto>(jsonString)
        backup.workouts.forEach { dto -> repository.saveWorkout(dto.toWorkout()) }
        return backup.workouts.size
    }

    private fun Workout.toDto() = WorkoutDto(
        name = name,
        blocks = blocks.map { it.toDto() }
    )

    private fun Block.toDto(): BlockDto = when (this) {
        is Block.Exercise -> BlockDto(
            type = BLOCK_TYPE_EXERCISE,
            orderIndex = orderIndex,
            workDurationSeconds = workDurationSeconds,
            restDurationSeconds = restDurationSeconds,
            repeats = repeats,
            name = name
        )
        is Block.Rest -> BlockDto(
            type = BLOCK_TYPE_REST,
            orderIndex = orderIndex,
            durationSeconds = durationSeconds
        )
    }

    private fun WorkoutDto.toWorkout() = Workout(
        id = 0L,
        name = name,
        createdAt = 0L,
        blocks = blocks.mapIndexed { idx, dto -> dto.toBlock(idx) }
    )

    private fun BlockDto.toBlock(fallbackIndex: Int): Block = when (type) {
        BLOCK_TYPE_EXERCISE -> Block.Exercise(
            id = 0L,
            orderIndex = orderIndex.takeIf { it >= 0 } ?: fallbackIndex,
            name = name,
            workDurationSeconds = workDurationSeconds,
            restDurationSeconds = restDurationSeconds,
            repeats = repeats
        )
        else -> Block.Rest(
            id = 0L,
            orderIndex = orderIndex.takeIf { it >= 0 } ?: fallbackIndex,
            durationSeconds = durationSeconds
        )
    }
}

fun Workout.formatForSharing(restLabel: String): String {
    val totalMin = totalDurationSeconds / 60
    val totalSec = totalDurationSeconds % 60
    val duration = when {
        totalMin > 0 && totalSec > 0 -> "$totalMin min $totalSec sec"
        totalMin > 0 -> "$totalMin min"
        else -> "$totalSec sec"
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
                        val exerciseName = block.name.ifEmpty { "Exercise" }
                        append("$exerciseName — ${block.repeats}× ${block.workDurationSeconds}s")
                        if (block.restDurationSeconds > 0) append(" / ${block.restDurationSeconds}s")
                    }
                    is Block.Rest -> append("$restLabel — ${block.durationSeconds}s")
                }
                appendLine()
            }
        }
    }.trimEnd()
}
