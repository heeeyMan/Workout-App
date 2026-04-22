package com.workout.android.ui.permissions

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.workout.shared.platform.AndroidTimerSettings
import org.koin.android.ext.android.getKoin

@Composable
fun AppEntryPermissionHandler() {
    val context = LocalContext.current
    val activity = context as? ComponentActivity ?: return
    val prefs = activity.getKoin().get<AndroidTimerSettings>()

    val needRequest = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val permission = Manifest.permission.POST_NOTIFICATIONS

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) prefs.skipNotificationPermissionPrompt = true
    }

    LaunchedEffect(Unit) {
        if (!needRequest) return@LaunchedEffect
        if (prefs.skipNotificationPermissionPrompt) return@LaunchedEffect
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            return@LaunchedEffect
        }
        launcher.launch(permission)
    }
}
