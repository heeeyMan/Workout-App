package com.workout.shared.platform

import androidx.compose.runtime.Composable

@Composable
actual fun rememberNotificationPermissionState(): NotificationPermissionState {
    return NotificationPermissionState(isGranted = true, request = {})
}
