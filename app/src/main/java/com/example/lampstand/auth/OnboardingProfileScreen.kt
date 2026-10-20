package com.example.lampstand.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lampstand.R
import com.example.lampstand.ui.components.CountryCodePicker
import com.example.lampstand.ui.components.LampButton
import com.example.lampstand.ui.components.defaultCountry
import com.example.lampstand.ui.theme.LampStandTheme

private val BodyGrey = Color(0xFF5A5A5A)
private val FieldBorder = Color(0xFFD0D7DC)
private val LampBlue = com.example.lampstand.ui.components.LampBlue

// ── Backend contract (mirrors convex/users.ts, no backend changes) ──
// VALID_SPIRITUAL_SEASONS = uncertain_future | pressure_stress |
//   struggling_to_trust | waiting | distant_from_god | none
// completeOnboarding { spiritualSeason*, studyTimePreference?,
//   contactPreference?, phoneNumber?, timezone? }

data class OnboardingData(
    val firstName: String,
    val lastName: String,
    /** E.164-ish, no spaces, e.g. +2348012345678. Null when blank. */
    val phoneE164: String?,
    val spiritualSeason: String,
    val studyTimePreference: String,
    /** Local-only clock time for the mobile notification; backend still gets only the slot above. */
    val reminderHour: Int,
    val reminderMinute: Int,
    val contactPreference: String = "email",
    val timezone: String
)

/** Web parity: `${dial}${digits-only}`, null when blank. */
fun buildPhoneE164(dial: String, national: String): String? {
    val digits = national.filter { it.isDigit() }
    if (digits.isBlank()) return null
    return "${dial.trim()}$digits"
}

private data class SeasonOption(val value: String, val label: String, val comingSoon: Boolean = false)
private data class TimeOption(
    val value: String,
    val label: String,
    val subtitle: String,
    val range: String,
    val glyph: String,
    val defaultHour: Int,
    val defaultMinute: Int
)

private val Seasons = listOf(
    SeasonOption("struggling_to_trust", "Struggling to trust God"),
    SeasonOption("uncertain_future", "Feeling uncertain about the future", comingSoon = true),
    SeasonOption("pressure_stress", "Living under pressure or stress", comingSoon = true),
    SeasonOption("waiting", "Waiting for something that hasn't come", comingSoon = true),
    SeasonOption("distant_from_god", "Feeling distant from God", comingSoon = true),
    SeasonOption("none", "None of these feel quite right", comingSoon = true)
)
private val Times = listOf(
    TimeOption("early_morning", "Early Morning", "Before the day begins", "5:00 – 9:00 AM", "◑", 6, 45),
    TimeOption("afternoon", "Afternoon", "A midday pause", "12:00 – 2:00 PM", "☀", 12, 30),
    TimeOption("evening", "Evening", "When things slow down", "5:00 – 8:00 PM", "◐", 18, 30),
    TimeOption("before_bed", "Before Bed", "A quiet close to the day", "9:00 – 11:00 PM", "☾", 22, 15)
)

private fun formatClock(hour: Int, minute: Int): String {
    val suffix = if (hour < 12) "AM" else "PM"
    val h12 = if (hour % 12 == 0) 12 else hour % 12
    return "%02d:%02d %s".format(h12, minute, suffix)
}

@Composable
private fun FieldLabel(text: String) {
    Text(text = text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp, color = BodyGrey)
}

@Composable
private fun RadioRow(
    label: String,
    subtitle: String? = null,
    glyph: String? = null,
    selected: Boolean,
    enabled: Boolean = true,
    trailingSoon: Boolean = false,
    onClick: () -> Unit
) {
    val border = if (!enabled) Color(0xFFE0E0E0) else if (selected) LampBlue else Color(0xFFB3B3B3)
    val bg = if (!enabled) Color(0xFFF5F5F5) else if (selected) Color(0xFFEFF6FA) else Color.White
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, border, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        if (glyph != null) Text(text = glyph, fontSize = 22.sp, color = LampBlue)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (enabled) LampBlue else Color(0xFF999999)
            )
            if (subtitle != null) Text(text = subtitle, fontSize = 13.sp, color = BodyGrey)
        }
        if (trailingSoon) {
            Text(text = "soon", fontSize = 11.sp, color = Color(0xFFBBBBBB))
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (selected) LampBlue else Color.Transparent)
                    .border(1.5.dp, if (enabled) LampBlue else Color(0xFFCCCCCC), CircleShape)
            ) {
                if (selected) Text(text = "✓", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

/**
 * Single-page onboarding (Android): profile + season + settable reminder time.
 * Backend gets the SAME slot string as web (zero backend changes);
 * the exact clock time stays on-device for the mobile notification.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingProfileScreen(
    onFinish: (OnboardingData) -> Unit = {},
    isLoading: Boolean = false,
    error: String? = null
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var country by remember { mutableStateOf(defaultCountry()) }

    // Web parity: season must be picked, slot defaults to early_morning.
    var season by remember { mutableStateOf<String?>(null) }
    var time by remember { mutableStateOf("early_morning") }
    // Local-only clock times per slot (notification time, not sent to backend).
    var times by remember {
        mutableStateOf(Times.associate { it.value to (it.defaultHour to it.defaultMinute) })
    }
    var pickerSlot by remember { mutableStateOf<String?>(null) }

    val canFinish = firstName.isNotBlank() && lastName.isNotBlank() &&
        season != null && time.isNotBlank() && !isLoading

    Box(modifier = Modifier.fillMaxSize().background(Color.White).statusBarsPadding()) {
        Column(
            modifier = Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp, bottom = 24.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.lampstandicon),
                contentDescription = "Lampstand",
                // lampstandicon.png is already rounded — no extra clip (matches LoginScreen/web).
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text("Complete Your Profile", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = LampBlue)
            Text("Please fill in the information below to create your account",
                fontSize = 16.sp, color = BodyGrey, lineHeight = 22.sp)
            Spacer(modifier = Modifier.height(24.dp))

            FieldLabel("First Name")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = firstName, onValueChange = { firstName = it }, singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LampBlue, unfocusedBorderColor = FieldBorder,
                    focusedContainerColor = Color.White, unfocusedContainerColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(58.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            FieldLabel("Last Name")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = lastName, onValueChange = { lastName = it }, singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LampBlue, unfocusedBorderColor = FieldBorder,
                    focusedContainerColor = Color.White, unfocusedContainerColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(58.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            FieldLabel("Phone Number (Optional)")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = phone, onValueChange = { phone = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LampBlue, unfocusedBorderColor = FieldBorder,
                    focusedContainerColor = Color.White, unfocusedContainerColor = Color.White),
                leadingIcon = { CountryCodePicker(selected = country, onSelect = { country = it }) },
                modifier = Modifier.fillMaxWidth().height(58.dp)
            )
            Spacer(modifier = Modifier.height(28.dp))

            Text("Where are you right now?", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = BodyGrey)
            Text("Choose the season that feels most true for you today.", fontSize = 13.sp, color = BodyGrey)
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Seasons.filter { !it.comingSoon }.forEach { s ->
                    RadioRow(label = s.label, selected = season == s.value, onClick = { season = s.value })
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Spacer(modifier = Modifier.weight(1f).height(1.dp).background(Color(0xFFE0E0E0)))
                    Text("Coming Soon", fontSize = 11.sp, color = Color(0xFF999999))
                    Spacer(modifier = Modifier.weight(1f).height(1.dp).background(Color(0xFFE0E0E0)))
                }
                Seasons.filter { it.comingSoon }.forEach { s ->
                    RadioRow(label = s.label, selected = false, enabled = false, trailingSoon = true, onClick = {})
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            Text("When would you like your session?", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = BodyGrey)
            Text("We’ll send a mobile notification at this time. Tap the time to set your own.",
                fontSize = 13.sp, color = BodyGrey)
            Text("Usual range: ${Times.first { it.value == time }.range}",
                fontSize = 12.sp, color = LampBlue, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Times.forEach { t ->
                    val selected = time == t.value
                    val (h, m) = times[t.value] ?: (t.defaultHour to t.defaultMinute)
                    val border = if (selected) LampBlue else Color(0xFFB3B3B3)
                    val bg = if (selected) Color(0xFFEFF6FA) else Color.White
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.5.dp, border, RoundedCornerShape(6.dp))
                            .clip(RoundedCornerShape(6.dp))
                            .background(bg)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { time = t.value }
                            )
                            .padding(14.dp)
                    ) {
                        Text(text = t.glyph, fontSize = 24.sp, color = LampBlue)
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = t.label, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = LampBlue)
                            Text(text = t.subtitle, fontSize = 13.sp, color = BodyGrey)
                            Text(text = t.range, fontSize = 12.sp, color = BodyGrey)
                            // Time dropdown chip — the intuitive set-your-own entry point.
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .border(1.dp, LampBlue, RoundedCornerShape(6.dp))
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { pickerSlot = t.value }
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(text = "◷", fontSize = 13.sp, color = LampBlue)
                                Text(text = formatClock(h, m), fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold, color = LampBlue)
                                Text(text = "▾", fontSize = 12.sp, color = LampBlue)
                            }
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (selected) LampBlue else Color.Transparent)
                                .border(1.5.dp, LampBlue, CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { time = t.value }
                                )
                        ) {
                            if (selected) Text(text = "✓", fontSize = 14.sp,
                                fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            if (error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(error, fontSize = 14.sp, color = Color(0xFFE5484D))
            }
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.fillMaxWidth().height(52.dp)) {
                LampButton(
                    text = if (isLoading) "Starting…" else "Start My Journey",
                    onClick = {
                        val tz = try { java.util.TimeZone.getDefault().id ?: "UTC" } catch (_: Exception) { "UTC" }
                        val (rh, rm) = times[time] ?: (6 to 45)
                        onFinish(
                            OnboardingData(
                                firstName = firstName.trim(),
                                lastName = lastName.trim(),
                                phoneE164 = buildPhoneE164(country.dial, phone),
                                spiritualSeason = season ?: return@LampButton,
                                studyTimePreference = time,
                                reminderHour = rh,
                                reminderMinute = rm,
                                contactPreference = "email",
                                timezone = tz
                            )
                        )
                    },
                    enabled = canFinish,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                )
                if (isLoading) CircularProgressIndicator(
                    color = LampBlue, modifier = Modifier.align(Alignment.Center).size(24.dp))
            }
            Spacer(modifier = Modifier.height(60.dp))
        }
        // Time picker dialog shared across the four slots.
        pickerSlot?.let { slotValue ->
            val slot = Times.first { it.value == slotValue }
            val (ih, im) = times[slotValue] ?: (slot.defaultHour to slot.defaultMinute)
            val pickerState = rememberTimePickerState(initialHour = ih, initialMinute = im, is24Hour = false)
            AlertDialog(
                onDismissRequest = { pickerSlot = null },
                title = { Text("${slot.label} reminder", fontWeight = FontWeight.SemiBold, color = LampBlue) },
                text = {
                    Column {
                        Text("Usual range: ${slot.range}", fontSize = 13.sp, color = BodyGrey)
                        Spacer(modifier = Modifier.height(8.dp))
                        TimePicker(state = pickerState)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        times = times.toMutableMap().apply { put(slotValue, pickerState.hour to pickerState.minute) }
                        time = slotValue
                        pickerSlot = null
                    }) { Text("Set time", color = LampBlue) }
                },
                dismissButton = {
                    TextButton(onClick = { pickerSlot = null }) { Text("Cancel", color = BodyGrey) }
                }
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun OnboardingProfileScreenPreview() {
    LampStandTheme(dynamicColor = false) { OnboardingProfileScreen() }
}
