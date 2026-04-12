package com.workout.shared.ios

import com.workout.core.database.DatabaseDriverFactory
import com.workout.core.model.Block
import com.workout.core.model.BLOCK_TYPE_EXERCISE
import com.workout.core.model.BLOCK_TYPE_REST
import com.workout.core.database.WorkoutDatabase
import com.workout.core.repository.WorkoutRepositoryImpl
import com.workout.shared.feature.createworkout.CreateWorkoutEffect
import com.workout.shared.feature.createworkout.CreateWorkoutIntent
import com.workout.shared.feature.createworkout.CreateWorkoutStore
import com.workout.shared.feature.home.HomeEffect
import com.workout.shared.feature.home.HomeStore
import com.workout.shared.feature.workoutlist.WorkoutListEffect
import com.workout.shared.feature.workoutlist.WorkoutListStore
import com.workout.shared.feature.timer.TimerEffect
import com.workout.shared.feature.timer.TimerStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/** Точка входа iOS (SwiftUI): общий репозиторий и сторы, как на Android. */
class WorkoutIosAppController {

    private val driverFactory = DatabaseDriverFactory()
    private val database = WorkoutDatabase(driverFactory.createDriver())
    private val repository = WorkoutRepositoryImpl(database)

    val homeStore = HomeStore(repository)

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun createCreateWorkoutStore(): CreateWorkoutStore = CreateWorkoutStore(repository)

    fun createTimerStore(): TimerStore = TimerStore(repository)

    fun createWorkoutListStore(): WorkoutListStore = WorkoutListStore(repository)

    fun observeWorkoutListEffects(
        store: WorkoutListStore,
        onEffect: (WorkoutListEffect) -> Unit,
    ): () -> Unit {
        val job = appScope.launch {
            store.effects.collect { onEffect(it) }
        }
        return { job.cancel() }
    }

    fun observeTimerEffects(
        store: TimerStore,
        onEffect: (TimerEffect) -> Unit,
    ): () -> Unit {
        val job = appScope.launch {
            store.effects.collect { onEffect(it) }
        }
        return { job.cancel() }
    }

    /**
     * Подписка на эффекты главного экрана (например [HomeEffect.NavigateToTimer] после старта).
     * Возвращает функцию отмены коллектора.
     */
    fun observeHomeEffects(onEffect: (HomeEffect) -> Unit): () -> Unit {
        val job = appScope.launch {
            homeStore.effects.collect { onEffect(it) }
        }
        return { job.cancel() }
    }

    fun observeCreateWorkoutEffects(
        store: CreateWorkoutStore,
        onEffect: (CreateWorkoutEffect) -> Unit
    ): () -> Unit {
        val job = appScope.launch {
            store.effects.collect { onEffect(it) }
        }
        return { job.cancel() }
    }

    fun dispose() {
        homeStore.destroy()
        appScope.cancel()
    }

    /** Строка для списка блоков в SwiftUI. */
    fun blockSummaryLine(block: Block): String = when (block) {
        is Block.Exercise ->
            "${block.name} · work ${block.workDurationSeconds}s / rest ${block.restDurationSeconds}s × ${block.repeats}"
        is Block.Rest -> "Rest · ${block.durationSeconds}s"
    }

    fun blockKind(block: Block): String = when (block) {
        is Block.Exercise -> BLOCK_TYPE_EXERCISE
        is Block.Rest -> BLOCK_TYPE_REST
    }

    fun exerciseBlockFields(block: Block): IosExerciseBlockFields? =
        (block as? Block.Exercise)?.let {
            IosExerciseBlockFields(
                id = it.id,
                orderIndex = it.orderIndex,
                name = it.name,
                workDurationSeconds = it.workDurationSeconds,
                restDurationSeconds = it.restDurationSeconds,
                repeats = it.repeats,
            )
        }

    fun restBlockFields(block: Block): IosRestBlockFields? =
        (block as? Block.Rest)?.let {
            IosRestBlockFields(
                id = it.id,
                orderIndex = it.orderIndex,
                durationSeconds = it.durationSeconds,
            )
        }

    fun dispatchUpdateExerciseBlock(
        store: CreateWorkoutStore,
        index: Int,
        id: Long,
        orderIndex: Int,
        name: String,
        workDurationSeconds: Int,
        restDurationSeconds: Int,
        repeats: Int,
    ) {
        store.dispatch(
            CreateWorkoutIntent.UpdateBlock(
                index = index,
                block = Block.Exercise(
                    id = id,
                    orderIndex = orderIndex,
                    name = name.trim(),
                    workDurationSeconds = workDurationSeconds,
                    restDurationSeconds = restDurationSeconds,
                    repeats = repeats,
                ),
            ),
        )
    }

    fun dispatchUpdateRestBlock(
        store: CreateWorkoutStore,
        index: Int,
        id: Long,
        orderIndex: Int,
        durationSeconds: Int,
    ) {
        store.dispatch(
            CreateWorkoutIntent.UpdateBlock(
                index = index,
                block = Block.Rest(
                    id = id,
                    orderIndex = orderIndex,
                    durationSeconds = durationSeconds,
                ),
            ),
        )
    }
}
