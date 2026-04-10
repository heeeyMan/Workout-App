package com.workout.android.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.workout.android.MainActivity
import com.workout.android.R

/**
 * Foreground service на время активной тренировки: процесс остаётся приоритетным в фоне,
 * таймер и звуки продолжают работать после сворачивания приложения.
 * Кнопки «Пауза» и «Дальше» в уведомлении дублируются на связанных часах Wear OS.
 */
class WorkoutTimerForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannelIfNeeded()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_RUN) {
            stopSelf()
            return START_NOT_STICKY
        }

        val workoutName = intent?.getStringExtra(EXTRA_WORKOUT_NAME).orEmpty()
        val phaseLine = intent?.getStringExtra(EXTRA_PHASE_LINE).orEmpty()
        val detailLine = intent?.getStringExtra(EXTRA_DETAIL_LINE).orEmpty()
        val timeLine = intent?.getStringExtra(EXTRA_TIME_LINE).orEmpty()
        val isPaused = intent?.getBooleanExtra(EXTRA_IS_PAUSED, false) ?: false

        val notification = buildNotification(workoutName, phaseLine, detailLine, timeLine, isPaused)
        startForeground(NOTIFICATION_ID, notification)
        return START_NOT_STICKY
    }

    private fun buildNotification(
        workoutName: String,
        phaseLine: String,
        detailLine: String,
        timeLine: String,
        isPaused: Boolean
    ): Notification {
        val openApp = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = workoutName.ifBlank { getString(R.string.timer_notification_title_fallback) }
        val text = buildString {
            append(phaseLine)
            if (timeLine.isNotBlank()) {
                if (isNotEmpty()) append(" · ")
                append(timeLine)
            }
        }.ifBlank { getString(R.string.timer_notification_running) }

        val style = NotificationCompat.BigTextStyle()
            .setBigContentTitle(title)
            .bigText(
                buildString {
                    append(text)
                    if (detailLine.isNotBlank()) {
                        append("\n")
                        append(detailLine)
                    }
                }
            )

        val pauseIntent = Intent(this, TimerNotificationActionReceiver::class.java).apply {
            action = TimerNotificationActionReceiver.ACTION_TOGGLE_PAUSE
            setPackage(packageName)
        }
        val pausePendingIntent = PendingIntent.getBroadcast(
            this,
            REQ_PAUSE,
            pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val skipIntent = Intent(this, TimerNotificationActionReceiver::class.java).apply {
            action = TimerNotificationActionReceiver.ACTION_SKIP_PHASE
            setPackage(packageName)
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            this,
            REQ_SKIP,
            skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setSubText(detailLine.takeIf { it.isNotBlank() })
            .setStyle(style)
            .setContentIntent(contentPendingIntent)
            .addAction(
                NotificationCompat.Action(
                    0,
                    getString(if (isPaused) R.string.notif_action_resume else R.string.notif_action_pause),
                    pausePendingIntent
                )
            )
            .addAction(
                NotificationCompat.Action(
                    0,
                    getString(R.string.notif_action_next_phase),
                    skipPendingIntent
                )
            )
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    override fun onDestroy() {
        try {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) { }
        super.onDestroy()
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.timer_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.timer_notification_channel_desc)
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "workout_timer_foreground"
        const val NOTIFICATION_ID = 71001

        private const val ACTION_RUN = "com.workout.android.timer.RUN"

        private const val EXTRA_WORKOUT_NAME = "workout_name"
        private const val EXTRA_PHASE_LINE = "phase_line"
        private const val EXTRA_DETAIL_LINE = "detail_line"
        private const val EXTRA_TIME_LINE = "time_line"
        private const val EXTRA_IS_PAUSED = "is_paused"

        private const val REQ_PAUSE = 71002
        private const val REQ_SKIP = 71003

        fun update(
            context: Context,
            workoutName: String,
            phaseLine: String,
            detailLine: String,
            timeLine: String,
            isPaused: Boolean
        ) {
            val app = context.applicationContext
            val intent = Intent(app, WorkoutTimerForegroundService::class.java).apply {
                action = ACTION_RUN
                putExtra(EXTRA_WORKOUT_NAME, workoutName)
                putExtra(EXTRA_PHASE_LINE, phaseLine)
                putExtra(EXTRA_DETAIL_LINE, detailLine)
                putExtra(EXTRA_TIME_LINE, timeLine)
                putExtra(EXTRA_IS_PAUSED, isPaused)
            }
            ContextCompat.startForegroundService(app, intent)
        }

        fun stop(context: Context) {
            val app = context.applicationContext
            app.stopService(Intent(app, WorkoutTimerForegroundService::class.java))
        }
    }
}
