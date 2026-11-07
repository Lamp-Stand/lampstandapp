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

    private fun hourOf(millis: Long): Int =
        Calendar.getInstance().apply { timeInMillis = millis }
            .get(Calendar.HOUR_OF_DAY)

    private fun minuteOf(millis: Long): Int =
        Calendar.getInstance().apply { timeInMillis = millis }
            .get(Calendar.MINUTE)

    private fun dayOf(millis: Long): Int =
        Calendar.getInstance().apply { timeInMillis = millis }
            .get(Calendar.DAY_OF_MONTH)

    // ---- First trigger (lastDeliveredAt == null) ----

    @Test
    fun firstTrigger_anchorInFuture_isTodayAtAnchor() {
        val now = cal(2026, Calendar.NOVEMBER, 7, 6, 0) // 6:00 AM
        val trigger = ReminderCalculator.calculateNextTrigger(
            now = now, lastDeliveredAt = null,
            anchorHour = 6, anchorMinute = 45
        )
        assertEquals(cal(2026, Calendar.NOVEMBER, 7, 6, 45), trigger)
    }

    @Test
    fun firstTrigger_anchorAlreadyPassed_isTomorrowAtAnchor() {
        val now = cal(2026, Calendar.NOVEMBER, 7, 8, 0) // 8:00 AM
        val trigger = ReminderCalculator.calculateNextTrigger(
            now = now, lastDeliveredAt = null,
            anchorHour = 6, anchorMinute = 45
        )
        assertEquals(cal(2026, Calendar.NOVEMBER, 8, 6, 45), trigger)
    }

    @Test
    fun firstTrigger_anchorExactlyNow_isTomorrowAtAnchor() {
        val now = cal(2026, Calendar.NOVEMBER, 7, 6, 45)
        val trigger = ReminderCalculator.calculateNextTrigger(
            now = now, lastDeliveredAt = null,
            anchorHour = 6, anchorMinute = 45
        )
        assertEquals(cal(2026, Calendar.NOVEMBER, 8, 6, 45), trigger)
    }

    // ---- Chain: lastDeliveredAt + 5h ----

    @Test
    fun chain_deliveredAtNoon_nextIs5PmSameDay() {
        val now = cal(2026, Calendar.NOVEMBER, 7, 12, 30)
        val last = cal(2026, Calendar.NOVEMBER, 7, 12, 30)
        val trigger = ReminderCalculator.calculateNextTrigger(
            now = now, lastDeliveredAt = last,
            anchorHour = 6, anchorMinute = 45
        )
        assertEquals(cal(2026, Calendar.NOVEMBER, 7, 17, 30), trigger)
    }

    @Test
    fun chain_deliveredAt5Pm_nextIs10PmSameDay() {
        val now = cal(2026, Calendar.NOVEMBER, 7, 17, 0)
        val last = cal(2026, Calendar.NOVEMBER, 7, 17, 0)
        val trigger = ReminderCalculator.calculateNextTrigger(
            now = now, lastDeliveredAt = last,
            anchorHour = 6, anchorMinute = 45
        )
        assertEquals(cal(2026, Calendar.NOVEMBER, 7, 22, 0), trigger)
    }

    // ---- Quiet hours: before 5:00 AM ----

    @Test
    fun quietHours_deliveredAt2Am_defersTo5AmSameDay() {
        val now = cal(2026, Calendar.NOVEMBER, 7, 2, 0)
        val last = cal(2026, Calendar.NOVEMBER, 7, 0, 0) // midnight
        val trigger = ReminderCalculator.calculateNextTrigger(
            now = now, lastDeliveredAt = last,
            anchorHour = 6, anchorMinute = 45
        )
        assertEquals(cal(2026, Calendar.NOVEMBER, 7, 5, 0), trigger)
    }

    @Test
    fun quietHours_exactlyAt5Am_isAllowed() {
        val now = cal(2026, Calendar.NOVEMBER, 7, 4, 0)
        val last = cal(2026, Calendar.NOVEMBER, 7, 0, 0) // midnight today
        val trigger = ReminderCalculator.calculateNextTrigger(
            now = now, lastDeliveredAt = last,
            anchorHour = 6, anchorMinute = 45
        )
        // last + 5h = 5:00 AM exactly — not before start of day, so allowed
        assertEquals(cal(2026, Calendar.NOVEMBER, 7, 5, 0), trigger)
    }

    // ---- Quiet hours: after 10:50 PM ----

    @Test
    fun quietHours_deliveredAt11Pm_defersTo5AmNextDay() {
        val now = cal(2026, Calendar.NOVEMBER, 7, 23, 0)
        val last = cal(2026, Calendar.NOVEMBER, 7, 23, 0)
        val trigger = ReminderCalculator.calculateNextTrigger(
            now = now, lastDeliveredAt = last,
            anchorHour = 6, anchorMinute = 45
        )
        assertEquals(cal(2026, Calendar.NOVEMBER, 8, 5, 0), trigger)
    }

    @Test
    fun quietHours_exactlyAt1050Pm_isAllowed() {
        val now = cal(2026, Calendar.NOVEMBER, 7, 22, 40)
        val last = cal(2026, Calendar.NOVEMBER, 7, 17, 50)
        val trigger = ReminderCalculator.calculateNextTrigger(
            now = now, lastDeliveredAt = last,
            anchorHour = 6, anchorMinute = 45
        )
        // last + 5h = 22:50 exactly — not after end of day, so allowed
        assertEquals(cal(2026, Calendar.NOVEMBER, 7, 22, 50), trigger)
    }

    @Test
    fun quietHours_deliveredAt1051Pm_defersTo5AmNextDay() {
        val now = cal(2026, Calendar.NOVEMBER, 7, 22, 51)
        val last = cal(2026, Calendar.NOVEMBER, 7, 22, 51)
        val trigger = ReminderCalculator.calculateNextTrigger(
            now = now, lastDeliveredAt = last,
            anchorHour = 6, anchorMinute = 45
        )
        assertEquals(cal(2026, Calendar.NOVEMBER, 8, 5, 0), trigger)
    }

    // ---- Deferred reminders do not cluster ----

    @Test
    fun deferredReminder_measuredFromActualDeliveryTime() {
        // Delivered at 11 PM → deferred to 5 AM next day.
        // Next chain step is 5 AM + 5h = 10 AM, not 11 PM + 5h = 4 AM.
        val last = cal(2026, Calendar.NOVEMBER, 7, 23, 0)
        val deferredTo = cal(2026, Calendar.NOVEMBER, 8, 5, 0)
        val next = ReminderCalculator.calculateNextTrigger(
            now = deferredTo, lastDeliveredAt = last,
            anchorHour = 6, anchorMinute = 45
        )
        // last + 5h = 4 AM (before quiet hours) → deferred to 5 AM same day
        assertEquals(cal(2026, Calendar.NOVEMBER, 8, 5, 0), next)
    }

    @Test
    fun chainFromDeferred5Am_nextIs10AmSameDay() {
        // After a 5 AM deferred delivery, next is +5h = 10 AM (within quiet hours)
        val last = cal(2026, Calendar.NOVEMBER, 8, 5, 0)
        val next = ReminderCalculator.calculateNextTrigger(
            now = cal(2026, Calendar.NOVEMBER, 8, 5, 1),
            lastDeliveredAt = last,
            anchorHour = 6, anchorMinute = 45
        )
        assertEquals(cal(2026, Calendar.NOVEMBER, 8, 10, 0), next)
    }

    // ---- Month/year boundaries ----

    @Test
    fun chain_spansAcrossMonthEnd() {
        val last = cal(2026, Calendar.NOVEMBER, 30, 20, 0)
        val next = ReminderCalculator.calculateNextTrigger(
            now = cal(2026, Calendar.NOVEMBER, 30, 20, 0),
            lastDeliveredAt = last,
            anchorHour = 6, anchorMinute = 45
        )
        // last + 5h = Dec 1 1:00 AM → before 5 AM → Dec 1 5:00 AM
        assertEquals(cal(2026, Calendar.DECEMBER, 1, 5, 0), next)
    }

    // ---- Result sanity: always in the future relative to now ----

    @Test
    fun result_isAfterNow_forRecentDeliveries() {
        val now = cal(2026, Calendar.NOVEMBER, 7, 14, 0)
        val cases = listOf(
            cal(2026, Calendar.NOVEMBER, 7, 10, 0), // +5h = 15:00 > now
            cal(2026, Calendar.NOVEMBER, 7, 23, 30), // +5h → deferred to 5 AM tomorrow
            null // anchor tomorrow > now
        )
        cases.forEach { last ->
            val trigger = ReminderCalculator.calculateNextTrigger(
                now = now, lastDeliveredAt = last,
                anchorHour = 6, anchorMinute = 45
            )
            assertTrue(
                "trigger=$trigger must be after now=$now (last=$last)",
                trigger > now
            )
        }
    }

    @Test
    fun result_staleDelivery_mayProducePastTrigger() {
        // KNOWN BEHAVIOUR: a stale lastDeliveredAt (e.g. app closed for a day)
        // yields last + 5h in the past; quiet-hours deferral may still leave it
        // before `now`. The alarm then fires immediately and sync() re-arms.
        val now = cal(2026, Calendar.NOVEMBER, 7, 14, 0) // 2 PM
        val staleLast = cal(2026, Calendar.NOVEMBER, 6, 22, 0) // yesterday 10 PM
        val trigger = ReminderCalculator.calculateNextTrigger(
            now = now, lastDeliveredAt = staleLast,
            anchorHour = 6, anchorMinute = 45
        )
        // last + 5h = 3 AM → deferred to 5 AM today, which is before now
        assertEquals(cal(2026, Calendar.NOVEMBER, 7, 5, 0), trigger)
        assertTrue("trigger is in the past relative to now", trigger < now)
    }

    @Test
    fun result_alwaysWithinQuietHours() {
        val now = cal(2026, Calendar.NOVEMBER, 7, 14, 0)
        val cases = listOf(
            cal(2026, Calendar.NOVEMBER, 7, 1, 0),
            cal(2026, Calendar.NOVEMBER, 7, 3, 30),
            cal(2026, Calendar.NOVEMBER, 7, 23, 0),
            null
        )
        cases.forEach { last ->
            val trigger = ReminderCalculator.calculateNextTrigger(
                now = now, lastDeliveredAt = last,
                anchorHour = 6, anchorMinute = 45
            )
            val h = hourOf(trigger)
            val m = minuteOf(trigger)
            val afterEarliest = h > 5 || (h == 5 && m >= 0)
            val beforeLatest = h < 23 || (h == 23 && m == 0)
            assertTrue(
                "trigger h=$h:$m must be within 5:00–23:00 (last=$last)",
                afterEarliest && beforeLatest
            )
        }
    }

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
}
