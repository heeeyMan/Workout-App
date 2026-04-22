package com.workout.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import android.content.Intent
import java.io.File

@Composable
actual fun rememberJsonExporter(onComplete: (success: Boolean) -> Unit): (String, String) -> Unit {
    val context = LocalContext.current
    return { suggestedName, content ->
        try {
            val file = File(context.cacheDir, suggestedName)
            file.writeText(content, Charsets.UTF_8)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, null))
            onComplete(true)
        } catch (e: Exception) {
            onComplete(false)
        }
    }
}
