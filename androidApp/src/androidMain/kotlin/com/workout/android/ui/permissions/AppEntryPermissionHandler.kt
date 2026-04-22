package com.workout.android.ui.permissions

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.workout.android.R
import com.workout.shared.platform.AndroidTimerSettings
import org.koin.android.ext.android.getKoin

@Composable
fun AppEntryPermissionHandler() {
    val context = LocalContext.current
    val activity = context as? ComponentActivity ?: return
    val prefs = remember { activity.getKoin().get<AndroidTimerSettings>() }

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val permission = Manifest.permission.POST_NOTIFICATIONS
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        )
    }
    var showBanner by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        if (!granted) showBanner = true
    }

    LaunchedEffect(Unit) {
        if (permissionGranted || prefs.skipNotificationPermissionPrompt) return@LaunchedEffect
        launcher.launch(permission)
    }

    if (showBanner && !permissionGranted && !prefs.skipNotificationPermissionPrompt) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(start = 12.dp, end = 12.dp, bottom = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            stringResource(R.string.permission_notifications_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.Black
                        )
                        Text(
                            stringResource(R.string.permission_banner_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF555555)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (activity.shouldShowRequestPermissionRationale(permission)) {
                                launcher.launch(permission)
                            } else {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(stringResource(R.string.allow))
                    }
                    IconButton(
                        onClick = {
                            prefs.skipNotificationPermissionPrompt = true
                            showBanner = false
                        }
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.Black)
                    }
                }
            }
        }
    }
}
