package com.workout.shared.ui.components

fun Int.toTimeString(): String {
    val m = this / 60
    val s = this % 60
    return "${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
}
