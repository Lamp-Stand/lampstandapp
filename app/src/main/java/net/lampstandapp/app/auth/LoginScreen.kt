package net.lampstandapp.app.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.lampstandapp.app.R
import net.lampstandapp.app.ui.theme.LampStandTheme

private val Blue = Color(0xFF335E78)
private val BodyGrey = Color(0xFF5A5A5A)

/**
 * 1-1 replica of web wireframe Screen 1 (`/auth` Step 1: Welcome).
 * Logo -> title -> subtitle -> Continue with Google. Nothing else.
 * (Web has no email/password/OTP UI on this step, no create-account link.)
 */
@Composable
fun LoginScreen(
    onGoogleLogIn: () -> Unit = {},
    isLoading: Boolean = false,
    error: String? = null,
    onSendOtp: (email: String) -> Unit = {},
    onVerifyOtp: (email: String, code: String) -> Unit = { _, _ -> },
    onCreateAccount: () -> Unit = {},
    onLogIn: (email: String, password: String) -> Unit = { _, _ -> },
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.White).statusBarsPadding()
    ) {
        Column(
            // Content owns the top 60%; bottom 40% stays blank white.
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                // Web: container px-7 (28px) pt-7 (28px), max-w-md centered.
                // Scrollable: email OTP added extra content vs web, so small
                // screens scroll instead of clipping the Google button.
                modifier = Modifier.weight(0.6f).fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp)
                    .padding(top = 28.dp, bottom = 16.dp)
            ) {
            // Web: logo h-12 w-12 (48px) mt-10 (40px) mb-10 (40px).
            Spacer(modifier = Modifier.height(40.dp))
            Image(
                painter = painterResource(id = R.drawable.lampstandicon),
                contentDescription = "logo",
                // Web SVG is already rounded — no extra clip.
                modifier = Modifier.size(48.dp)
            )
            // Web: logo mb-10 (40px) + h1 mt-10 (40px) = 80px gap.
            Spacer(modifier = Modifier.height(80.dp))
            Text(
                text = "Welcome to Lampstand!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 32.sp,
                color = Blue
            )
            // Web: h1 -> p has no gap.
            Text(
                text = "Your formation journey starts here.",
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 28.sp,
                color = BodyGrey
            )
            // Web: button block mt-8 (32px).
            Spacer(modifier = Modifier.height(32.dp))
            // Web: rounded-lg (8px), border gray-300, px-4 py-3.5 (14px),
            // text-sm (14px) semibold gray-700, gap-3 (12px), icon 20px, shadow-sm.
            OutlinedButton(
                onClick = onGoogleLogIn,
                enabled = !isLoading,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFD1D5DB)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF374151)
                ),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_google),
                    contentDescription = "Google",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Continue with Google",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF374151)
                )
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
            }
            // Bottom 40% — blank.
            Spacer(modifier = Modifier.weight(0.4f).fillMaxWidth())
        }
        if (isLoading) {
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
fun LoginScreenPreview() {
    LampStandTheme(dynamicColor = false) { LoginScreen() }
}
