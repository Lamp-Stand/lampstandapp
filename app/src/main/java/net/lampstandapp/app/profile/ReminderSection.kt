package net.lampstandapp.app.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Blue = Color(0xFF335E78)
private val BodyGrey = Color(0xFF5A5A5A)

data class ReminderSlot(
    val title: String,
    val subtitle: String,
    val glyph: String
)

val DefaultReminderSlots = listOf(
    ReminderSlot("Early Morning", "Before the day begins", "◑"),
    ReminderSlot("Afternoon", "A midday pause", "☀"),
    ReminderSlot("Evening", "When things slow down", "◐"),
    ReminderSlot("Before Bed", "A quiet close to the day", "☾")
)

/** Hours between meditation reminder rings while a session is pending. */
private const val CHAIN_STEP_HOURS = 5

/**
 * The reminder chain from an anchor time: anchor, +5h, +10h, +15h.
 * The 6:45 AM default yields 6:45, 11:45, 16:45, 21:45.
 */
fun chainReminderTimes(anchorHour: Int, anchorMinute: Int): Map<Int, Pair<Int, Int>> =
    (0 until 4).associate { step ->
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, anchorHour)
            set(java.util.Calendar.MINUTE, anchorMinute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            add(java.util.Calendar.HOUR_OF_DAY, CHAIN_STEP_HOURS * step)
        }
        step to (calendar.get(java.util.Calendar.HOUR_OF_DAY) to calendar.get(java.util.Calendar.MINUTE))
    }

/** Default chain: the benchmark 6:45 AM anchor. */
fun defaultReminderTimes(): Map<Int, Pair<Int, Int>> = chainReminderTimes(6, 45)

fun formatReminderTime(hour: Int, minute: Int): String {
    val suffix = if (hour < 12) "AM" else "PM"
    val h12 = if (hour % 12 == 0) 12 else hour % 12
    return "%02d:%02d %s".format(h12, minute, suffix)
}

/**
 * Meditation-time cards with user-editable times.
 *
 * Tapping a card selects the slot; tapping the time chip opens a
 * time picker scoped to that slot. [onTimePicked] fires after a
 * successful pick so the host can confirm it (e.g. snackbar).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderTimeCards(
    times: Map<Int, Pair<Int, Int>>,
    selectedSlot: Int?,
    onSlotSelect: (Int) -> Unit,
    onTimeChange: (slot: Int, hour: Int, minute: Int) -> Unit,
    onTimePicked: (slotTitle: String, timeLabel: String) -> Unit,
    showHint: Boolean = true
) {
    var pickerSlot by remember { mutableStateOf<Int?>(null) }

    if (showHint) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = "◷", fontSize = 14.sp, color = Blue)
            Text(
                text = "Tap the time to set your own",
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic,
                color = BodyGrey,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DefaultReminderSlots.forEachIndexed { index, slot ->
            val time = times[index] ?: (12 to 0)
            val selected = selectedSlot == index
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, Color(0xFFB3B3B3), RoundedCornerShape(6.dp))
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFFCFCFC))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSlotSelect(index) }
                    )
                    .padding(14.dp)
            ) {
                Text(text = slot.glyph, fontSize = 24.sp, color = Blue)
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = slot.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 20.sp,
                        color = Blue
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .border(1.dp, Blue, RoundedCornerShape(6.dp))
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { pickerSlot = index }
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(text = "◷", fontSize = 13.sp, color = Blue)
                        Text(
                            text = formatReminderTime(time.first, time.second),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Blue
                        )
                    }
                    Text(
                        text = slot.subtitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 17.sp,
                        color = BodyGrey
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (selected) Blue else Color.Transparent)
                        .border(1.5.dp, Blue, CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSlotSelect(index) }
                        )
                ) {
                    if (selected) {
                        Text(
                            text = "✓",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    pickerSlot?.let { slotIndex ->
        val initial = times[slotIndex] ?: (12 to 0)
        val pickerState = rememberTimePickerState(
            initialHour = initial.first,
            initialMinute = initial.second,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { pickerSlot = null },
            title = {
                Text(
                    text = "${DefaultReminderSlots[slotIndex].title} meditation time",
                    fontWeight = FontWeight.SemiBold,
                    color = Blue
                )
            },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTimeChange(slotIndex, pickerState.hour, pickerState.minute)
                        onTimePicked(
                            DefaultReminderSlots[slotIndex].title,
                            formatReminderTime(pickerState.hour, pickerState.minute)
                        )
                        pickerSlot = null
                    }
                ) { Text("Set time", color = Blue) }
            },
            dismissButton = {
                TextButton(onClick = { pickerSlot = null }) {
                    Text("Cancel", color = BodyGrey)
                }
            }
        )
    }
}
