package com.workout.shared.feature.createworkout

import com.workout.core.model.Block

data class CreateWorkoutState(
    val workoutId: Long = 0L,
    val name: String = "",
    val blocks: List<Block> = emptyList(),
    val isSaving: Boolean = false,
    val totalDurationSeconds: Int = 0
)

sealed interface CreateWorkoutIntent {
    data class LoadWorkout(val workoutId: Long) : CreateWorkoutIntent
    data class UpdateName(val name: String) : CreateWorkoutIntent
    data class AddExerciseBlock(val afterIndex: Int? = null) : CreateWorkoutIntent
    data class AddRestBlock(val afterIndex: Int? = null) : CreateWorkoutIntent
    data class UpdateBlock(val index: Int, val block: Block) : CreateWorkoutIntent
    data class RemoveBlock(val index: Int) : CreateWorkoutIntent
    data class MoveBlock(val fromIndex: Int, val toIndex: Int) : CreateWorkoutIntent
    data class DuplicateBlock(val index: Int) : CreateWorkoutIntent
    data object Save : CreateWorkoutIntent
    data object Discard : CreateWorkoutIntent
}

sealed interface CreateWorkoutEffect {
    data object NavigateBack : CreateWorkoutEffect
    data class ShowError(val message: String) : CreateWorkoutEffect
}
