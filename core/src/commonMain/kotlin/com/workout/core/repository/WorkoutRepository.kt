package com.workout.core.repository

import com.workout.core.model.Workout
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    fun getWorkouts(): Flow<List<Workout>>
    suspend fun getWorkoutById(id: Long): Workout?
    suspend fun saveWorkout(workout: Workout): Long
    suspend fun saveWorkouts(workouts: List<Workout>)
    suspend fun deleteWorkout(id: Long)
    suspend fun markWorkoutStarted(id: Long)
}
