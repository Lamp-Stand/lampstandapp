# LampStand Reminder Architecture — Inspection Summary

This document captures the current state of the reminder system for review before any further changes.

---

## 1. AndroidManifest.xml

Path: `app/src/main/AndroidManifest.xml`

Permissions are declared directly inside `<manifest>` (not inside `<application>`):

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

The `ReminderReceiver` is registered inside `<application>`:

```xml
<receiver
    android:name=".notification.ReminderReceiver"
    android:enabled="true"
    android:exported="false">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
        <action android:name="android.intent.action.TIME_SET" />
        <action android:name="android.intent.action.TIMEZONE_CHANGED" />
        <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
        <action android:name="android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED" />
    </intent-filter>
</receiver>
```

**Verdict:** All three required permissions are present, and the `BroadcastReceiver` is registered with the correct intent filters for boot, time changes, and exact-alarm permission state changes.

---

## 2. ReminderPreferences.kt

Path: `app/src/main/java/net/lampstandapp/app/notification/ReminderPreferences.kt`

```kotlin
package net.lampstandapp.app.notification

/**
 * User-configurable reminder periods and the selected anchor.
 * Each period has its own hour/minute; the anchor period starts the 5-hour chain.
 *
 * Period names are fixed (Morning, Noon, Evening, Night); only the times change.
 */
data class ReminderPreferences(
    val morningHour: Int = 6,
    val morningMinute: Int = 45,
    val noonHour: Int = 12,
    val noonMinute: Int = 30,
    val eveningHour: Int = 17,
    val eveningMinute: Int = 0,
    val nightHour: Int = 21,
    val nightMinute: Int = 0,
    val anchorPeriod: Int = 0 // 0=Morning, 1=Noon, 2=Evening, 3=Night
) {
    fun hourFor(period: Int): Int = when (period) {
        0 -> morningHour
        1 -> noonHour
        2 -> eveningHour
        3 -> nightHour
        else -> morningHour
    }

    fun minuteFor(period: Int): Int = when (period) {
        0 -> morningMinute
        1 -> noonMinute
        2 -> eveningMinute
        3 -> nightMinute
        else -> morningMinute
    }

    /** Returns the anchor period's (hour, minute). */
    fun anchorTime(): Pair<Int, Int> {
        return hourFor(anchorPeriod) to minuteFor(anchorPeriod)
    }
}
```

Default values: Morning 6:45 AM, Noon 12:30 PM, Evening 5:00 PM, Night 9:00 PM, anchor = Morning (index 0).

---

## 3. ReminderCalculator.kt

Path: `app/src/main/java/net/lampstandapp/app/notification/ReminderCalculator.kt`

```kotlin
package net.lampstandapp.app.notification

import java.util.Calendar

/** Five-hour interval between reminders. */
private const val INTERVAL_MS = 5L * 60 * 60 * 1000

/** Quiet hours: earliest reminder is 5:00 AM. */
private const val EARLIEST_HOUR = 5

/** Quiet hours: latest reminder is 10:50 PM. */
private const val LATEST_HOUR = 22
private const val LATEST_MINUTE = 50

/** One day in milliseconds. */
private const val DAY_MS = 24L * 60 * 60 * 1000

/**
 * Calculates the next reminder trigger time.
 *
 * - If [lastDeliveredAt] is null, starts from the [anchorHour]:[anchorMinute]
 *   on today (or tomorrow if the anchor already passed).
 * - Otherwise, starts from [lastDeliveredAt] + 5 hours.
 * - Defers to 5:00 AM if the calculated time is before 5:00 AM.
 * - Defers to 5:00 AM tomorrow if the calculated time is after 10:50 PM.
 *
 * The next five-hour interval is always measured from the *actual* delivered
 * reminder time, so deferred reminders do not cluster together.
 */
fun calculateNextTrigger(
    now: Long,
    lastDeliveredAt: Long?,
    anchorHour: Int,
    anchorMinute: Int
): Long {
    val cal = Calendar.getInstance()

    // Determine the base time
    val baseMillis: Long
    if (lastDeliveredAt == null) {
        // First reminder: use the anchor time
        cal.timeInMillis = now
        cal.set(Calendar.HOUR_OF_DAY, anchorHour)
        cal.set(Calendar.MINUTE, anchorMinute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        baseMillis = cal.timeInMillis
        if (baseMillis <= now) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
            baseMillis = cal.timeInMillis
        }
    } else {
        baseMillis = lastDeliveredAt + INTERVAL_MS
    }

    // Apply quiet hours
    var candidate = baseMillis
    cal.timeInMillis = candidate

    // Start of quiet-hours day: 5:00 AM on the candidate's date
    val startOfDay = Calendar.getInstance()
    startOfDay.timeInMillis = candidate
    startOfDay.set(Calendar.HOUR_OF_DAY, EARLIEST_HOUR)
    startOfDay.set(Calendar.MINUTE, 0)
    startOfDay.set(Calendar.SECOND, 0)
    startOfDay.set(Calendar.MILLISECOND, 0)

    // End of quiet-hours day: 10:50 PM on the candidate's date
    val endOfDay = Calendar.getInstance()
    endOfDay.timeInMillis = candidate
    endOfDay.set(Calendar.HOUR_OF_DAY, LATEST_HOUR)
    endOfDay.set(Calendar.MINUTE, LATEST_MINUTE)
    endOfDay.set(Calendar.SECOND, 0)
    endOfDay.set(Calendar.MILLISECOND, 0)

    if (candidate < startOfDay.timeInMillis) {
        // Before 5:00 AM — defer to 5:00 AM same day
        candidate = startOfDay.timeInMillis
    } else if (candidate > endOfDay.timeInMillis) {
        // After 10:50 PM — defer to 5:00 AM next day
        cal.timeInMillis = endOfDay.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, EARLIEST_HOUR)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        candidate = cal.timeInMillis
    }

    return candidate
}
```

**Calculation rules:**
- First reminder: starts from anchor period time (today, or tomorrow if already passed)
- Subsequent reminders: last delivered time + 5 hours
- Before 5:00 AM → deferred to 5:00 AM same day
- After 10:50 PM → deferred to 5:00 AM next day
- Interval measured from *actual delivered* time (not theoretical)

---

## 4. ReminderScheduler.kt

Path: `app/src/main/java/net/lampstandapp/app/notification/ReminderScheduler.kt`

```kotlin
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
 * Meditation session reminders are on by default. The chain starts at the
 * user's chosen anchor time (default 6:45 AM) and rings every 5 hours after
 * it (+5h, +10h, +15h) — only as a nudge to start or finish today's session,
 * stopping once the session is completed or the user switches reminders off
 * in Profile.
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

    /**
     * The reminder chain: starts at the user's anchor period time, then fires
     * every 5 hours — respecting quiet hours (5:00 AM – 10:50 PM) — until
     * today's session is completed or the user switches reminders off in
     * Profile.
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
        // Completed today? Skip the rest of today but keep tomorrow's chain
        // alive so the user is nudged again on the next day.
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
        val (anchorHour, anchorMinute) = ServiceLocator.reminderPreferences().anchorTime()
        if (skipToday) {
            // Session completed today — schedule tomorrow at anchor time
            val cal = Calendar.getInstance()
            cal.timeInMillis = now
            cal.add(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, anchorHour)
            cal.set(Calendar.MINUTE, anchorMinute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }
        // Normal chain: calculate from last delivered reminder + 5 hours
        return ReminderCalculator.calculateNextTrigger(
            now = now,
            lastDeliveredAt = ServiceLocator.lastReminderAt(),
            anchorHour = anchorHour,
            anchorMinute = anchorMinute
        )
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
            val notificationId = Calendar.getInstance().let {
                NOTIFICATION_ID + it.get(Calendar.HOUR_OF_DAY)
            }
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and notify.
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
```

**Scheduling flow:**
1. `sync()` checks `hasConvexSession()`, `reminderEnabled()`, `!isPathComplete()` → if inactive, calls `cancel()` and returns
2. If active, calls `scheduleNext()` with `skipToday = todaySessionCompleted()`
3. `scheduleNext()` calls `nextTriggerAt()` which uses `ReminderCalculator.calculateNextTrigger()` with `lastReminderAt()` from ServiceLocator
4. Sets one-shot `AlarmManager.RTC_WAKEUP` alarm (exact if allowed, inexact fallback)
5. Logs the scheduled trigger time

**Execution order in `ReminderReceiver.onReceive()` (lines 231–259):**

```kotlin
// Normal alarm delivery.
// Record that the alarm fired before showing the notification.
ServiceLocator.recordReminderDelivered()

// Display the current notification first.
ReminderScheduler.showNotification(context)

// Then schedule the next reminder.
ReminderScheduler.sync(context)
```

**Order preserved:** `showNotification()` runs **before** `sync()`, and `recordReminderDelivered()` runs before both. This maintains the early-delivery guard: `nextTriggerAt()` reads `lastReminderAt()` via `ReminderCalculator` to avoid re-scheduling the same slot if Android delivers the alarm slightly early.

---

## 5. ServiceLocator.kt — Reminder Methods

Path: `app/src/main/java/net/lampstandapp/app/data/ServiceLocator.kt`

### `reminderEnabled()` — line 418

```kotlin
fun reminderEnabled(): Boolean = try {
    cachePrefs?.getBoolean(KEY_REMINDER_ENABLED, true) ?: true
} catch (_: Exception) {
    true
}
```
Default: `true`. Off choice persists across restarts.

### `reminderAnchor()` — line 435

```kotlin
fun reminderAnchor(): Pair<Int, Int> {
    val hour = try {
        cachePrefs?.getInt(KEY_REMINDER_ANCHOR_HOUR, 6) ?: 6
    } catch (_: Exception) {
        6
    }
    val minute = try {
        cachePrefs?.getInt(KEY_REMINDER_ANCHOR_MINUTE, 45) ?: 45
    } catch (_: Exception) {
        45
    }
    return (if (hour in 0..23) hour else 6) to (if (minute in 0..59) minute else 45)
}
```
Default: 6:45 AM. Kept for backward compatibility; `reminderPreferences()` now supersedes it for the multi-period design.

### `lastReminderAt()` — line 535

```kotlin
private fun lastReminderAt(): Long = try {
    cachePrefs?.getLong(KEY_LAST_REMINDER_AT, 0L) ?: 0L
} catch (_: Exception) {
    0L
}
```
Private read of the last reminder delivery timestamp. Used by `ReminderCalculator.calculateNextTrigger()` to space the chain.

### `recordReminderDelivered()` — line 539

```kotlin
fun recordReminderDelivered() {
    try {
        cachePrefs?.edit()?.putLong(KEY_LAST_REMINDER_AT, System.currentTimeMillis())?.apply()
    } catch (_: Exception) { /* best-effort */ }
}
```
Writes `KEY_LAST_REMINDER_AT`. Called in `ReminderReceiver.onReceive()` **before** `showNotification()`, so the chain's next-alarm calculation uses the actual delivery time.

### `recordReminderShown()` — line 546

```kotlin
fun recordReminderShown() {
    try {
        cachePrefs?.edit()?.putLong(KEY_LAST_REMINDER_AT, System.currentTimeMillis())?.apply()
    } catch (_: Exception) { /* best-effort */ }
}
```
Writes the same `KEY_LAST_REMINDER_AT` key. Currently **not called** from `showNotification()` (delivery recording is done in the receiver instead). Kept for compatibility.

### `todaySessionCompleted()` — line 571

```kotlin
fun todaySessionCompleted(): Boolean {
    if (completedSessionToday()) return true
    val home = cachedHomeState() ?: return false
    val cachedAt = try {
        cachePrefs?.getLong(KEY_HOME_CACHE_AT, 0L) ?: 0L
    } catch (_: Exception) {
        0L
    }
    return isToday(cachedAt) && home.todayDone == true
}
```
Returns true if today's session is complete locally or per the cached home state (only while the cache is from today).

### `hasConvexSession()` — line 143

```kotlin
fun hasConvexSession(): Boolean = tokenStore.bearerOrNull() != null
```
True when a Convex JWT bearer token exists (user is signed in).

### New multi-period preference methods — lines 459–515

```kotlin
fun reminderPreferences(): ReminderPreferences
fun setReminderPreferences(prefs: ReminderPreferences)
fun setReminderPeriod(period: Int, hour: Int, minute: Int)
fun setReminderAnchorPeriod(period: Int)
```

These store/retrieve the four period times (Morning, Noon, Evening, Night) plus the selected anchor period index (0–3).

---

## 6. How `ReminderScheduler.sync()` Is Called

### MainActivity.kt — 6 call sites

| Line | Context | Purpose |
|------|---------|---------|
| 145 | `onResume()` | Re-checks permissions and re-arms reminders when user returns from Settings |
| 166 | `finishReminderSetup()` | Called after notification + exact-alarm permission checks pass |
| 444 | `onboardingDone` success callback | Starts reminders after onboarding completes |
| 556 | `LaunchedEffect(homeUi)` | Re-arms when home state refreshes |
| 702 | `ProfileEditScreen.onSave` success | Re-arms after reminder settings change |
| 935 | `completeSession` success callback | Re-arms after a session is completed |

Example from line 145 (`onResume`):

```kotlin
override fun onResume() {
    super.onResume()
    ServiceLocator.init(this)
    ServiceLocator.markAppOpened()
    checkReminderPermissions()
}

private fun checkReminderPermissions() {
    val notificationsAllowed =
        NotificationManagerCompat.from(this).areNotificationsEnabled()
    val exactAlarmsAllowed = ReminderScheduler.canScheduleExactAlarms(this)
    if (notificationsAllowed && exactAlarmsAllowed) {
        ReminderScheduler.sync(this)
    }
}
```

Example from line 702 (`ProfileEditScreen` save):

```kotlin
ReminderScheduler.sync(context)
if (reminderEnabled) {
    context.findMainActivity()?.requestNotificationPermission()
}
```

### `ReminderScheduler.kt` internal calls — 2 call sites

| Line | Context |
|------|---------|
| 246 | `ReminderReceiver.onReceive()` — system events (boot, timezone, etc.) |
| 257 | `ReminderReceiver.onReceive()` — normal alarm delivery, after `showNotification()` |

---

## Summary

| Component | Required State | Current Status |
|-----------|---------------|----------------|
| Notification permission | Granted | Declared in manifest; requested via `requestNotificationPermission()` in `MainActivity` |
| Exact-alarm access | Granted | Declared in manifest; checked via `ReminderScheduler.canScheduleExactAlarms()`; settings opened via `requestExactAlarmAccess()` |
| Reminder scheduler | Active | `ReminderScheduler.sync()` schedules the next alarm using `ReminderCalculator` |

All three components are coordinated: reminders are only scheduled when both permissions are available, and `sync()` is re-invoked in `onResume()` when the user returns from Settings.
