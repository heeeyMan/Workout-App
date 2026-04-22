package com.workout.android.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.workout.android.MainActivity
import com.workout.android.R
import com.workout.android.widget.WorkoutWidget
import com.workout.shared.feature.timer.TimerIntent
import com.workout.shared.platform.AndroidForegroundTimerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WorkoutTimerForegroundService : Service() {

    private var mediaSession: MediaSession? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannelIfNeeded()
        setupMediaSession()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_RUN) {
            stopSelf()
            return START_NOT_STICKY
        }

        val workoutName = intent.getStringExtra(AndroidForegroundTimerService.EXTRA_WORKOUT_NAME).orEmpty()
        val phaseLine = intent.getStringExtra(AndroidForegroundTimerService.EXTRA_PHASE_LINE).orEmpty()
        val detailLine = intent.getStringExtra(AndroidForegroundTimerService.EXTRA_DETAIL_LINE).orEmpty()
        val isPaused = intent.getBooleanExtra(AndroidForegroundTimerService.EXTRA_IS_PAUSED, false)
        val phaseType = intent.getStringExtra(AndroidForegroundTimerService.EXTRA_PHASE_TYPE).orEmpty()

        updateMediaSessionPlaybackState(isPaused)

        // Первый вызов — startForeground, дальнейшие обновления через NotificationManager.notify напрямую
        val notification = buildNotificationInternal(workoutName, phaseLine, detailLine, "", isPaused, phaseType)
        startForeground(NOTIFICATION_ID, notification)
        return START_NOT_STICKY
    }

    private fun setupMediaSession() {
        mediaSession = MediaSession(this, "WorkoutTimer").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    AndroidForegroundTimerService.dispatchCallback?.invoke(TimerIntent.TogglePause)
                }
                override fun onPause() {
                    AndroidForegroundTimerService.dispatchCallback?.invoke(TimerIntent.TogglePause)
                }
                override fun onSkipToNext() {
                    AndroidForegroundTimerService.dispatchCallback?.invoke(TimerIntent.SkipPhase)
                }
                override fun onSkipToPrevious() {
                    AndroidForegroundTimerService.dispatchCallback?.invoke(TimerIntent.PreviousPhase)
                }
            })
            isActive = true
        }
    }

    private fun updateMediaSessionPlaybackState(isPaused: Boolean) {
        val state = if (isPaused) PlaybackState.STATE_PAUSED else PlaybackState.STATE_PLAYING
        mediaSession?.setPlaybackState(
            PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or
                    PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackState.ACTION_PLAY_PAUSE
                )
                .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f)
                .build()
        )
    }

    private fun buildNotificationInternal(
        workoutName: String, phaseLine: String, detailLine: String,
        timeLine: String, isPaused: Boolean, phaseType: String
    ): Notification {
        return buildNotificationWithContext(this, workoutName, phaseLine, detailLine, timeLine, isPaused, phaseType,
            mediaSession?.sessionToken)
    }

    override fun onDestroy() {
        mediaSession?.apply { isActive = false; release() }
        mediaSession = null
        try {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) { }
        updateWidget()
        super.onDestroy()
    }

    private fun updateWidget() {
        val context = applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            val manager = GlanceAppWidgetManager(context)
            manager.getGlanceIds(WorkoutWidget::class.java)
                .forEach { id -> WorkoutWidget().update(context, id) }
        }
    }

    private fun createChannelIfNeeded() {
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

        private const val REQ_PAUSE = 71002
        private const val REQ_SKIP = 71003
        private const val REQ_PREVIOUS = 71004

        private const val COLOR_WORK_ORANGE = 0xFFFF6B35.toInt()
        private const val COLOR_REST_GREEN = 0xFF4CAF50.toInt()
        private const val COLOR_PREP_GRAY = 0xFF9E9E9E.toInt()

        /** Вызывается из [AndroidForegroundTimerService] через reflection для обновления без startForegroundService. */
        @JvmStatic
        fun buildNotificationStatic(
            context: Context,
            workoutName: String, phaseLine: String, detailLine: String,
            timeLine: String, isPaused: Boolean, phaseType: String
        ): Notification {
            // Для static вызова — без mediaSession token (он в Service instance)
            return buildNotificationWithContext(context, workoutName, phaseLine, detailLine, timeLine, isPaused, phaseType, null)
        }

        private fun buildNotificationWithContext(
            context: Context,
            workoutName: String, phaseLine: String, detailLine: String,
            timeLine: String, isPaused: Boolean, phaseType: String,
            sessionToken: android.media.session.MediaSession.Token?
        ): Notification {
            val openApp = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val contentPI = PendingIntent.getActivity(
                context, 0, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = workoutName.ifBlank { context.getString(R.string.timer_notification_title_fallback) }

            // contentText: "Work · Exercise 1 · Set 1/5 · 00:40"
            val text = buildString {
                if (phaseLine.isNotBlank()) append(phaseLine)
                if (detailLine.isNotBlank()) { if (isNotEmpty()) append(" · "); append(detailLine) }
                if (timeLine.isNotBlank()) { if (isNotEmpty()) append(" · "); append(timeLine) }
            }.ifBlank { context.getString(R.string.timer_notification_running) }

            val previousPI = actionPI(context, TimerNotificationActionReceiver.ACTION_PREVIOUS_PHASE, REQ_PREVIOUS)
            val pausePI = actionPI(context, TimerNotificationActionReceiver.ACTION_TOGGLE_PAUSE, REQ_PAUSE)
            val skipPI = actionPI(context, TimerNotificationActionReceiver.ACTION_SKIP_PHASE, REQ_SKIP)

            val pauseIcon = if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause
            val pauseLabel = context.getString(if (isPaused) R.string.notif_action_resume else R.string.notif_action_pause)

            val accentColor = when (phaseType) {
                "work" -> COLOR_WORK_ORANGE
                "rest" -> COLOR_REST_GREEN
                else -> COLOR_PREP_GRAY
            }

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(contentPI)
                .setColor(accentColor)
                .setColorized(true)
                .addAction(NotificationCompat.Action(android.R.drawable.ic_media_previous, context.getString(R.string.cd_previous_phase), previousPI))
                .addAction(NotificationCompat.Action(pauseIcon, pauseLabel, pausePI))
                .addAction(NotificationCompat.Action(android.R.drawable.ic_media_next, context.getString(R.string.notif_action_next_phase), skipPI))
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSilent(true)
                .setOnlyAlertOnce(true)

            if (sessionToken != null) {
                val style = androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(
                        android.support.v4.media.session.MediaSessionCompat.Token.fromToken(sessionToken)
                    )
                    .setShowActionsInCompactView(0, 1, 2)
                builder.setStyle(style)
            }

            return builder.build()
        }

        private fun actionPI(context: Context, action: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, TimerNotificationActionReceiver::class.java).apply {
                this.action = action
                setPackage(context.packageName)
            }
            return PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun stop(context: Context) {
            context.applicationContext.stopService(
                Intent(context.applicationContext, WorkoutTimerForegroundService::class.java)
            )
        }
    }
}
