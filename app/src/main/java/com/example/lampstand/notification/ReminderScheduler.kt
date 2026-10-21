package com.example.lampstand.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.lampstand.MainActivity
import com.example.lampstand.R
import com.example.lampstand.data.ServiceLocator
import java.util.Calendar

/**
 * The mobile-only reminder: the web stores a study-time slot, Android also
 * keeps the exact clock time locally and fires a daily notification.
 */
object ReminderScheduler {
    private const val CHANNEL_ID = "lampstand_daily_reminder"
    private const val NOTIFICATION_ID = 4101
    private const val ALARM_REQUEST = 4102

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Daily Reminder",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "A daily nudge to spend time with God"
                }
            )
        }
    }

    /**
     * Web parity: reminders only exist while a path is active. This also
     * stops the alarm after Day 7 and re-arms it when a new path starts.
     */
    fun sync(context: Context) {
        val settings = ServiceLocator.reminderSettings()
        val home = ServiceLocator.cachedHomeState()
        if (settings != null && settings.enabled && home?.state == "active") {
            schedule(context, settings.hour, settings.minute)
        } else {
            cancel(context)
        }
    }

    fun schedule(context: Context, hour: Int, minute: Int) {
        if (hour !in 0..23 || minute !in 0..59) return
        ensureChannel(context)
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val triggerAt = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }.timeInMillis
        val pending = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST,
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarm.setRepeating(AlarmManager.RTC_WAKEUP, triggerAt, AlarmManager.INTERVAL_DAY, pending)
        } catch (_: SecurityException) {
            // Exact-alarm restrictions don't apply here, but never crash the UI.
        }
    }

    fun cancel(context: Context) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST,
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        try {
            alarm.cancel(pending)
            pending.cancel()
        } catch (_: Exception) {
            // best-effort
        }
    }

    fun showNotification(context: Context) {
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        // Don't nudge for a finished path or a session already done today.
        val home = ServiceLocator.cachedHomeState()
        if (home != null && home.state != "active") return
        if (ServiceLocator.completedSessionToday()) return
        ensureChannel(context)
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time with God")
            .setContentText("Your daily Lampstand session is ready.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and notify.
        }
    }
}

/** Fires the daily notification and re-arms after reboot/timezone changes. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            ServiceLocator.init(context)
            ReminderScheduler.sync(context)
            return
        }
        ReminderScheduler.showNotification(context)
    }
}
