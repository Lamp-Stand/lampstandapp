package net.lampstandapp.app.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import net.lampstandapp.app.MainActivity
import net.lampstandapp.app.R
import net.lampstandapp.app.data.ServiceLocator
import java.util.Calendar

/**
 * Session reminders are on by default. During quiet-hours-aware slots
 * (6:45 AM, 11:45 AM, 4:45 PM, 9:45 PM) the app nudges the user to start or
 * finish today's session, stopping once the session is completed or the user
 * switches reminders off in Profile.
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
                    "Session Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Gentle nudges to start or finish today's session"
                }
            )
        }
    }

    /**
     * Quiet hours run 6:45 AM to 10:00 PM. Inside that window a nudge fires
     * every 5 hours, and the one-shot alarm re-arms itself after each delivery
     * until today's session is completed or the user switches reminders off.
     */
    private val reminderSlots = listOf(6 to 45, 11 to 45, 16 to 45, 21 to 45)

    /** Suppress a re-arm of the slot that just fired if the OS delivered it early. */
    private const val REARM_GUARD_MS = 10 * 60 * 1000L

    fun sync(context: Context) {
        ServiceLocator.init(context)
        val active = ServiceLocator.hasConvexSession() &&
            ServiceLocator.reminderEnabled() &&
            !ServiceLocator.isPathComplete()
        if (!active) {
            cancel(context)
            return
        }
        // Completed today? Skip the rest of today but keep tomorrow's chain
        // alive so the user is nudged again on the next day.
        scheduleNext(context, skipToday = ServiceLocator.todaySessionCompleted())
    }

    private fun scheduleNext(context: Context, skipToday: Boolean = false) {
        ensureChannel(context)
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val triggerAt = nextTriggerAt(skipToday = skipToday)
        val pending = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST,
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            if (canScheduleExactAlarms(context)) {
                alarm.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pending
                )
            } else {
                // Inexact fallback until the user grants exact-alarm access.
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        } catch (_: SecurityException) {
            try {
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } catch (_: SecurityException) {
                // Never crash the UI if the OS rejects alarm scheduling.
            }
        }
    }

    internal fun nextTriggerAt(
        now: Long = System.currentTimeMillis(),
        skipToday: Boolean = false
    ): Long {
        val cutoff = maxOf(now, ServiceLocator.lastReminderAt() + REARM_GUARD_MS)
        val calendar = Calendar.getInstance()
        if (!skipToday) {
            for ((hour, minute) in reminderSlots) {
                calendar.timeInMillis = now
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                if (calendar.timeInMillis > cutoff) return calendar.timeInMillis
            }
        }
        calendar.timeInMillis = now
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, reminderSlots.first().first)
        calendar.set(Calendar.MINUTE, reminderSlots.first().second)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return false
        return alarm.canScheduleExactAlarms()
    }

    /**
     * Opens the system screen that grants exact-alarm access. Android 12+
     * denies this by default for most newly installed apps.
     */
    fun requestExactAlarmAccess(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        if (canScheduleExactAlarms(context)) return
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:${context.packageName}")
            if (context !is android.app.Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // Some OEMs don't expose the exact-alarm settings screen.
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
        ServiceLocator.init(context)
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        // Don't nudge signed-out users, after the path is finished, or once
        // today's session is done.
        if (!ServiceLocator.hasConvexSession()) return
        if (ServiceLocator.isPathComplete()) return
        if (ServiceLocator.todaySessionCompleted()) return
        val messages = ReminderMessages.pool(appOpenedToday = ServiceLocator.appOpenedToday())
        if (messages.isEmpty()) return
        val message = messages[ServiceLocator.nextReminderMessageIndex(messages.size)]
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
            .setContentTitle(message.title)
            .setContentText(message.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            ServiceLocator.recordReminderShown()
        } catch (_: SecurityException) {
            // Permission revoked between the check and notify.
        }
    }
}

/** Fires the daily notification and re-arms after reboot/timezone changes. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        ServiceLocator.init(context)
        val action = intent?.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        ) {
            ReminderScheduler.sync(context)
            return
        }
        ReminderScheduler.showNotification(context)
        // The alarm is one-shot; arm the next slot (or tomorrow after a
        // completed session) before returning.
        ReminderScheduler.sync(context)
    }
}
