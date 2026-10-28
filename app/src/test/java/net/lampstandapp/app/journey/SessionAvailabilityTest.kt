package net.lampstandapp.app.journey

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class SessionAvailabilityTest {

    private val day = 86_400_000L
    private val startedAt = 1_700_000_000_000L

    @Test
    fun completedPathHasNoNextSession() {
        val message = nextSessionAvailabilityMessage(
            nextDayNumber = 8,
            totalDays = 7,
            startedAtMillis = startedAt,
            nowMillis = startedAt + 7 * day,
            locale = Locale.US
        )
        assertEquals("You've completed every session in this path.", message)
    }

    @Test
    fun nextSessionAlreadyUnlocked() {
        val message = nextSessionAvailabilityMessage(
            nextDayNumber = 5,
            totalDays = 7,
            startedAtMillis = startedAt,
            nowMillis = startedAt + 4 * day + 60_000L,
            locale = Locale.US
        )
        assertEquals("Your next session is available now.", message)
    }

    @Test
    fun nextSessionCountsDownInDaysAndHours() {
        val message = nextSessionAvailabilityMessage(
            nextDayNumber = 5,
            totalDays = 7,
            startedAtMillis = startedAt,
            nowMillis = startedAt + 3 * day + 2 * 3_600_000L,
            locale = Locale.US
        )
        assertTrue(message, message.startsWith("Next session available in 22h"))
    }

    @Test
    fun lockedBeforeCalendarUnlock() {
        assertTrue(
            isNextSessionLocked(
                nextDayNumber = 5,
                totalDays = 7,
                startedAtMillis = startedAt,
                nowMillis = startedAt + 3 * day + 2 * 3_600_000L
            )
        )
    }

    @Test
    fun unlockedAfterCalendarDayOrPastPath() {
        assertFalse(
            isNextSessionLocked(
                nextDayNumber = 5,
                totalDays = 7,
                startedAtMillis = startedAt,
                nowMillis = startedAt + 4 * day + 60_000L
            )
        )
        assertFalse(
            isNextSessionLocked(
                nextDayNumber = 8,
                totalDays = 7,
                startedAtMillis = startedAt,
                nowMillis = startedAt + 7 * day
            )
        )
    }

    @Test
    fun missingStartDateIsNotLocked() {
        assertFalse(
            isNextSessionLocked(
                nextDayNumber = 2,
                totalDays = 7,
                startedAtMillis = null,
                nowMillis = startedAt
            )
        )
    }

    @Test
    fun missingStartDateFallsBack() {
        val message = nextSessionAvailabilityMessage(
            nextDayNumber = 2,
            totalDays = 7,
            startedAtMillis = null,
            nowMillis = startedAt,
            locale = Locale.US
        )
        assertEquals("Your next session will be available soon.", message)
    }
}
