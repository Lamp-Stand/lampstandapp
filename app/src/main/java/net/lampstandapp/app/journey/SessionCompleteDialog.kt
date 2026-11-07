package net.lampstandapp.app.journey

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontWeight
import net.lampstandapp.app.data.ServiceLocator
import net.lampstandapp.app.profile.formatReminderTime

/**
 * Shown instead of the "Session unavailable" screen when today's session is
 * already done. The next day stays locked in Convex until its calendar day, so
 * this explains the wait and when the reminder will fire.
 */
@Composable
fun SessionCompleteDialog(
    onDismiss: () -> Unit,
    onSetReminder: (() -> Unit)? = null
) {
    val remindersOn = remember { ServiceLocator.reminderEnabled() }
    val nextAlarmLabel = remember {
        // Tomorrow's earliest phase (today's remaining alarms are skipped).
        val phases = ServiceLocator.reminderPreferences().allPeriodTimes()
        val trigger = net.lampstandapp.app.notification.ReminderCalculator
            .calculateNextPhaseTrigger(
                now = System.currentTimeMillis(),
                phases = phases,
                skipToday = true
            )
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = trigger }
        formatReminderTime(cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Back to Journey", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = if (!remindersOn && onSetReminder != null) {
            {
                TextButton(onClick = onSetReminder) {
                    Text("Set a reminder", fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            null
        },
        title = {
            Text("Today's session is complete", fontWeight = FontWeight.SemiBold)
        },
        text = {
            Text(
                if (remindersOn) {
                    "You've finished today's session. Your next session unlocks tomorrow — meditation reminders will nudge you again from $nextAlarmLabel."
                } else {
                    "You've finished today's session. Your next session unlocks tomorrow. Turn on reminders in Profile so you don't miss it."
                }
            )
        }
    )
}
