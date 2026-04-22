package com.workout.shared.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object SettingsRoute

@Serializable
data class CreateWorkoutRoute(val id: Long = 0L)

@Serializable
data class TimerRoute(val workoutId: Long)
