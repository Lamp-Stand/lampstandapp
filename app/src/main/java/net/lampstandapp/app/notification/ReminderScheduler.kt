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
 * Reliable background meditation reminder scheduler powered by AlarmManager (exact alarms).
 * Runs 5-hour interval notification chains to nudge users to start or finish
 * today's session, stopping automatically once completed or when path is finished.
 */
object ReminderScheduler {
    private const val CHANNEL_ID = "lampstand_daily_reminder"
    private const val NOTIFICATION_ID = 4101
    private const val ALARM_REQUEST = 4102

    /** Hours between meditation reminder rings while a session is pending. */
    private const val CHAIN_STEP_HOURS = 5

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

    internal fun chainSlots(
        anchorHour: Int,
        anchorMinute: Int
    ): List<Pair<Int, Int>> = (0 until 4).map { step ->
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, anchorHour)
            set(Calendar.MINUTE, anchorMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.HOUR_OF_DAY, CHAIN_STEP_HOURS * step)
        }
        calendar.get(Calendar.HOUR_OF_DAY) to calendar.get(Calendar.MINUTE)
    }

    /** Suppress a re-arm of the slot that just fired if the OS delivered it early. */
    private const val REARM_GUARD_MS = 10 * 60 * 1000L

    fun sync(context: Context) {
        ServiceLocator.init(context)
        val active = ServiceLocator.hasConvexSession() &&
            ServiceLocator.reminderEnabled() &&
            !ServiceLocator.isPathComplete() &&
            !ServiceLocator.todaySessionCompleted()

        if (!active) {
            cancel(context)
            return
        }
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
        val cutoff = maxOf(now, ServiceLocator.lastReminderAt() + REARM_GUARD_MS)
        val slotsMap = ServiceLocator.getReminderSlotsTimes()
        val calendar = Calendar.getInstance()

        val todayTriggers = mutableListOf<Long>()
        if (!skipToday) {
            for ((_, time) in slotsMap) {
                calendar.timeInMillis = now
                calendar.set(Calendar.HOUR_OF_DAY, time.first)
                calendar.set(Calendar.MINUTE, time.second)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                if (calendar.timeInMillis > cutoff) {
                    todayTriggers.add(calendar.timeInMillis)
                }
            }
            if (todayTriggers.isNotEmpty()) {
                return todayTriggers.minOrNull()!!
            }
        }

        val tomorrowTriggers = mutableListOf<Long>()
        for ((_, time) in slotsMap) {
            calendar.timeInMillis = now
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, time.first)
            calendar.set(Calendar.MINUTE, time.second)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            tomorrowTriggers.add(calendar.timeInMillis)
        }
        return tomorrowTriggers.minOrNull() ?: (now + 24 * 60 * 60 * 1000L)
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
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            ServiceLocator.recordReminderShown()
        } catch (_: SecurityException) {
            // best-effort
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
        ReminderScheduler.sync(context)
    }
}
