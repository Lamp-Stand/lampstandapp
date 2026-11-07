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
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import net.lampstandapp.app.MainActivity
import net.lampstandapp.app.R
import net.lampstandapp.app.data.ServiceLocator
import java.util.Calendar

/**
 * Meditation session reminders are on by default. Four INDEPENDENT daily
 * alarms — Morning, Noon, Evening, Night — each locked to its own window
 * inside 5:00 AM – 11:00 PM (ReminderPreferences.WINDOWS_MINUTES). They
 * ring only as a nudge to start or finish today's session, stopping once
 * the session is completed or the user switches reminders off in Profile.
 */
object ReminderScheduler {
    private const val CHANNEL_ID = "lampstand_daily_reminder"
    private const val NOTIFICATION_ID = 4101
    private const val ALARM_REQUEST = 4102

    /** Suppress a re-arm of the slot that just fired if the OS delivered it early. */
    private const val REARM_GUARD_MS = 10 * 60 * 1000L

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
     * The reminder schedule: four independent daily alarms (Morning, Noon,
     * Evening, Night) inside 5:00 AM – 11:00 PM — until today's session is
     * completed or the user switches reminders off in Profile.
     */

    fun sync(context: Context) {
        ServiceLocator.init(context)
        val active = ServiceLocator.hasConvexSession() &&
            ServiceLocator.reminderEnabled() &&
            !ServiceLocator.isPathComplete()

        if (!active) {
            cancel(context)
            return
        }
        // Completed today? Skip the rest of today but keep tomorrow's first
        // alarm alive so the user is nudged again on the next day.
        scheduleNext(context, skipToday = ServiceLocator.todaySessionCompleted())
    }

    private fun scheduleNext(context: Context, skipToday: Boolean = false) {
        ensureChannel(context)
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val triggerAt = nextTriggerAt(skipToday = skipToday)

        Log.d(
            "ReminderScheduler",
            "Scheduling next reminder: ${java.util.Date(triggerAt)}"
        )
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
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        } catch (_: SecurityException) {
            try {
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } catch (_: SecurityException) {
                // best-effort
            }
        }
    }

    internal fun nextTriggerAt(
        now: Long = System.currentTimeMillis(),
        skipToday: Boolean = false
    ): Long {
        // If the OS delivered the last alarm early, hold the next re-arm
        // until at least 10 minutes after it (their REARM_GUARD_MS idea).
        val last = ServiceLocator.lastReminderAt()
        val cutoff = if (last != null) maxOf(now, last + REARM_GUARD_MS) else now
        val phases = ServiceLocator.reminderPreferences().allPeriodTimes()
        return ReminderCalculator.calculateNextPhaseTrigger(
            now = cutoff,
            phases = phases,
            skipToday = skipToday
        )
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return false
        return alarm.canScheduleExactAlarms()
    }

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
            // best-effort
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
        Log.d(
            "ReminderReceiver",
            "Attempting to show notification"
        )

        ServiceLocator.init(context)
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        if (!ServiceLocator.hasConvexSession()) return
        if (ServiceLocator.isPathComplete()) return
        if (ServiceLocator.todaySessionCompleted()) return

        val messages = ReminderMessages.pool(appOpenedToday = ServiceLocator.appOpenedToday())
        if (messages.isEmpty()) return
        val message = messages[ServiceLocator.nextReminderMessageIndex(messages.size)]
        ensureChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openApp = PendingIntent.getActivity(
            context,
            0,
            intent,
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
            val notificationId = Calendar.getInstance().let {
                NOTIFICATION_ID + it.get(Calendar.HOUR_OF_DAY)
            }
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // best-effort
        }
    }
}

/** Fires the daily notification and re-arms after reboot/timezone changes. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {

        Log.d(
            "ReminderReceiver",
            "Alarm received at: ${java.util.Date()}"
        )

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
        // Normal alarm delivery.
        // Record that the alarm fired before showing the notification.
        ServiceLocator.recordReminderDelivered()

        // Display the current notification first.
        ReminderScheduler.showNotification(context)

        // Then schedule the next reminder.
        ReminderScheduler.sync(context)
    }
}
