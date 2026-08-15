package com.nsvault.app.data.audio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.nsvault.app.MainActivity
import com.nsvault.app.R
import com.nsvault.app.domain.model.RecorderState

/**
 * Builds the recording foreground notification: elapsed chronometer
 * plus Pause/Resume and Stop actions, so a recording can be driven
 * without reopening the app.
 */
object RecordingNotifications {

    const val NOTIFICATION_ID = 41

    private const val CHANNEL_ID = "recording"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Recording",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shown while a recording is in progress"
                setShowBadge(false)
            },
        )
    }

    fun build(
        context: Context,
        state: RecorderState,
        elapsedMillis: Long,
    ): android.app.Notification {
        val isPaused = state == RecorderState.Paused

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_recording)
            .setContentTitle(if (isPaused) "Recording paused" else "Recording")
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (isPaused) {
            builder.setUsesChronometer(false)
            builder.setContentText(formatElapsed(elapsedMillis))
        } else {
            builder.setWhen(System.currentTimeMillis() - elapsedMillis)
            builder.setUsesChronometer(true)
        }

        if (isPaused) {
            builder.addAction(0, "Resume", serviceAction(context, RecordingService.ACTION_RESUME))
        } else {
            builder.addAction(0, "Pause", serviceAction(context, RecordingService.ACTION_PAUSE))
        }
        builder.addAction(0, "Stop", serviceAction(context, RecordingService.ACTION_STOP))

        return builder.build()
    }

    private fun serviceAction(context: Context, action: String): PendingIntent =
        PendingIntent.getService(
            context,
            action.hashCode(),
            Intent(context, RecordingService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun formatElapsed(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }
}
