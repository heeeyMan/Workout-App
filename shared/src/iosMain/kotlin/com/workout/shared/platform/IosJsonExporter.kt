package com.workout.shared.platform

import androidx.compose.runtime.Composable

@Composable
actual fun rememberJsonExporter(onComplete: (success: Boolean) -> Unit): (String, String) -> Unit = { _, _ -> }
