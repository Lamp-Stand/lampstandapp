package net.lampstandapp.app.notification

/**
 * User-configurable reminder periods and the selected anchor.
 * Each period is an INDEPENDENT daily alarm with its own hour/minute,
 * locked to a fixed time-of-day window so labels always match the clock:
 *
 *   Morning  5:00 AM – 11:59 AM
 *   Noon     12:00 PM – 4:59 PM
 *   Evening  5:00 PM – 8:59 PM
 *   Night    9:00 PM – 11:00 PM
 *
 * All four windows live inside 5:00 AM – 11:00 PM. Period names are fixed
 * (Morning, Noon, Evening, Night); only the times within each window change.
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

    /** All four period times as (hour, minute) pairs, index = period. */
    fun allPeriodTimes(): List<Pair<Int, Int>> =
        (0..3).map { hourFor(it) to minuteFor(it) }

    companion object {
        /**
         * Allowed minute-of-day range per period. Contiguous, no gaps or
         * overlaps, all inside 5:00 AM (300) – 11:00 PM (1380, inclusive).
         */
        val WINDOWS_MINUTES: List<IntRange> = listOf(
            5 * 60..11 * 60 + 59,   // Morning   5:00 AM – 11:59 AM
            12 * 60..16 * 60 + 59,  // Noon     12:00 PM –  4:59 PM
            17 * 60..20 * 60 + 59,  // Evening   5:00 PM –  8:59 PM
            21 * 60..23 * 60        // Night     9:00 PM – 11:00 PM (23:00 inclusive)
        )

        val WINDOW_LABELS = listOf(
            "5:00 AM – 11:59 AM",
            "12:00 PM – 4:59 PM",
            "5:00 PM – 8:59 PM",
            "9:00 PM – 11:00 PM"
        )

        fun windowFor(period: Int): IntRange =
            WINDOWS_MINUTES.getOrElse(period) { WINDOWS_MINUTES[0] }

        fun windowLabel(period: Int): String =
            WINDOW_LABELS.getOrElse(period) { WINDOW_LABELS[0] }

        /** True when (hour, minute) falls inside the period's window. */
        fun isValidTime(period: Int, hour: Int, minute: Int): Boolean {
            if (hour !in 0..23 || minute !in 0..59) return false
            return (hour * 60 + minute) in windowFor(period)
        }
    }
}
