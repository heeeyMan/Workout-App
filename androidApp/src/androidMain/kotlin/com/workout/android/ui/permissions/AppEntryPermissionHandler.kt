package com.workout.android.ui.permissions

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.workout.android.R
import com.workout.android.data.TimerPreferences
import org.koin.android.ext.android.getKoin

@Composable
fun AppEntryPermissionHandler() {
    val context = LocalContext.current
    val activity = context as? ComponentActivity ?: return
    val prefs = activity.getKoin().get<TimerPreferences>()
    var showDialog by remember { mutableStateOf(false) }

    val needRequest = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val permission = Manifest.permission.POST_NOTIFICATIONS

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { showDialog = false }

    LaunchedEffect(Unit) {
        if (!needRequest) return@LaunchedEffect
        if (prefs.skipNotificationPermissionPrompt) return@LaunchedEffect
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            return@LaunchedEffect
        }
        showDialog = true
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.permission_notifications_title)) },
            text = {
                Text(stringResource(R.string.permission_notifications_body))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        launcher.launch(permission)
                    }
                ) {
                    Text(stringResource(R.string.allow))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        prefs.skipNotificationPermissionPrompt = true
                        showDialog = false
                    }
                ) {
                    Text(stringResource(R.string.later))
                }
            }
        )
    }
}
