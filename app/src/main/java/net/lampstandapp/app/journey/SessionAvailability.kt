package net.lampstandapp.app.journey

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val DAY_MS = 86_400_000L

/**
 * Convex unlocks day N at startedAt + (N - 1) days
 * (paths:getPathOverview / getSessionContent use floor(elapsed / 1 day)).
 */
internal fun nextSessionAvailabilityMessage(
    nextDayNumber: Int,
    totalDays: Int,
    startedAtMillis: Long?,
    nowMillis: Long = System.currentTimeMillis(),
    locale: Locale = Locale.getDefault()
): String {
    if (nextDayNumber > totalDays) {
        return "You've completed every session in this path."
    }
    if (startedAtMillis == null || startedAtMillis <= 0L) {
        return "Your next session will be available soon."
    }
    val unlockAt = startedAtMillis + (nextDayNumber - 1) * DAY_MS
    if (unlockAt <= nowMillis) {
        return "Your next session is available now."
    }
    val remainingMs = unlockAt - nowMillis
    val totalMinutes = (remainingMs + 59_999L) / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val duration = when {
        hours >= 24 -> {
            val days = hours / 24
            val restHours = hours % 24
            if (restHours > 0) "${days}d ${restHours}h" else "${days}d"
        }
        hours > 0 -> if (minutes > 0) "${hours}h ${minutes}m" else "${hours}h"
        else -> "${minutes.coerceAtLeast(1)}m"
    }
    val unlockLabel = SimpleDateFormat("MMM d 'at' h:mm a", locale).format(Date(unlockAt))
    return "Next session available in $duration ($unlockLabel)."
}
