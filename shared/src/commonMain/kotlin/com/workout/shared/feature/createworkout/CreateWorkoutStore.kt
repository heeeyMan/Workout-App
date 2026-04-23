package com.workout.shared.feature.createworkout

import com.workout.core.model.Block
import com.workout.core.repository.WorkoutRepository
import com.workout.core.model.Workout
import com.workout.shared.mvi.BaseStore
import kotlinx.coroutines.launch

class CreateWorkoutStore(
    private val workoutRepository: WorkoutRepository
) : BaseStore<CreateWorkoutState, CreateWorkoutIntent, CreateWorkoutEffect>(CreateWorkoutState()) {

    override fun dispatch(intent: CreateWorkoutIntent) {
        when (intent) {
            is CreateWorkoutIntent.LoadWorkout -> loadWorkout(intent.workoutId)
            is CreateWorkoutIntent.SetDefaultWorkoutNameIfEmpty -> {
                if (state.value.name.isBlank()) {
                    setState { copy(name = intent.name) }
                }
            }
            is CreateWorkoutIntent.UpdateName -> setState { copy(name = intent.name) }
            is CreateWorkoutIntent.AddExerciseBlock -> addExerciseBlock(intent.afterIndex, intent.defaultExerciseName)
            is CreateWorkoutIntent.AddRestBlock -> addRestBlock(intent.afterIndex)
            is CreateWorkoutIntent.UpdateBlock -> updateBlock(intent.index, intent.block)
            is CreateWorkoutIntent.RemoveBlock -> removeBlock(intent.index)
            is CreateWorkoutIntent.MoveBlock -> moveBlock(intent.fromIndex, intent.toIndex)
            is CreateWorkoutIntent.DuplicateBlock -> duplicateBlock(intent.index)
            is CreateWorkoutIntent.Save -> save()
            is CreateWorkoutIntent.Discard -> emitEffect(CreateWorkoutEffect.NavigateBack)
        }
    }

    private fun loadWorkout(id: Long) {
        scope.launch {
            val workout = workoutRepository.getWorkoutById(id) ?: return@launch
            setState {
                copy(
                    workoutId = workout.id,
                    name = workout.name,
                    blocks = workout.blocks,
                    createdAt = workout.createdAt,
                )
            }
        }
    }

    private fun addExerciseBlock(afterIndex: Int?, defaultExerciseName: String) {
        val newBlock = Block.Exercise(
            orderIndex = 0,
            name = defaultExerciseName,
            workDurationSeconds = DEFAULT_WORK_SECONDS,
            restDurationSeconds = DEFAULT_REST_SECONDS,
            repeats = DEFAULT_REPEATS
        )
        insertBlock(newBlock, afterIndex)
    }

    private fun addRestBlock(afterIndex: Int?) {
        val newBlock = Block.Rest(orderIndex = 0, durationSeconds = DEFAULT_REST_BLOCK_SECONDS)
        insertBlock(newBlock, afterIndex)
    }

    private fun insertBlock(block: Block, afterIndex: Int?) {
        setState {
            val mutable = blocks.toMutableList()
            val insertAt = (if (afterIndex != null) afterIndex + 1 else mutable.size)
                .coerceIn(0, mutable.size)
            mutable.add(insertAt, block)
            copy(blocks = mutable.reindexed())
        }
    }

    private fun updateBlock(index: Int, block: Block) {
        setState {
            val mutable = blocks.toMutableList()
            mutable[index] = block
            copy(blocks = mutable.reindexed())
        }
    }

    private fun removeBlock(index: Int) {
        setState {
            val mutable = blocks.toMutableList()
            mutable.removeAt(index)
            copy(blocks = mutable.reindexed())
        }
    }

    private fun moveBlock(fromIndex: Int, toIndex: Int) {
        setState {
            val mutable = blocks.toMutableList()
            val block = mutable.removeAt(fromIndex)
            mutable.add(toIndex, block)
            copy(blocks = mutable.reindexed())
        }
    }

    private fun duplicateBlock(index: Int) {
        setState {
            val mutable = blocks.toMutableList()
            val duplicated = when (val block = mutable[index]) {
                is Block.Exercise -> block.copy(id = 0L)
                is Block.Rest -> block.copy(id = 0L)
            }
            mutable.add(index + 1, duplicated)
            copy(blocks = mutable.reindexed())
        }
    }

    private fun save() {
        val current = state.value
        if (current.isSaving) return
        if (current.name.isBlank()) {
            emitEffect(CreateWorkoutEffect.ShowErrorEmptyWorkoutName)
            return
        }
        setState { copy(isSaving = true) }
        scope.launch {
            try {
                val workout = Workout(
                    id = current.workoutId,
                    name = current.name.trim(),
                    createdAt = current.createdAt,
                    blocks = current.blocks
                )
                workoutRepository.saveWorkout(workout)
                emitEffect(CreateWorkoutEffect.NavigateBack)
            } catch (_: Exception) {
                setState { copy(isSaving = false) }
                emitEffect(CreateWorkoutEffect.ShowSaveError)
            }
        }
    }

    // Re-assign orderIndex to match list positions
    private fun List<Block>.reindexed(): List<Block> = mapIndexed { index, block ->
        when (block) {
            is Block.Exercise -> block.copy(orderIndex = index)
            is Block.Rest -> block.copy(orderIndex = index)
        }
    }

    companion object {
        private const val DEFAULT_WORK_SECONDS = 40
        private const val DEFAULT_REST_SECONDS = 20
        private const val DEFAULT_REPEATS = 3
        private const val DEFAULT_REST_BLOCK_SECONDS = 60
    }
}
