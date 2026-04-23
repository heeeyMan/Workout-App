package com.workout.shared.feature.createworkout

import com.workout.core.model.Block

data class CreateWorkoutState(
    val workoutId: Long = 0L,
    val name: String = "",
    val blocks: List<Block> = emptyList(),
    val isSaving: Boolean = false,
    val createdAt: Long = 0L,
) {
    val totalDurationSeconds: Int get() = blocks.sumOf { it.totalDurationSeconds }
}

sealed interface CreateWorkoutIntent {
    data class LoadWorkout(val workoutId: Long) : CreateWorkoutIntent
    /** Задаёт имя для новой тренировки, если поле ещё пустое (локализованная строка с платформы). */
    data class SetDefaultWorkoutNameIfEmpty(val name: String) : CreateWorkoutIntent
    data class UpdateName(val name: String) : CreateWorkoutIntent
    data class AddExerciseBlock(val afterIndex: Int? = null, val defaultExerciseName: String) : CreateWorkoutIntent
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
    data object ShowErrorEmptyWorkoutName : CreateWorkoutEffect
    data object ShowSaveError : CreateWorkoutEffect
}
