package com.workout.shared.backup

import kotlinx.serialization.Serializable

@Serializable
data class WorkoutBackupDto(
    val version: Int = 1,
    val workouts: List<WorkoutDto>
)

@Serializable
data class WorkoutDto(
    val name: String,
    val createdAt: Long = 0L,
    val blocks: List<BlockDto>
)

@Serializable
data class BlockDto(
    val type: String,
    val orderIndex: Int = 0,
    val workDurationSeconds: Int = 0,
    val restDurationSeconds: Int = 0,
    val repeats: Int = 1,
    val name: String = "",
    val durationSeconds: Int = 0
)
