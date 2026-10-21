package net.lampstandapp.app.auth

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.lampstandapp.app.R
import net.lampstandapp.app.ui.components.LampButton
import net.lampstandapp.app.ui.theme.LampStandTheme
import kotlinx.coroutines.delay

private val Blue = Color(0xFF335E78)
private val BodyGrey = Color(0xFF5A5A5A)
private val FieldBorder = Color(0xFFD0D7DC)

/**
 * 1-1 replica of web wireframe Screen 2 (`/auth` Step 2: Verification Code).
 * 4 digit boxes (auto-submit on 4th) + resend w/ 60s cooldown + Continue.
 * No dev-mode box. Email is shown in the subtitle.
 */
@Composable
fun CreateAccountScreen(
    email: String = "",
    onContinue: (email: String, password: String) -> Unit = { _, _ -> },
    onSendOtp: (email: String) -> Unit = {},
    onVerifyOtp: (email: String, code: String) -> Unit = { e, c -> onContinue(e, c) },
    onGoogleSignUp: () -> Unit = {},
    onLogIn: () -> Unit = {},
    isLoading: Boolean = false,
    error: String? = null,
) {
    var code by remember { mutableStateOf(listOf("", "", "", "")) }
    var cooldown by remember { mutableStateOf(60) }
    LaunchedEffect(cooldown) {
        if (cooldown > 0) { delay(1000); cooldown-- }
    }
    fun resend() {
        onSendOtp(email)
        code = listOf("", "", "", "")
        cooldown = 60
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.White).statusBarsPadding()
    ) {
        Column(
            // Content owns the top 60%; bottom 40% stays blank white.
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.weight(0.6f).fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .padding(top = 28.dp)
            ) {
                Spacer(modifier = Modifier.height(40.dp))
                Text(
                    text = "Verification Code",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 32.sp,
                    color = Blue
                )
                Text(
                    text = "Enter the 4-digit code we sent to ${email.ifBlank { "your email" }}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 28.sp,
                    color = BodyGrey
                )
                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val focusers = remember { List(4) { FocusRequester() } }
                    code.forEachIndexed { i, digit ->
                        OutlinedTextField(
                            value = digit,
                            onValueChange = { v ->
                                val d = v.filter { it.isDigit() }.takeLast(1)
                                val next = code.toMutableList().apply { this[i] = d }
                                code = next
                                if (d.isNotEmpty() && i < 3) focusers[i + 1].requestFocus()
                                if (next.all { it.isNotEmpty() }) onVerifyOtp(email, next.joinToString(""))
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Blue,
                                unfocusedBorderColor = FieldBorder,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedTextColor = Blue,
                                unfocusedTextColor = Blue
                            ),
                            modifier = Modifier.width(64.dp).height(64.dp).focusRequester(focusers[i])
                        )
                    }
                    LaunchedEffect(Unit) { focusers[0].requestFocus() }
                }
                error?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = it,
                        fontSize = 14.sp,
                        color = Color(0xFFE5484D),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(60.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Didn't receive a code?", fontSize = 14.sp, color = BodyGrey)
                    Spacer(modifier = Modifier.height(4.dp))
                    if (cooldown > 0) {
                        Text("Resend available in ${cooldown}s", fontSize = 14.sp, color = Color(0xFF92ADBE))
                    } else {
                        Text(
                            text = "Resend code",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Blue,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = ::resend
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                LampButton(
                    text = if (isLoading) "Verifying…" else "Continue",
                    onClick = { onVerifyOtp(email, code.joinToString("")) },
                    enabled = code.all { it.isNotEmpty() } && !isLoading,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                )
            }
            // Bottom 40% — blank.
            Spacer(modifier = Modifier.weight(0.4f).fillMaxWidth())
        }
        if (isLoading) {
            // Web replaces the whole page with <Loader /> while verifying.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize().background(Color(0x80FFFFFF))
            ) {
                CircularProgressIndicator(color = Blue)
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CreateAccountScreenPreview() {
    LampStandTheme(dynamicColor = false) { CreateAccountScreen(email = "you@example.com") }
}
