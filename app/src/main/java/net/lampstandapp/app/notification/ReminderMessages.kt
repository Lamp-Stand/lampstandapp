package net.lampstandapp.app.notification

/**
 * Two pools of gentle nudges:
 * - [NOT_OPENED]: the app has not been opened today.
 * - [OPENED_NOT_COMPLETED]: the app (or a session) was opened today, but
 *   today's session is still unfinished.
 */
data class ReminderMessage(val title: String, val body: String)

object ReminderMessages {

    private val notOpened = listOf(
        ReminderMessage(
            "God is near",
            "Pause, breathe, and come spend a few minutes with Him."
        ),
        ReminderMessage(
            "Come as you are",
            "No need to have it all together. Just open Lampstand and sit with Him."
        ),
        ReminderMessage(
            "He's waiting for you",
            "Today's session is still open. Let's meet with God together."
        ),
        ReminderMessage(
            "Slow down with Him",
            "Take a few minutes to breathe, reflect, and be still before God."
        ),
        ReminderMessage(
            "Faith, love, communion",
            "Make a little space today to connect with God and His Word."
        ),
        ReminderMessage(
            "A moment of peace",
            "Step away and rest in God's presence for a few minutes."
        ),
        ReminderMessage(
            "You're not alone",
            "Let's walk through today's session together, one quiet minute at a time."
        ),
        ReminderMessage(
            "Come back to stillness",
            "A few minutes with God can reset the whole day."
        ),
        ReminderMessage(
            "Make room for Him",
            "Slow down, meditate, and let today's session draw you closer to God."
        ),
        ReminderMessage(
            "A pause for your soul",
            "Open Lampstand and take a few minutes to meditate on God's love."
        )
    )

    private val openedNotCompleted = listOf(
        ReminderMessage(
            "Your session is unfinished",
            "Let's pick up where you left off and finish together."
        ),
        ReminderMessage(
            "Still time today",
            "You started, but today's session isn't complete. Let's take the next step."
        ),
        ReminderMessage(
            "Let's finish well",
            "You're partway through. A few minutes now can complete it."
        ),
        ReminderMessage(
            "Your session is waiting",
            "Continue where you paused and make room for faith and love."
        ),
        ReminderMessage(
            "One small step",
            "You don't have to start over. Just continue today's session."
        ),
        ReminderMessage(
            "Don't rush past this",
            "You opened the app; let's complete today's session together."
        ),
        ReminderMessage(
            "Today's session is calling",
            "Come back and finish what you started."
        ),
        ReminderMessage(
            "A gentle nudge",
            "Your session is still open. A few quiet minutes with God will finish it."
        ),
        ReminderMessage(
            "One more moment",
            "You're close. Let's complete today's session together."
        ),
        ReminderMessage(
            "Still here for you",
            "You haven't finished today's session. Let's take a few minutes now."
        )
    )

    val notOpenedCount: Int get() = notOpened.size
    val openedNotCompletedCount: Int get() = openedNotCompleted.size

    fun pool(appOpenedToday: Boolean): List<ReminderMessage> =
        if (appOpenedToday) openedNotCompleted else notOpened
}
