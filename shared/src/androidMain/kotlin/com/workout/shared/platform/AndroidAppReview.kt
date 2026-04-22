package com.workout.shared.platform

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.google.android.play.core.review.ReviewManagerFactory

@Composable
actual fun rememberAppReviewLauncher(): () -> Unit {
    val context = LocalContext.current
    return lambda@{
        val activity = context as? Activity ?: return@lambda
        val manager = ReviewManagerFactory.create(context)
        manager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                manager.launchReviewFlow(activity, task.result)
            }
        }
    }
}
