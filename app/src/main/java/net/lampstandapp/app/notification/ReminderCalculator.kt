package net.lampstandapp.app.notification

import java.util.Calendar

/** Quiet hours: earliest reminder is 5:00 AM. */
private const val EARLIEST_HOUR = 5

/** Quiet hours: latest reminder is 11:00 PM (23:00). */
private const val LATEST_HOUR = 23
private const val LATEST_MINUTE = 0

/** One day in milliseconds. */
private const val DAY_MS = 24L * 60 * 60 * 1000

object ReminderCalculator {

    /**
     * Next trigger from the four INDEPENDENT daily phase times (option A):
     * the earliest phase time strictly after [now] today, otherwise the
     * earliest phase time tomorrow.
     *
     * With [skipToday] (today's session completed), tomorrow's earliest
     * phase is returned regardless of today's remaining phases.
     *
     * [phases] is a list of (hour, minute) pairs; order does not matter.
     * Times are expected to already be inside their period windows
     * (5:00 AM – 11:00 PM); quiet-hours clamping is applied defensively.
     */
    fun calculateNextPhaseTrigger(
        now: Long,
        phases: List<Pair<Int, Int>>,
        skipToday: Boolean = false
    ): Long {
        if (phases.isEmpty()) return now + DAY_MS
        val cal = Calendar.getInstance()

        if (!skipToday) {
            var best = Long.MAX_VALUE
            for ((hour, minute) in phases) {
                cal.timeInMillis = now
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val candidate = cal.timeInMillis
                if (candidate > now && candidate < best) best = candidate
            }
            if (best != Long.MAX_VALUE) return clampToQuietHours(best)
        }

        // Nothing left today (or skipToday): earliest phase tomorrow.
        var best = Long.MAX_VALUE
        for ((hour, minute) in phases) {
            cal.timeInMillis = now
            cal.add(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            if (cal.timeInMillis < best) best = cal.timeInMillis
        }
        return clampToQuietHours(best)
    }

    /**
     * Defers to 5:00 AM the same day if [candidate] is before 5:00 AM,
     * or to 5:00 AM the next day if it is after 11:00 PM.
     */
    private fun clampToQuietHours(candidate: Long): Long {
        val cal = Calendar.getInstance()

        val startOfDay = Calendar.getInstance()
        startOfDay.timeInMillis = candidate
        startOfDay.set(Calendar.HOUR_OF_DAY, EARLIEST_HOUR)
        startOfDay.set(Calendar.MINUTE, 0)
        startOfDay.set(Calendar.SECOND, 0)
        startOfDay.set(Calendar.MILLISECOND, 0)

        val endOfDay = Calendar.getInstance()
        endOfDay.timeInMillis = candidate
        endOfDay.set(Calendar.HOUR_OF_DAY, LATEST_HOUR)
        endOfDay.set(Calendar.MINUTE, LATEST_MINUTE)
        endOfDay.set(Calendar.SECOND, 0)
        endOfDay.set(Calendar.MILLISECOND, 0)

        return when {
            candidate < startOfDay.timeInMillis -> startOfDay.timeInMillis
            candidate > endOfDay.timeInMillis -> {
                cal.timeInMillis = endOfDay.timeInMillis
                cal.add(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, EARLIEST_HOUR)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            else -> candidate
        }
    }
}
