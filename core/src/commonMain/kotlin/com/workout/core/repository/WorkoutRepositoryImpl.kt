package com.workout.core.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.workout.core.database.BlockEntity
import com.workout.core.database.WorkoutDatabase
import com.workout.core.model.Block
import com.workout.core.model.BLOCK_TYPE_EXERCISE
import com.workout.core.model.BLOCK_TYPE_REST
import com.workout.core.model.Workout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class WorkoutRepositoryImpl(
    private val database: WorkoutDatabase
) : WorkoutRepository {

    override fun getWorkouts(): Flow<List<Workout>> =
        database.workoutEntityQueries
            .selectAllWorkouts()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { entities ->
                entities.map { entity ->
                    val blockEntities = database.blockEntityQueries
                        .selectBlocksByWorkoutId(entity.id)
                        .executeAsList()
                    entity.toDomain(blockEntities)
                }
            }

    override suspend fun getWorkoutById(id: Long): Workout? {
        val entity = database.workoutEntityQueries
            .selectWorkoutById(id)
            .executeAsOneOrNull() ?: return null
        val blocks = database.blockEntityQueries
            .selectBlocksByWorkoutId(id)
            .executeAsList()
        return entity.toDomain(blocks)
    }

    override suspend fun saveWorkout(workout: Workout): Long {
        var insertedId = workout.id
        database.transaction {
            if (workout.id == 0L) {
                database.workoutEntityQueries.insertWorkout(
                    name = workout.name,
                    created_at = Clock.System.now().toEpochMilliseconds()
                )
                insertedId = database.workoutEntityQueries.lastInsertRowId().executeAsOne()
            } else {
                database.blockEntityQueries.deleteBlocksByWorkoutId(workout.id)
            }
            workout.blocks.forEachIndexed { index, block ->
                database.blockEntityQueries.insertBlock(
                    workout_id = insertedId,
                    type = if (block is Block.Exercise) BLOCK_TYPE_EXERCISE else BLOCK_TYPE_REST,
                    order_index = index.toLong(),
                    name = (block as? Block.Exercise)?.name,
                    work_duration_seconds = (block as? Block.Exercise)?.workDurationSeconds?.toLong(),
                    rest_duration_seconds = when (block) {
                        is Block.Exercise -> block.restDurationSeconds.toLong()
                        is Block.Rest -> block.durationSeconds.toLong()
                    },
                    repeats = (block as? Block.Exercise)?.repeats?.toLong(),
                    video_path = (block as? Block.Exercise)?.videoPath
                )
            }
        }
        return insertedId
    }

    override suspend fun deleteWorkout(id: Long) {
        database.transaction {
            database.blockEntityQueries.deleteBlocksByWorkoutId(id)
            database.workoutEntityQueries.deleteWorkout(id)
        }
    }

    // --- Mappers ---

    private fun com.workout.core.database.WorkoutEntity.toDomain(
        blockEntities: List<BlockEntity>
    ): Workout = Workout(
        id = id,
        name = name,
        createdAt = created_at,
        blocks = blockEntities.mapIndexed { index, entity -> entity.toDomain(index) }
    )

    private fun BlockEntity.toDomain(index: Int): Block = when (type) {
        BLOCK_TYPE_EXERCISE -> Block.Exercise(
            id = id,
            orderIndex = order_index.toInt(),
            name = name ?: "Упражнение ${index + 1}",
            workDurationSeconds = work_duration_seconds?.toInt() ?: 40,
            restDurationSeconds = rest_duration_seconds?.toInt() ?: 20,
            repeats = repeats?.toInt() ?: 1,
            videoPath = video_path
        )
        else -> Block.Rest(
            id = id,
            orderIndex = order_index.toInt(),
            durationSeconds = rest_duration_seconds?.toInt() ?: 60
        )
    }
}
