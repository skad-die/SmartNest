package com.eldroid.smartnest.alerts

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.eldroid.smartnest.R
import com.eldroid.smartnest.data.model.AlertEvent
import com.eldroid.smartnest.ui.dashboard.DashboardActivity
import com.eldroid.smartnest.ui.history.HistoryActivity
import com.eldroid.smartnest.ui.history.HistoryContract

/** Builds and posts the notifications used by mobile alerts. */
class AlertNotifier(private val context: Context) {

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MONITORING,
                context.getString(R.string.alert_channel_monitoring_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.alert_channel_monitoring_description)
                setShowBadge(false)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALERTS,
                context.getString(R.string.alert_channel_alerts_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.alert_channel_alerts_description)
            }
        )
    }

    /** The persistent notification shown while the monitoring service is running. */
    fun monitoringNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, DashboardActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_MONITORING)
            .setSmallIcon(R.drawable.ic_notification_alert)
            .setContentTitle(context.getString(R.string.alert_monitoring_title))
            .setContentText(context.getString(R.string.alert_monitoring_text))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

    fun showAlert(event: AlertEvent, deviceName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // One notification per device and alert type: a repeat replaces the previous one.
        val notificationId = (event.macKey + event.type).hashCode()

        val openHistory = PendingIntent.getActivity(
            context,
            notificationId,
            HistoryActivity.intent(context, event.macKey, deviceName, HistoryContract.Tab.ALERTS),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = AlertFormatter.message(context, event)
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_notification_alert)
            .setContentTitle(
                context.getString(
                    R.string.alert_notification_title,
                    AlertFormatter.title(context, event),
                    deviceName
                )
            )
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setWhen(event.timestamp)
            .setShowWhen(true)
            .setContentIntent(openHistory)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    companion object {
        const val MONITORING_NOTIFICATION_ID = 1001
        private const val CHANNEL_MONITORING = "alert_monitoring"
        private const val CHANNEL_ALERTS = "cage_alerts"
    }
}
