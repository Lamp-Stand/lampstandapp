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
 * Four INDEPENDENT daily alarms — Morning, Noon, Evening, Night — locked to
 * their own windows inside 5:00 AM – 11:00 PM (ReminderPreferences).
 *
 * Model: arm each slot once for its next occurrence and leave it alone.
 * A slot re-arms itself only after it fires. sync() only fills gaps (missed
 * fire, boot, prefs change) — it does not cancel/rebuild the whole day on
 * every app resume. All slots are cancelled only when today's session is
 * complete, reminders are off, or the path is finished.
 */
object ReminderScheduler {
    private const val CHANNEL_ID = "lampstand_daily_reminder"
    private const val NOTIFICATION_ID = 4101
    private const val ALARM_REQUEST_BASE = 4102
    private const val SLOT_COUNT = 4
    private const val EXTRA_SLOT = "lampstand.slot"
    internal const val EXTRA_SLOT_KEY = EXTRA_SLOT
    private const val PREFS_NAME = "lampstand_alarm_slots"
    private const val KEY_SLOT_PREFIX = "t"

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
     * Idempotent: arm any slot that is missing or stale (stored trigger in the
     * past = missed fire). Never touches a slot whose stored time matches the
     * desired next occurrence.
     */
    fun sync(context: Context) {
        ServiceLocator.init(context)
        val active = ServiceLocator.hasConvexSession() &&
            ServiceLocator.reminderEnabled() &&
            !ServiceLocator.isPathComplete()

        if (!active) {
            cancelAll(context)
            return
        }

        ensureChannel(context)
        val phases = ServiceLocator.reminderPreferences().allPeriodTimes()
        val now = System.currentTimeMillis()

        if (ServiceLocator.todaySessionCompleted()) {
            // Session done for today: drop every pending slot and arm tomorrow's four.
            cancelAll(context)
            phases.forEachIndexed { slot, (hour, minute) ->
                armSlot(context, slot, startOfNextDay(now, hour, minute))
            }
            return
        }

        phases.forEachIndexed { slot, (hour, minute) ->
            val desired = nextOccurrence(now, hour, minute)
            val stored = storedTrigger(context, slot)
            val exists = slotExists(context, slot)
            if (!exists || stored != desired) {
                // Missing, time prefs changed, or stored trigger is stale/past.
                cancelSlot(context, slot)
                armSlot(context, slot, desired)
            }
        }
    }

    /** Full rebuild — use when clock/timezone changed or reminder times were edited. */
    fun reschedule(context: Context) {
        cancelAll(context)
        sync(context)
    }

    private fun nextOccurrence(now: Long, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= now) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.timeInMillis
    }

    private fun startOfNextDay(now: Long, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun receiverIntent(context: Context, slot: Int): Intent =
        Intent(context, ReminderReceiver::class.java).putExtra(EXTRA_SLOT, slot)

    private fun slotPrefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun storedTrigger(context: Context, slot: Int): Long =
        slotPrefs(context).getLong(KEY_SLOT_PREFIX + slot, 0L)

    private fun slotExists(context: Context, slot: Int): Boolean =
        PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_BASE + slot,
            receiverIntent(context, slot),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) != null

    private fun armSlot(context: Context, slot: Int, triggerAt: Long) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        Log.d("ReminderScheduler", "Arming slot $slot at ${java.util.Date(triggerAt)}")
        try {
            slotPrefs(context).edit().putLong(KEY_SLOT_PREFIX + slot, triggerAt).apply()
        } catch (_: Exception) { /* best-effort */ }
        val pending = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_BASE + slot,
            receiverIntent(context, slot),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            if (canScheduleExactAlarms(context)) {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
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

    private fun cancelSlot(context: Context, slot: Int) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        try {
            slotPrefs(context).edit().remove(KEY_SLOT_PREFIX + slot).apply()
        } catch (_: Exception) { /* best-effort */ }
        val pending = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_BASE + slot,
            receiverIntent(context, slot),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        try {
            alarm.cancel(pending)
            pending.cancel()
        } catch (_: Exception) {
            // best-effort
        }
    }

    fun cancelAll(context: Context) {
        for (slot in 0 until SLOT_COUNT) {
            cancelSlot(context, slot)
        }
    }

    /** Drop one fired slot so sync() will re-arm it for tomorrow. */
    internal fun clearFiredSlot(context: Context, slot: Int) {
        if (slot in 0 until SLOT_COUNT) cancelSlot(context, slot)
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

    /** Returns true only when a notification was actually posted. */
    fun showNotification(context: Context): Boolean {
        Log.d("ReminderReceiver", "Attempting to show notification")

        ServiceLocator.init(context)
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        if (!ServiceLocator.hasConvexSession()) return false
        if (ServiceLocator.isPathComplete()) return false
        if (ServiceLocator.todaySessionCompleted()) return false

        val messages = ReminderMessages.pool(appOpenedToday = ServiceLocator.appOpenedToday())
        if (messages.isEmpty()) return false
        val message = messages[ServiceLocator.nextReminderMessageIndex(messages.size)]
        ensureChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_JOURNEY, true)
        }
        val openApp = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle(message.title)
            .setContentText(message.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()

        return try {
            val notificationId = Calendar.getInstance().let {
                NOTIFICATION_ID + it.get(Calendar.HOUR_OF_DAY)
            }
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}

/** Fires the daily notification; re-arms that one slot for tomorrow. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("ReminderReceiver", "Alarm received at: ${java.util.Date()}")

        ServiceLocator.init(context)
        val action = intent?.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        ) {
            // Clock/projected-time changes invalidate stored triggers — full rebuild.
            ReminderScheduler.reschedule(context)
            return
        }

        // Normal alarm delivery: drop this slot, then sync re-arms it for
        // tomorrow without touching the other three pending alarms.
        val slot = intent?.getIntExtra(ReminderScheduler.EXTRA_SLOT_KEY, -1) ?: -1
        ReminderScheduler.clearFiredSlot(context, slot)
        ReminderScheduler.sync(context)

        // Only count delivery when a notification was actually posted —
        // early returns (no permission, session done, etc.) must not bump
        // lastReminderAt.
        if (ReminderScheduler.showNotification(context)) {
            ServiceLocator.recordReminderDelivered()
        }
    }
}
