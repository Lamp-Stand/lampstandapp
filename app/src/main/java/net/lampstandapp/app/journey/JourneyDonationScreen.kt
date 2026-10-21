package net.lampstandapp.app.journey

import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.lampstandapp.app.R
import net.lampstandapp.app.dashboard.BottomNavBar
import net.lampstandapp.app.ui.theme.LampStandTheme
import java.util.Locale

private val Navy = Color(0xFF184159)
private val Blue = Color(0xFF335E78)
private val MutedSlate = Color(0xFF5A7181)
private val SubtleGrey = Color(0xFF8A8A8A)
private val UnselectedBorder = Color(0xFFE1E7EB)
private val PaleSelected = Color(0xFFEEF4F8)
private val NoteAmber = Color(0xFFB45309)

/** Mirrors web support/page.tsx EXCHANGE_RATES — USD-denominated multipliers. */
private val exchangeRates = mapOf(
    "USD" to 1.0,
    "NGN" to 1383.63,
    "GBP" to 0.75,
    "EUR" to 0.88,
    "CAD" to 1.42,
    "AUD" to 1.45,
    "JPY" to 162.55
)

private val currencySymbols = mapOf(
    "USD" to "$", "NGN" to "₦", "GBP" to "£", "EUR" to "€",
    "CAD" to "C$", "AUD" to "A$", "JPY" to "¥"
)

private val currencyOptions = listOf(
    "USD" to "USD ($)", "NGN" to "NGN (₦)", "GBP" to "GBP (£)", "EUR" to "EUR (€)",
    "CAD" to "CAD (C$)", "AUD" to "AUD (A$)", "JPY" to "JPY (¥)"
)

/**
 * What the user chose on the support screen. Mirrors the web
 * `initializeOneTimeDonation` / `initializeMonthlyDonation` arguments.
 */
data class DonationRequest(
    val kind: String,          // "one_time" | "monthly"
    val currency: String,
    val amountMinor: Long? = null,   // one-time: smallest currency unit (cents/kobo)
    val tierKey: String? = null      // monthly: monthly_7 / monthly_ngn_7 / ...
)

private fun groupInt(value: Double): String = String.format(Locale.US, "%,.0f", value)
private fun group2(value: Double): String = String.format(Locale.US, "%,.2f", value)

@Composable
private fun AmountChip(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(52.dp)
            .border(
                width = if (selected) 1.5.dp else 0.5.dp,
                color = if (selected) Blue else UnselectedBorder,
                shape = RoundedCornerShape(6.dp)
            )
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) PaleSelected else Color.White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSelect
            )
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Blue else Color(0xFF666666)
        )
    }
}

/** Monthly tier chip: amount on top, "PER MONTH" micro-label under it. */
@Composable
private fun TierChip(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .height(60.dp)
            .border(
                width = if (selected) 1.5.dp else 0.5.dp,
                color = if (selected) Blue else UnselectedBorder,
                shape = RoundedCornerShape(6.dp)
            )
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) PaleSelected else Color.White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSelect
            )
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Blue else Color(0xFF666666)
        )
        Text(
            text = "PER MONTH",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = Color(0xFF9CA3AF),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun AmountField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MutedSlate
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color(0xFF9CA3AF), fontSize = 15.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Blue,
                unfocusedBorderColor = UnselectedBorder,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedTextColor = Navy,
                unfocusedTextColor = Navy
            ),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        )
    }
}

/**
 * Support screen — web /my-journey/[id]/support.
 * Currency selector, then "1.) One-Time Gift" and "2.) Monthly Support",
 * then the no-pressure footer with "Maybe Later".
 */
@Composable
fun JourneyDonationScreen(
    onItemSelected: (Int) -> Unit = {},
    onBack: () -> Unit = {},
    isProcessing: Boolean = false,
    onGive: (DonationRequest) -> Unit = {},
    onMaybeLater: () -> Unit = {}
) {
    val context = LocalContext.current
    var currency by remember { mutableStateOf("USD") }
    var menuOpen by remember { mutableStateOf(false) }

    // Web starts with nothing selected in either section.
    var selectedOneTimePreset by remember { mutableStateOf<Double?>(null) }
    var customOneTime by remember { mutableStateOf("") }
    var selectedMonthlyTier by remember { mutableStateOf<String?>(null) }
    var customMonthly by remember { mutableStateOf("") }

    val isNgn = currency == "NGN"
    val symbol = currencySymbols[currency] ?: currency
    val oneTimePresets = if (isNgn) listOf(3000.0, 7000.0, 15000.0) else listOf(5.0, 15.0, 30.0)
    val monthlyTiers = if (isNgn) {
        listOf("₦2,500" to "monthly_ngn_3", "₦5,000" to "monthly_ngn_7", "₦10,000" to "monthly_ngn_15")
    } else {
        listOf("$3" to "monthly_3", "$7" to "monthly_7", "$15" to "monthly_15")
    }

    // Web getOneTimeButtonLabel()
    fun oneTimeLabel(): String {
        val amount = selectedOneTimePreset ?: customOneTime.toDoubleOrNull()
        if (amount == null || amount.isNaN() || amount <= 0) return "Give with Card"
        return when (currency) {
            "USD" -> "Give $${group2(amount)}"
            "NGN" -> "Give ₦${groupInt(amount)}"
            else -> {
                val converted = amount * (exchangeRates[currency] ?: 1.0)
                val approx = if (currency == "JPY") groupInt(converted) else group2(converted)
                "Give $${group2(amount)} (approx. $symbol$approx)"
            }
        }
    }

    // Web getMonthlyButtonLabel()
    fun monthlyLabel(): String {
        val tier = selectedMonthlyTier
        if (tier == null) {
            val custom = customMonthly.toDoubleOrNull()
            if (custom != null && !custom.isNaN()) {
                return if (isNgn) "Subscribe to ₦5,000 / month" else "Subscribe to $7 / month"
            }
            return "Give Monthly"
        }
        // Same "includes" rules as web: 3 -> $3, 15 -> $15, else $7.
        val usd = when {
            tier.contains("15") -> 15.0
            tier.contains("3") -> 3.0
            else -> 7.0
        }
        return when (currency) {
            "USD" -> "Subscribe for $${usd.toInt()} / month"
            "NGN" -> {
                val ngn = if (usd == 3.0) "2,500" else if (usd == 7.0) "5,000" else "10,000"
                "Subscribe for ₦$ngn / month"
            }
            else -> {
                val converted = usd * (exchangeRates[currency] ?: 1.0)
                val approx = if (currency == "JPY") groupInt(converted) else group2(converted)
                "Subscribe for $${usd.toInt()}/mo (approx. $symbol$approx/mo)"
            }
        }
    }

    // Web resets every selection whenever the currency changes.
    fun changeCurrency(next: String) {
        currency = next
        selectedOneTimePreset = null
        customOneTime = ""
        selectedMonthlyTier = null
        customMonthly = ""
    }

    fun submitOneTime() {
        val amount = selectedOneTimePreset ?: customOneTime.toDoubleOrNull()
        if (amount == null || amount.isNaN() || amount <= 0) {
            Toast.makeText(context, "Please select a preset amount or enter a valid custom amount.", Toast.LENGTH_SHORT).show()
            return
        }
        val rate = if (currency == "NGN") 1.0 else exchangeRates[currency] ?: 1.0
        val minor = (amount * rate * 100).toLong()
        if (minor <= 0L) {
            Toast.makeText(context, "Please enter a valid amount.", Toast.LENGTH_SHORT).show()
            return
        }
        onGive(DonationRequest(kind = "one_time", currency = currency, amountMinor = minor))
    }

    fun submitMonthly() {
        if (customMonthly.isNotBlank()) {
            onGive(DonationRequest(kind = "monthly", currency = currency, tierKey = if (isNgn) "monthly_ngn_7" else "monthly_7"))
            return
        }
        val tier = selectedMonthlyTier
        if (tier == null) {
            Toast.makeText(context, "Please select a monthly tier first.", Toast.LENGTH_SHORT).show()
            return
        }
        onGive(DonationRequest(kind = "monthly", currency = currency, tierKey = tier))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 24.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBack
                    )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = "Back",
                    tint = Blue,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_read_cv_logo),
                    contentDescription = null,
                    tint = Color(0xFF92ADBE),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "SUPPORT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 14.sp,
                    letterSpacing = 1.2.sp,
                    color = Color(0xFF92ADBE)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Support the Mission",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 32.sp,
                color = Color(0xFF1E3F57)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Lampstand is free right now because we believe formation should be accessible to every believer. We're a small team building something we believe matters deeply. If this journey has been meaningful to you, consider supporting what we're building.",
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 22.sp,
                color = MutedSlate
            )
            Spacer(modifier = Modifier.height(24.dp))

            // ---- Currency selector (web: bg-slate-50 rounded row + <select>) ----
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(0.5.dp, UnselectedBorder, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "Pay In Your Local Currency:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B)
                )
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White)
                            .border(0.5.dp, UnselectedBorder, RoundedCornerShape(6.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { menuOpen = true }
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = currencyOptions.firstOrNull { it.first == currency }?.second ?: currency,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Blue
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        currencyOptions.forEach { (code, label) ->
                            DropdownMenuItem(
                                text = { Text(label, fontSize = 13.sp) },
                                onClick = {
                                    menuOpen = false
                                    changeCurrency(code)
                                }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(28.dp))

            // ---- 1.) One-Time Gift ----
            Text(text = "1.) One-Time Gift", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Blue)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "A simple way to support the mission",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                oneTimePresets.forEach { amount ->
                    AmountChip(
                        label = if (isNgn) "₦${groupInt(amount)}" else "$${amount.toInt()}",
                        selected = selectedOneTimePreset == amount,
                        onSelect = {
                            selectedOneTimePreset = amount
                            customOneTime = ""
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            AmountField(
                label = "Give Another Amount (${if (isNgn) "₦" else "$"})",
                placeholder = if (isNgn) "Enter Amount (₦)" else "Enter Amount ($)",
                value = customOneTime,
                onValueChange = {
                    customOneTime = it
                    selectedOneTimePreset = null
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { submitOneTime() },
                enabled = !isProcessing,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blue),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(
                    if (isProcessing) "Opening checkout..." else oneTimeLabel(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(32.dp))

            // ---- 2.) Monthly Support ----
            Text(text = "2.) Monthly Support", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Blue)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Help us build and maintain the vision.",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF64748B)
            )
            if (!isNgn && currency != "USD") {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "* Note: Recurring monthly support is billed in USD and automatically converted by your bank.",
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 18.sp,
                    color = Color(0xFF64748B),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFF8FAFC))
                        .padding(10.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                monthlyTiers.forEach { (label, key) ->
                    TierChip(
                        label = label,
                        selected = selectedMonthlyTier == key,
                        onSelect = {
                            selectedMonthlyTier = key
                            customMonthly = ""
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            AmountField(
                label = "Give Another Amount (${if (isNgn) "₦" else "$"})",
                placeholder = if (isNgn) "Enter Amount (₦)" else "Enter Amount ($)",
                value = customMonthly,
                onValueChange = {
                    customMonthly = it
                    selectedMonthlyTier = null
                },
                modifier = Modifier.fillMaxWidth()
            )
            if (customMonthly.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Note: Custom monthly amounts will be redirected to our suggested ${if (isNgn) "₦5,000" else "$7"} plan. For specific values, please select a preset tier above.",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 16.sp,
                    color = NoteAmber
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { submitMonthly() },
                enabled = !isProcessing,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blue),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(
                    if (isProcessing) "Opening checkout..." else monthlyLabel(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(28.dp))

            // ---- Footer ----
            Text(
                text = "** No pressure. Every amount helps. You can also skip and come back to this anytime.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 20.sp,
                color = MutedSlate,
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onMaybeLater,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Blue),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Maybe Later", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Blue)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
        BottomNavBar(selectedIndex = 2, onItemSelected = onItemSelected)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun JourneyDonationScreenPreview() {
    LampStandTheme(dynamicColor = false) {
        JourneyDonationScreen()
    }
}
