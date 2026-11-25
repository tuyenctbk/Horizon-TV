package com.example.data.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.AlertSeverity
import com.example.data.model.SevereWeatherAlert

object SevereWeatherAlertNotificationHelper {

    const val CHANNEL_ID = "horizon_severe_weather_alerts"
    const val EXTRA_ALERT_ID = "extra_severe_alert_id"
    const val EXTRA_ALERT_TITLE = "extra_severe_alert_title"
    const val EXTRA_ALERT_SUMMARY = "extra_severe_alert_summary"
    const val EXTRA_ALERT_INSTRUCTIONS = "extra_severe_alert_instructions"
    const val EXTRA_ALERT_SEVERITY = "extra_severe_alert_severity"

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing != null) return

            val channelName = context.getString(R.string.notif_channel_severe_name)
            val channelDesc = context.getString(R.string.notif_channel_severe_desc)

            val channel = NotificationChannel(
                CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = channelDesc
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0L, 400L, 200L, 500L)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    /**
     * Dispatches a high-priority push notification for a severe weather alert in the user's area.
     * Returns true if the notification was posted to the system notification manager.
     */
    fun postSevereWeatherNotification(
        context: Context,
        locationName: String,
        alert: SevereWeatherAlert
    ): Boolean {
        ensureNotificationChannel(context)
        if (!hasNotificationPermission(context)) {
            return false
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ALERT_ID, alert.id)
            putExtra(EXTRA_ALERT_TITLE, alert.title)
            putExtra(EXTRA_ALERT_SUMMARY, alert.summary)
            putExtra(EXTRA_ALERT_INSTRUCTIONS, alert.safetyInstructions)
            putExtra(EXTRA_ALERT_SEVERITY, alert.severity.name)
        }

        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

        val notificationId = (locationName.lowercase().hashCode() * 31) + alert.id.hashCode()
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            pendingFlags
        )

        val accentColor = when (alert.severity) {
            AlertSeverity.EMERGENCY -> Color.parseColor("#C5221F")
            AlertSeverity.WARNING -> Color.parseColor("#D9381E")
            AlertSeverity.WATCH -> Color.parseColor("#E37400")
            AlertSeverity.ADVISORY -> Color.parseColor("#0B57D0")
        }

        val titleText = "$locationName • ${alert.title}"
        val expandedBody = "${alert.summary}\n\n${alert.safetyInstructions}"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(titleText)
            .setContentText(alert.summary)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(titleText)
                    .bigText(expandedBody)
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(accentColor)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        return try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
