package com.example.lampstand.journey

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lampstand.R
import com.example.lampstand.ui.theme.LampStandTheme

/**
 * Guided Prayer (Journey screen 5 / web GuidedPrayerSection).
 * Complete Session calls straight through — web has no delay or popup.
 */
@Composable
fun JourneyPrayerScreen(
    onBack: () -> Unit = {},
    onComplete: () -> Unit = {},
    // Web: top-left back links to path detail; footer arrow steps back
    // to Application.
    onBackToPath: () -> Unit = {},
    isCompleting: Boolean = false,
    sessionError: String? = null,
    // Web GuidedPrayerSection: content.prayerText.
    prayerText: String? = null
) {
    val effectivePrayer = prayerText?.takeIf { it.isNotBlank() }
        ?: "Father, thank You for being my refuge when life feels loud and overwhelming. Teach me what it truly means to be still before You."
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // M1 back: 30 x 30, top 71, left 19, radius 16, white.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 19.dp, top = 71.dp)
                    .size(30.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBackToPath
                    )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = "Back",
                    tint = Color(0xFF335E78),
                    modifier = Modifier.size(16.dp)
                )
            }
            // M2: 263 x 14, top 125, left 20, gap 4 (24 below M1).
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 20.dp, top = 125.dp)
                    .width(263.dp)
                    .height(14.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_read_cv_logo),
                    contentDescription = null,
                    tint = Color(0xFF92ADBE),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "GUIDED PRAYER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 14.sp,
                    letterSpacing = 0.sp,
                    color = Color(0xFF92ADBE),
                    maxLines = 1,
                    modifier = Modifier
                        .width(247.dp)
                        .height(14.dp)
                )
            }
            // M3: "Talk to God", 226 x 42, top 148, left 19, 20/26.
            Text(
                text = "Talk to God",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 26.sp,
                letterSpacing = 0.sp,
                color = Color(0xFF184159),
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 19.dp, top = 148.dp)
                    .width(226.dp)
                    .height(42.dp)
            )
            // Lower flow from M4 (top 247, 57 below M3): card, 132,
            // nav, 80, button, 131 bottom space.
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 247.dp)
                    .fillMaxWidth()
            ) {
                // M4 card: 335 wide, radius 4, 3px left border,
                // background #335E78 at 10%. Wraps so the prayer
                // text (needs ~3+ lines) never clips.
                Box(
                    modifier = Modifier
                        .padding(start = 20.dp)
                        .width(335.dp)
                        .heightIn(min = 125.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x1A335E78))
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(3.dp)
                            .fillMaxHeight()
                            .clip(
                                RoundedCornerShape(
                                    topStart = 4.dp,
                                    bottomStart = 4.dp,
                                    topEnd = 0.dp,
                                    bottomEnd = 0.dp
                                )
                            )
                            .background(Color(0xFF335E78))
                    )
                    // M4E1: 116 x 16, top 16, left 13, gap 4.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 13.dp, top = 16.dp)
                            .width(116.dp)
                            .height(16.dp)
                    ) {
                        // Star chip: 16 x 16, radius 15, tinted 20%.
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(Color(0x33335E78))
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.stars),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                        // M4E1E2: 96 x 15, Italic Medium 13/15, #335E78.
                        Text(
                            text = "Guided Prayer",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontStyle = FontStyle.Italic,
                            lineHeight = 15.sp,
                            letterSpacing = 0.sp,
                            color = Color(0xFF335E78),
                            maxLines = 1,
                            modifier = Modifier
                                .width(96.dp)
                                .height(15.dp)
                        )
                    }
                    // M4E2: live guided prayer text.
                    Text(
                        text = effectivePrayer,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 20.sp,
                        letterSpacing = 0.sp,
                        color = Color(0xFF535353),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 17.dp, top = 45.dp, end = 22.dp, bottom = 12.dp)
                            .width(296.dp)
                    )
                }
                Spacer(modifier = Modifier.height(132.dp))
                // M5: 187 x 30, left 20, space-between — prev + dots.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .padding(start = 20.dp)
                        .width(187.dp)
                        .height(30.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onBack
                            )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = "Previous",
                            tint = Color(0xFF335E78),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    // Fifth bullet highlighted.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .width(74.dp)
                            .height(10.dp)
                    ) {
                        repeat(5) { index ->
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (index == 4) Color(0xFF335E78)
                                        else Color(0xFFB3B3B3)
                                    )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(80.dp))
                if (sessionError != null) {
                    Text(
                        text = sessionError,
                        fontSize = 13.sp,
                        color = Color(0xFFE5484D),
                        modifier = Modifier.padding(start = 19.dp, bottom = 8.dp)
                    )
                }
                // M6 button: 334 x 42, left 19, radius 8, padding
                // 10/12, #335E78. Web: straight to completeSession.
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(start = 19.dp)
                        .width(334.dp)
                        .height(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isCompleting) Color(0xFF92ADBE) else Color(0xFF335E78))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = !isCompleting,
                            onClick = onComplete
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = if (isCompleting) "Completing..." else "Complete Session",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 22.sp,
                        letterSpacing = 0.sp,
                        textAlign = TextAlign.Center,
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier
                            .width(139.dp)
                            .height(22.dp)
                    )
                }
                // Web footer is px-8 py-[23px] under the CTA; there is no
                // bottom nav on /session to clear any more.
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun JourneyPrayerScreenPreview() {
    LampStandTheme(dynamicColor = false) {
        JourneyPrayerScreen()
    }
}
