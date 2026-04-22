package com.workout.shared.platform

import androidx.compose.runtime.Composable

@Composable
expect fun rememberJsonExporter(onComplete: (success: Boolean) -> Unit): (suggestedName: String, content: String) -> Unit
