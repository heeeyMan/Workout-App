package com.workout.shared.ios

/** Поля упражнения для SwiftUI (у [com.workout.core.model.Block] в K/N нет полей в заголовке). */
data class IosExerciseBlockFields(
    val id: Long,
    val orderIndex: Int,
    val name: String,
    val workDurationSeconds: Int,
    val restDurationSeconds: Int,
    val repeats: Int,
)

/** Поля отдыха для SwiftUI. */
data class IosRestBlockFields(
    val id: Long,
    val orderIndex: Int,
    val durationSeconds: Int,
)
