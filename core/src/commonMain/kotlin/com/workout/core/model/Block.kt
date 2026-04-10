package com.workout.core.model

sealed class Block {
    abstract val id: Long
    abstract val orderIndex: Int
    abstract val totalDurationSeconds: Int

    data class Exercise(
        override val id: Long = 0L,
        override val orderIndex: Int,
        val name: String,
        val workDurationSeconds: Int,
        val restDurationSeconds: Int,
        val repeats: Int,
        val videoPath: String? = null
    ) : Block() {
        override val totalDurationSeconds: Int
            get() = (workDurationSeconds + restDurationSeconds) * repeats
    }

    data class Rest(
        override val id: Long = 0L,
        override val orderIndex: Int,
        val durationSeconds: Int
    ) : Block() {
        override val totalDurationSeconds: Int
            get() = durationSeconds
    }
}

const val BLOCK_TYPE_EXERCISE = "exercise"
const val BLOCK_TYPE_REST = "rest"
