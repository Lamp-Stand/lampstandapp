package net.lampstandapp.app.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderPreferencesTest {

    @Test
    fun defaults_matchSpec() {
        val p = ReminderPreferences()
        assertEquals(6, p.morningHour)
        assertEquals(45, p.morningMinute)
        assertEquals(12, p.noonHour)
        assertEquals(30, p.noonMinute)
        assertEquals(17, p.eveningHour)
        assertEquals(0, p.eveningMinute)
        assertEquals(21, p.nightHour)
        assertEquals(0, p.nightMinute)
        assertEquals(0, p.anchorPeriod)
    }

    @Test
    fun anchorTime_defaultsToMorning() {
        val p = ReminderPreferences()
        assertEquals(6 to 45, p.anchorTime())
    }

    @Test
    fun anchorTime_noonPeriod() {
        val p = ReminderPreferences(anchorPeriod = 1)
        assertEquals(12 to 30, p.anchorTime())
    }

    @Test
    fun anchorTime_eveningPeriod() {
        val p = ReminderPreferences(anchorPeriod = 2)
        assertEquals(17 to 0, p.anchorTime())
    }

    @Test
    fun anchorTime_nightPeriod() {
        val p = ReminderPreferences(anchorPeriod = 3)
        assertEquals(21 to 0, p.anchorTime())
    }

    @Test
    fun hourFor_unknownPeriod_fallsBackToMorning() {
        val p = ReminderPreferences()
        assertEquals(6, p.hourFor(99))
        assertEquals(45, p.minuteFor(99))
    }

    @Test
    fun hourAndMinuteFor_eachPeriod() {
        val p = ReminderPreferences()
        assertEquals(6, p.hourFor(0)); assertEquals(45, p.minuteFor(0))
        assertEquals(12, p.hourFor(1)); assertEquals(30, p.minuteFor(1))
        assertEquals(17, p.hourFor(2)); assertEquals(0, p.minuteFor(2))
        assertEquals(21, p.hourFor(3)); assertEquals(0, p.minuteFor(3))
    }

    @Test
    fun customTimes_flowsThroughAnchorTime() {
        val p = ReminderPreferences(
            morningHour = 7, morningMinute = 15,
            anchorPeriod = 0
        )
        assertEquals(7 to 15, p.anchorTime())
    }

    @Test
    fun allPeriodTimes_returnsFourInOrder() {
        val p = ReminderPreferences()
        assertEquals(
            listOf(6 to 45, 12 to 30, 17 to 0, 21 to 0),
            p.allPeriodTimes()
        )
    }

    // ---- Time windows (5:00 AM – 11:00 PM, option A) ----

    @Test
    fun defaultTimes_areAllInsideTheirWindows() {
        val p = ReminderPreferences()
        (0..3).forEach { period ->
            assertTrue(
                "default for period $period must be in window",
                ReminderPreferences.isValidTime(period, p.hourFor(period), p.minuteFor(period))
            )
        }
    }

    @Test
    fun morningWindow_boundaries() {
        assertTrue(ReminderPreferences.isValidTime(0, 5, 0))     // opens 5:00 AM
        assertTrue(ReminderPreferences.isValidTime(0, 11, 59))   // closes 11:59 AM
        assertFalse(ReminderPreferences.isValidTime(0, 4, 59))   // before window
        assertFalse(ReminderPreferences.isValidTime(0, 12, 0))   // that's Noon's
    }

    @Test
    fun noonWindow_boundaries() {
        assertTrue(ReminderPreferences.isValidTime(1, 12, 0))
        assertTrue(ReminderPreferences.isValidTime(1, 16, 59))
        assertFalse(ReminderPreferences.isValidTime(1, 11, 59))
        assertFalse(ReminderPreferences.isValidTime(1, 17, 0))
    }

    @Test
    fun eveningWindow_boundaries() {
        assertTrue(ReminderPreferences.isValidTime(2, 17, 0))
        assertTrue(ReminderPreferences.isValidTime(2, 20, 59))
        assertFalse(ReminderPreferences.isValidTime(2, 16, 59))
        assertFalse(ReminderPreferences.isValidTime(2, 21, 0))
    }

    @Test
    fun nightWindow_boundaries() {
        assertTrue(ReminderPreferences.isValidTime(3, 21, 0))
        assertTrue(ReminderPreferences.isValidTime(3, 23, 0))    // 11:00 PM inclusive
        assertFalse(ReminderPreferences.isValidTime(3, 23, 1))   // past 11 PM
        assertFalse(ReminderPreferences.isValidTime(3, 20, 59))
    }

    @Test
    fun amTimeRejectedInPmPeriods_andViceVersa() {
        // The bug the user reported: AM in Evening / PM in Morning.
        assertFalse(ReminderPreferences.isValidTime(2, 9, 0))    // 9 AM in Evening
        assertFalse(ReminderPreferences.isValidTime(0, 19, 0))   // 7 PM in Morning
        assertFalse(ReminderPreferences.isValidTime(3, 14, 0))   // 2 PM in Night
        assertFalse(ReminderPreferences.isValidTime(1, 6, 45))   // 6:45 AM in Noon
    }

    @Test
    fun invalidClockValues_rejected() {
        assertFalse(ReminderPreferences.isValidTime(0, 24, 0))
        assertFalse(ReminderPreferences.isValidTime(0, -1, 0))
        assertFalse(ReminderPreferences.isValidTime(0, 6, 60))
        assertFalse(ReminderPreferences.isValidTime(0, 6, -1))
    }

    @Test
    fun windows_coverFullRange_withoutGapsOrOverlaps() {
        // Contiguous 5:00 (300) → 23:00 (1380), each minute in exactly one window.
        var covered = 0
        ReminderPreferences.WINDOWS_MINUTES.forEach { covered += it.count() }
        assertEquals(23 * 60 - 5 * 60 + 1, covered) // 300..1380 inclusive
        val sorted = ReminderPreferences.WINDOWS_MINUTES.sortedBy { it.first }
        for (i in 0 until sorted.lastIndex) {
            assertEquals(
                "window ${i + 1} must start right after window $i ends",
                sorted[i].last + 1,
                sorted[i + 1].first
            )
        }
    }
}
