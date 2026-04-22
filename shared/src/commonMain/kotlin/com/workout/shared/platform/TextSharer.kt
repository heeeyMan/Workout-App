package com.workout.shared.platform

import androidx.compose.runtime.Composable

@Composable
expect fun rememberTextSharer(): (String) -> Unit
