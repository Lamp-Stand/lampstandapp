package net.lampstandapp.app.profile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.lampstandapp.app.R
import net.lampstandapp.app.dashboard.BottomNavBar
import net.lampstandapp.app.ui.components.CountryCodePicker
import net.lampstandapp.app.ui.components.defaultCountry
import net.lampstandapp.app.ui.components.formatFullPhone
import net.lampstandapp.app.ui.components.matchCountryForPhone
import net.lampstandapp.app.ui.theme.LampStandTheme

private val Blue = Color(0xFF335E78)
private val BodyGrey = Color(0xFF5A5A5A)
private val FieldBorder = Color(0xFFD0D7DC)

@Composable
private fun EditFieldLabel(text: String) {
    Text(
        text = text,
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 22.sp,
        color = BodyGrey
    )
}

@Composable
private fun EditTextField(
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    leading: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Blue,
            unfocusedBorderColor = FieldBorder,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedTextColor = BodyGrey,
            unfocusedTextColor = BodyGrey
        ),
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal
        ),
        leadingIcon = leading,
        modifier = Modifier.fillMaxWidth().height(58.dp)
    )
}

/**
 * Profile edit screen (design/profileeditscreen.png).
 */
@Composable
fun ProfileEditScreen(
    initialFirstName: String = "John",
    initialLastName: String = "Doe",
    initialPhone: String = "+234 801 0000 000",
    initialReminderEnabled: Boolean = true,
    onItemSelected: (Int) -> Unit = {},
    onBack: () -> Unit = {},
    onSave: (firstName: String, lastName: String, phone: String, reminderEnabled: Boolean) -> Unit = { _, _, _, _ -> }
) {
    var firstName by remember { mutableStateOf(initialFirstName) }
    var lastName by remember { mutableStateOf(initialLastName) }
    // Split stored "+234 801..." into selectable country + national number.
    val initialSplit = remember(initialPhone) { matchCountryForPhone(initialPhone, defaultCountry()) }
    var country by remember(initialPhone) { mutableStateOf(initialSplit.first) }
    var phone by remember(initialPhone) { mutableStateOf(initialSplit.second) }
    var reminderEnabled by remember { mutableStateOf(initialReminderEnabled) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
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
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Personal Information",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 30.sp,
                color = Blue
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Edit your personal details",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 22.sp,
                color = BodyGrey
            )
            Spacer(modifier = Modifier.height(32.dp))
            EditFieldLabel("First Name")
            Spacer(modifier = Modifier.height(8.dp))
            EditTextField(value = firstName, onValueChange = { firstName = it })
            Spacer(modifier = Modifier.height(20.dp))
            EditFieldLabel("Last Name")
            Spacer(modifier = Modifier.height(8.dp))
            EditTextField(value = lastName, onValueChange = { lastName = it })
            Spacer(modifier = Modifier.height(20.dp))
            EditFieldLabel("Phone Number")
            Spacer(modifier = Modifier.height(8.dp))
            EditTextField(
                value = phone,
                onValueChange = { phone = it },
                keyboardType = KeyboardType.Phone,
                leading = {
                    CountryCodePicker(
                        selected = country,
                        onSelect = { country = it }
                    )
                }
            )
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Daily Reminder",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 22.sp,
                        color = BodyGrey
                    )
                    Text(
                        text = "A gentle nudge for your daily session",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 18.sp,
                        color = BodyGrey
                    )
                }
                Switch(
                    checked = reminderEnabled,
                    onCheckedChange = { reminderEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Blue,
                        checkedThumbColor = Color.White
                    )
                )
            }
            if (reminderEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Every 5 hours from 6:45 AM to 10:00 PM until today's session is completed.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 18.sp,
                    color = BodyGrey
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { onSave(firstName, lastName, formatFullPhone(country, phone), reminderEnabled) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C5C5C)),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(
                    "Save Changes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            BottomNavBar(selectedIndex = 3, onItemSelected = onItemSelected)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfileEditScreenPreview() {
    LampStandTheme(dynamicColor = false) {
        ProfileEditScreen()
    }
}
