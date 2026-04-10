package com.workout.core.model

data class Workout(
    val id: Long = 0L,
    val name: String,
    val createdAt: Long,
    val lastStartedAt: Long? = null,
    val blocks: List<Block>
) {
    val totalDurationSeconds: Int
        get() = blocks.sumOf { it.totalDurationSeconds }
}
