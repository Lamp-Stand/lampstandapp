package net.lampstandapp.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class CountryCallingCode(
    val iso: String,
    val name: String,
    val dial: String,
    val flag: String
)

val countryCallingCodes = listOf(
    CountryCallingCode("NG", "Nigeria", "+234", "\uD83C\uDDF3\uD83C\uDDEC"),
    CountryCallingCode("GH", "Ghana", "+233", "\uD83C\uDDEC\uD83C\uDDED"),
    CountryCallingCode("KE", "Kenya", "+254", "\uD83C\uDDF0\uD83C\uDDEA"),
    CountryCallingCode("ZA", "South Africa", "+27", "\uD83C\uDDFF\uD83C\uDDE6"),
    CountryCallingCode("EG", "Egypt", "+20", "\uD83C\uDDEA\uD83C\uDDEC"),
    CountryCallingCode("US", "United States", "+1", "\uD83C\uDDFA\uD83C\uDDF8"),
    CountryCallingCode("CA", "Canada", "+1", "\uD83C\uDDE8\uD83C\uDDE6"),
    CountryCallingCode("GB", "United Kingdom", "+44", "\uD83C\uDDEC\uD83C\uDDE7"),
    CountryCallingCode("IE", "Ireland", "+353", "\uD83C\uDDEE\uD83C\uDDEA"),
    CountryCallingCode("FR", "France", "+33", "\uD83C\uDDEB\uD83C\uDDF7"),
    CountryCallingCode("DE", "Germany", "+49", "\uD83C\uDDE9\uD83C\uDDEA"),
    CountryCallingCode("ES", "Spain", "+34", "\uD83C\uDDEA\uD83C\uDDF8"),
    CountryCallingCode("IT", "Italy", "+39", "\uD83C\uDDEE\uD83C\uDDF9"),
    CountryCallingCode("NL", "Netherlands", "+31", "\uD83C\uDDF3\uD83C\uDDF1"),
    CountryCallingCode("IN", "India", "+91", "\uD83C\uDDEE\uD83C\uDDF3"),
    CountryCallingCode("PK", "Pakistan", "+92", "\uD83C\uDDF5\uD83C\uDDF0"),
    CountryCallingCode("PH", "Philippines", "+63", "\uD83C\uDDF5\uD83C\uDDED"),
    CountryCallingCode("AU", "Australia", "+61", "\uD83C\uDDE6\uD83C\uDDFA"),
    CountryCallingCode("NZ", "New Zealand", "+64", "\uD83C\uDDF3\uD83C\uDDFF"),
    CountryCallingCode("BR", "Brazil", "+55", "\uD83C\uDDE7\uD83C\uDDF7"),
    CountryCallingCode("AE", "UAE", "+971", "\uD83C\uDDE6\uD83C\uDDEA"),
    CountryCallingCode("SA", "Saudi Arabia", "+966", "\uD83C\uDDF8\uD83C\uDDE6"),
    CountryCallingCode("JP", "Japan", "+81", "\uD83C\uDDEF\uD83C\uDDF5"),
    CountryCallingCode("CN", "China", "+86", "\uD83C\uDDE8\uD83C\uDDF3")
)

fun defaultCountry(): CountryCallingCode = countryCallingCodes.first()

/** Split "+234 801..." into its country + national number. Falls back to [default]. */
fun matchCountryForPhone(phone: String, default: CountryCallingCode = defaultCountry()): Pair<CountryCallingCode, String> {
    val trimmed = phone.trim()
    val match = countryCallingCodes
        .sortedByDescending { it.dial.length }
        .firstOrNull { trimmed.startsWith(it.dial) }
    return if (match != null) {
        match to trimmed.removePrefix(match.dial).trim()
    } else {
        default to trimmed.removePrefix("+").trim()
    }
}

/** Combines dial code + national number for storage, e.g. "+234 801 000 000". */
fun formatFullPhone(country: CountryCallingCode, nationalNumber: String): String {
    val number = nationalNumber.trim()
    return if (number.isBlank()) country.dial else "${country.dial} $number".trim()
}

/**
 * Flag + dial-code picker used as the leading icon of phone fields.
 * Tapping opens a dropdown so the user can change country.
 */
@Composable
fun CountryCodePicker(
    selected: CountryCallingCode,
    onSelect: (CountryCallingCode) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .padding(start = 12.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { expanded = true }
                )
                .padding(vertical = 4.dp, horizontal = 2.dp)
        ) {
            Text(text = selected.flag, fontSize = 18.sp)
            Text(text = selected.dial, fontSize = 14.sp, color = Color.Black)
            Text(text = "\u02C7", fontSize = 12.sp, color = Color.Black)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            countryCallingCodes.forEach { country ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = country.flag, fontSize = 18.sp)
                            Text(text = "${country.name} ${country.dial}", fontSize = 14.sp)
                        }
                    },
                    onClick = {
                        onSelect(country)
                        expanded = false
                    }
                )
            }
        }
    }
}
