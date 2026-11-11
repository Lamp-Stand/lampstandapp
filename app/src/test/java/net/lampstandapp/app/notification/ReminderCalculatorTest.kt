package net.lampstandapp.app.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ReminderCalculatorTest {

    private fun cal(
        year: Int, month: Int, day: Int, hour: Int, minute: Int
    ): Long = Calendar.getInstance().apply {
        set(year, month, day, hour, minute, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    // ---- Phase-based trigger (4 independent daily alarms) ----

    private val defaultPhases = listOf(6 to 45, 12 to 30, 17 to 0, 21 to 0)

    @Test
    fun phase_earliestAfterNowToday() {
        val now = cal(2026, Calendar.NOVEMBER, 7, 7, 0)
        val trigger = ReminderCalculator.calculateNextPhaseTrigger(now, defaultPhases)
        assertEquals(cal(2026, Calendar.NOVEMBER, 7, 12, 30), trigger)
    }

    @Test
    fun phase_nowExactlyAtPhase_skipsToNextPhase() {
        val now = cal(2026, Calendar.NOVEMBER, 7, 12, 30)
        val trigger = ReminderCalculator.calculateNextPhaseTrigger(now, defaultPhases)
        assertEquals(cal(2026, Calendar.NOVEMBER, 7, 17, 0), trigger)
    }

    @Test
    fun phase_noneLeftToday_tomorrowEarliest() {
        val now = cal(2026, Calendar.NOVEMBER, 7, 22, 0)
        val trigger = ReminderCalculator.calculateNextPhaseTrigger(now, defaultPhases)
        assertEquals(cal(2026, Calendar.NOVEMBER, 8, 6, 45), trigger)
    }

    @Test
    fun phase_skipToday_tomorrowEarliest() {
        val now = cal(2026, Calendar.NOVEMBER, 7, 7, 0)
        val trigger = ReminderCalculator.calculateNextPhaseTrigger(
            now, defaultPhases, skipToday = true
        )
        assertEquals(cal(2026, Calendar.NOVEMBER, 8, 6, 45), trigger)
    }

    @Test
    fun phase_unsortedInput_orderDoesNotMatter() {
        val now = cal(2026, Calendar.NOVEMBER, 7, 7, 0)
        val shuffled = listOf(21 to 0, 6 to 45, 17 to 0, 12 to 30)
        val trigger = ReminderCalculator.calculateNextPhaseTrigger(now, shuffled)
        assertEquals(cal(2026, Calendar.NOVEMBER, 7, 12, 30), trigger)
    }

    @Test
    fun phase_outOfWindowInput_clampedToFiveAm() {
        // Defensive: a 3:00 AM phase (invalid window) clamps to 5:00 AM.
        val now = cal(2026, Calendar.NOVEMBER, 7, 2, 0)
        val trigger = ReminderCalculator.calculateNextPhaseTrigger(now, listOf(3 to 0))
        assertEquals(cal(2026, Calendar.NOVEMBER, 7, 5, 0), trigger)
    }

    @Test
    fun phase_lastPhaseAt11Pm_allowed() {
        val now = cal(2026, Calendar.NOVEMBER, 7, 22, 0)
        val trigger = ReminderCalculator.calculateNextPhaseTrigger(now, listOf(23 to 0))
        assertEquals(cal(2026, Calendar.NOVEMBER, 7, 23, 0), trigger)
    }

    @Test
    fun phase_emptyPhases_fallsForwardOneDay() {
        val now = cal(2026, Calendar.NOVEMBER, 7, 14, 0)
        val trigger = ReminderCalculator.calculateNextPhaseTrigger(now, emptyList())
        assertEquals(now + 24L * 60 * 60 * 1000, trigger)
    }

    @Test
    fun phase_spansMonthEnd() {
        val now = cal(2026, Calendar.NOVEMBER, 30, 22, 0)
        val trigger = ReminderCalculator.calculateNextPhaseTrigger(now, defaultPhases)
        assertEquals(cal(2026, Calendar.DECEMBER, 1, 6, 45), trigger)
    }

    @Test
    fun phase_resultAlwaysAfterNowForFuturePhases() {
        val now = cal(2026, Calendar.NOVEMBER, 7, 14, 0)
        val trigger = ReminderCalculator.calculateNextPhaseTrigger(now, defaultPhases)
        assertTrue("trigger=$trigger must be after now=$now", trigger > now)
    }
}
