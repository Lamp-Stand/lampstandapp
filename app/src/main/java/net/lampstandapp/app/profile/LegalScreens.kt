package net.lampstandapp.app.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.lampstandapp.app.R

private val LegalBlue = Color(0xFF335E78)
private val LegalHeading = Color(0xFF184159)
private val LegalBody = Color(0xFF535353)

private data class LegalSection(
    val heading: String? = null,
    val body: String? = null,
    val bullets: List<String> = emptyList()
)

/** app/privacy/page.tsx */
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit = {}) {
    LegalPage(
        title = "Privacy Policy",
        onBack = onBack,
        sections = listOf(
            LegalSection(
                body = "At Lampstand, we take your privacy seriously. This Privacy Policy explains how we collect, use, and protect your information when you use our application."
            ),
            LegalSection(
                heading = "1. Information We Collect",
                bullets = listOf(
                    "Account Information: When you sign in with Google or Email, we collect your name, email address, and profile picture.",
                    "Formation Data: Preferences, spiritual season selections, and daily progress data you provide to personalize your experience."
                )
            ),
            LegalSection(
                heading = "2. How We Use Information",
                body = "We use your data solely to provide, personalize, and improve the Lampstand experience, deliver daily session reminders, and manage your account. We never sell your personal data."
            ),
            LegalSection(
                heading = "3. Data Security & Storage",
                body = "Your data is stored securely using industry-standard backend infrastructure (Convex) and authenticated via encrypted OAuth / session tokens."
            ),
            LegalSection(
                heading = "4. Contact Us",
                body = "If you have any questions about this Privacy Policy, please contact us at lampstand26@gmail.com."
            )
        )
    )
}

/** app/terms/page.tsx */
@Composable
fun TermsOfServiceScreen(onBack: () -> Unit = {}) {
    LegalPage(
        title = "Terms of Service",
        onBack = onBack,
        sections = listOf(
            LegalSection(
                body = "Welcome to Lampstand. By accessing or using our application, you agree to be bound by these Terms of Service."
            ),
            LegalSection(
                heading = "1. Acceptance of Terms",
                body = "By creating an account or using Lampstand, you agree to comply with these terms and applicable laws. If you do not agree, please do not use the app."
            ),
            LegalSection(
                heading = "2. Use of Service",
                body = "Lampstand provides spiritual formation paths and content. You agree to use the service for personal, non-commercial purposes and not to misuse or attempt unauthorized access to our systems."
            ),
            LegalSection(
                heading = "3. Account Responsibility",
                body = "You are responsible for maintaining the security of your account authentication methods and for all activities that occur under your account."
            ),
            LegalSection(
                heading = "4. Contact Us",
                body = "For questions regarding these Terms, contact us at lampstand26@gmail.com."
            )
        )
    )
}

@Composable
private fun LegalPage(
    title: String,
    sections: List<LegalSection>,
    onBack: () -> Unit
) {
    Column(
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
                .padding(top = 12.dp, bottom = 32.dp)
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
                    tint = LegalBlue,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.size(24.dp))
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 30.sp,
                color = LegalHeading
            )
            Text(
                text = "Effective Date: October 3, 2026",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = LegalBody,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                sections.forEach { section ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        section.heading?.let {
                            Text(
                                text = it,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 22.sp,
                                color = LegalHeading
                            )
                        }
                        section.body?.let {
                            Text(
                                text = it,
                                fontSize = 14.sp,
                                lineHeight = 21.sp,
                                color = LegalBody
                            )
                        }
                        section.bullets.forEach { bullet ->
                            Text(
                                text = "\u2022  $bullet",
                                fontSize = 14.sp,
                                lineHeight = 21.sp,
                                color = LegalBody
                            )
                        }
                    }
                }
            }
        }
    }
}
