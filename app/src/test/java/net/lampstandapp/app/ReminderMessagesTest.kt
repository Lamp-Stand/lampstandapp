package net.lampstandapp.app

import net.lampstandapp.app.notification.ReminderMessages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderMessagesTest {

    @Test
    fun poolsContainTenMessagesEach() {
        assertEquals(10, ReminderMessages.notOpenedCount)
        assertEquals(10, ReminderMessages.openedNotCompletedCount)
    }

    @Test
    fun messagesAlwaysHaveTitleAndBody() {
        val pools = listOf(
            ReminderMessages.pool(appOpenedToday = false),
            ReminderMessages.pool(appOpenedToday = true)
        )
        pools.flatten().forEach { message ->
            assertTrue(message.title.isNotBlank())
            assertTrue(message.body.isNotBlank())
        }
    }

    @Test
    fun poolSwitchesOnAppOpenedToday() {
        val notOpened = ReminderMessages.pool(appOpenedToday = false)
        val opened = ReminderMessages.pool(appOpenedToday = true)
        assertEquals(10, notOpened.size)
        assertEquals(10, opened.size)
        assertTrue(notOpened.none { it in opened })
    }
}
