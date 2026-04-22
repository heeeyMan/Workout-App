package com.workout.shared.platform

import androidx.compose.runtime.Composable

@Composable
actual fun rememberJsonImporter(onResult: (content: String?) -> Unit): () -> Unit = { }
