package com.workout.shared.backup

import com.workout.core.model.Block
import com.workout.core.model.BLOCK_TYPE_EXERCISE
import com.workout.core.model.BLOCK_TYPE_REST
import com.workout.core.model.Workout
import com.workout.core.repository.WorkoutRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

class WorkoutBackupManager(private val repository: WorkoutRepository) {

    suspend fun exportToJson(): String {
        val workouts = repository.getWorkouts().first()
        val dto = WorkoutBackupDto(workouts = workouts.map { it.toDto() })
        return json.encodeToString(dto)
    }

    suspend fun importFromJson(jsonString: String): Int {
        val backup = json.decodeFromString<WorkoutBackupDto>(jsonString)
        if (backup.version > SUPPORTED_VERSION) {
            error("Unsupported backup version: ${backup.version}. Max supported: $SUPPORTED_VERSION")
        }
        val workouts = backup.workouts.mapIndexed { _, dto -> dto.toWorkout() }
        repository.saveWorkouts(workouts)
        return workouts.size
    }

    private fun Workout.toDto() = WorkoutDto(
        name = name,
        createdAt = createdAt,
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
        createdAt = createdAt,
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
        BLOCK_TYPE_REST -> Block.Rest(
            id = 0L,
            orderIndex = orderIndex.takeIf { it >= 0 } ?: fallbackIndex,
            durationSeconds = durationSeconds
        )
        else -> error("Unknown block type: $type")
    }

    companion object {
        private const val SUPPORTED_VERSION = 1
        private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    }
}
