package com.workout.shared.platform

import androidx.compose.runtime.Composable

data class NotificationPermissionState(
    val isGranted: Boolean,
    val shouldOpenSettings: Boolean = false,
    val request: () -> Unit,
    val openSettings: () -> Unit = {}
)

@Composable
expect fun rememberNotificationPermissionState(): NotificationPermissionState
