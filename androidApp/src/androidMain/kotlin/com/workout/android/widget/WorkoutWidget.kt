package com.workout.android.widget

import android.content.Context
import android.content.Intent
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.wrapContentSize
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.workout.android.MainActivity
import com.workout.android.R
import com.workout.core.model.Workout
import com.workout.core.repository.WorkoutRepository
import kotlinx.coroutines.flow.first
import org.koin.core.context.GlobalContext

class WorkoutWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = GlobalContext.get().get<WorkoutRepository>()
        val workouts = repository.getWorkouts().first()
        val lastWorkout = workouts
            .filter { it.lastStartedAt != null }
            .maxByOrNull { it.lastStartedAt ?: 0L }

        provideContent {
            Content(context, lastWorkout)
        }
    }

    @Suppress("RestrictedApi")
    @Composable
    private fun Content(context: Context, workout: Workout?) {
        if (workout == null) {
            val createIntent = Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_OPEN_CREATE, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(R.color.widget_background)
                    .padding(16.dp)
                    .clickable(actionStartActivity(createIntent)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    context.getString(R.string.widget_create_hint),
                    style = TextStyle(
                        color = ColorProvider(R.color.widget_text_primary),
                        fontSize = 14.sp
                    )
                )
            }
        } else {
            val startIntent = Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_WORKOUT_ID, workout.id)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(R.color.widget_background)
                    .padding(16.dp)
                    .clickable(actionStartActivity(startIntent)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    workout.name,
                    modifier = GlanceModifier.fillMaxWidth(),
                    style = TextStyle(
                        color = ColorProvider(R.color.widget_text_primary),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    formatDuration(context, workout.totalDurationSeconds),
                    style = TextStyle(
                        color = ColorProvider(R.color.widget_text_secondary),
                        fontSize = 13.sp
                    )
                )
                Spacer(modifier = GlanceModifier.height(10.dp))
                Text(
                    "▶",
                    modifier = GlanceModifier.wrapContentSize(),
                    style = TextStyle(
                        color = ColorProvider(R.color.widget_text_primary),
                        fontSize = 20.sp
                    )
                )
            }
        }
    }

    private fun formatDuration(context: Context, seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return when {
            hours > 0 -> context.getString(R.string.widget_duration_hours, hours, minutes)
            minutes > 0 -> context.getString(R.string.widget_duration_minutes, minutes)
            else -> context.getString(R.string.widget_duration_seconds, secs)
        }
    }
}
