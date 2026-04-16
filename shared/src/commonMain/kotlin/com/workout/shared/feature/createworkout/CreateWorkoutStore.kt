package com.workout.shared.feature.createworkout

import com.workout.core.model.Block
import com.workout.core.repository.WorkoutRepository
import com.workout.core.model.Workout
import com.workout.shared.mvi.BaseStore
import kotlinx.coroutines.launch
import kotlin.time.Clock

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
                    totalDurationSeconds = workout.totalDurationSeconds
                )
            }
        }
    }

    private fun addExerciseBlock(afterIndex: Int?, defaultExerciseName: String) {
        val newBlock = Block.Exercise(
            orderIndex = 0,
            name = defaultExerciseName,
            workDurationSeconds = 40,
            restDurationSeconds = 20,
            repeats = 3
        )
        insertBlock(newBlock, afterIndex)
    }

    private fun addRestBlock(afterIndex: Int?) {
        val newBlock = Block.Rest(orderIndex = 0, durationSeconds = 60)
        insertBlock(newBlock, afterIndex)
    }

    private fun insertBlock(block: Block, afterIndex: Int?) {
        setState {
            val mutable = blocks.toMutableList()
            val insertAt = if (afterIndex != null) afterIndex + 1 else mutable.size
            mutable.add(insertAt, block)
            copy(blocks = mutable.reindexed(), totalDurationSeconds = mutable.sumOf { it.totalDurationSeconds })
        }
    }

    private fun updateBlock(index: Int, block: Block) {
        setState {
            val mutable = blocks.toMutableList()
            mutable[index] = block
            copy(blocks = mutable.reindexed(), totalDurationSeconds = mutable.sumOf { it.totalDurationSeconds })
        }
    }

    private fun removeBlock(index: Int) {
        setState {
            val mutable = blocks.toMutableList()
            mutable.removeAt(index)
            copy(blocks = mutable.reindexed(), totalDurationSeconds = mutable.sumOf { it.totalDurationSeconds })
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
            val copy = when (val block = mutable[index]) {
                is Block.Exercise -> block.copy(id = 0L)
                is Block.Rest -> block.copy(id = 0L)
            }
            mutable.add(index + 1, copy)
            copy(blocks = mutable.reindexed(), totalDurationSeconds = mutable.sumOf { it.totalDurationSeconds })
        }
    }

    private fun save() {
        val current = state.value
        if (current.name.isBlank()) {
            emitEffect(CreateWorkoutEffect.ShowErrorEmptyWorkoutName)
            return
        }
        scope.launch {
            setState { copy(isSaving = true) }
            val workout = Workout(
                id = current.workoutId,
                name = current.name.trim(),
                createdAt = Clock.System.now().toEpochMilliseconds(),
                blocks = current.blocks
            )
            workoutRepository.saveWorkout(workout)
            emitEffect(CreateWorkoutEffect.NavigateBack)
        }
    }

    // Re-assign orderIndex to match list positions
    private fun List<Block>.reindexed(): List<Block> = mapIndexed { index, block ->
        when (block) {
            is Block.Exercise -> block.copy(orderIndex = index)
            is Block.Rest -> block.copy(orderIndex = index)
        }
    }
}
