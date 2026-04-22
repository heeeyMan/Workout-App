package com.workout.shared.platform

import androidx.compose.runtime.Composable

@Composable
expect fun rememberJsonImporter(onResult: (content: String?) -> Unit): () -> Unit
