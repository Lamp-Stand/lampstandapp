package com.example.lampstand.journey

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lampstand.R
import com.example.lampstand.ui.theme.LampStandTheme

@Composable
fun JourneyScreenThree(
    onBack: () -> Unit = {},
    onNext: () -> Unit = {},
    // Web: top-left back links to path detail; footer arrow steps back
    // a section.
    onBackToPath: () -> Unit = {},
    // Web InsightSection: coreInsight = first sentence of insightText,
    // paragraphs = [insightText] or "Insight coming soon."
    insightText: String? = null
) {
    // Same split the web does: insightText.split(".")[0] + "."
    val coreInsight = insightText
        ?.takeIf { it.isNotBlank() }
        ?.split(".")?.firstOrNull()
        ?.let { "$it." }
        ?: "God is Compassionate & Gracious"
    val insightBody = insightText?.takeIf { it.isNotBlank() } ?: "Insight coming soon."
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
            // Back button: 30 x 30, top 71, left 19, radius 16, white.
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
            // First frame: 264 wide, top 125, left 19, gap 9.
            // Height wraps: two 26dp heading lines need ~52dp,
            // so spec 65 would clip "God" on line 2.
            Column(
                verticalArrangement = Arrangement.spacedBy(9.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 19.dp, top = 125.dp)
                    .width(264.dp)
            ) {
                // E1: 264 x 14, gap 4 — icon + label.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .width(264.dp)
                        .height(14.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_read_cv_logo),
                        contentDescription = null,
                        tint = Color(0xFF92ADBE),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "CHARACTER OF GOD INSIGHT",
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
                // Heading: 226 wide, 20/26, #184159. Wraps so line 2
                // never clips (two 26dp lines need 52, not spec 42).
                Text(
                    text = "What this Reveals About God",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 26.sp,
                    letterSpacing = 0.sp,
                    color = Color(0xFF184159),
                    modifier = Modifier.width(226.dp)
                )
            }
            // Second frame: card + body + nav; wraps for live text.
            Column(
                verticalArrangement = Arrangement.spacedBy(59.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 17.dp, top = 226.dp, bottom = 40.dp)
                    .width(338.dp)
            ) {
                // E1: card + body, gap 30.
                Column(
                    verticalArrangement = Arrangement.spacedBy(30.dp),
                    modifier = Modifier.width(338.dp)
                ) {
                    // E1E1 card: radius 4, 3px left
                    // border #335E78, background #335E78 at 10%.
                    // heightIn lets two-line core insights fit.
                    Box(
                        modifier = Modifier
                            .width(338.dp)
                            .heightIn(min = 107.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x1A335E78))
                    ) {
                        // Left border bar.
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
                        // E1E1E1: star, 16 x 16, top 16, left 13,
                        // radius 15, tinted #335E78 at 20%.
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = 13.dp, top = 16.dp)
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
                        // E1E1E2: 96 x 15, top 16, left 33,
                        // Italic Medium 11/15, #335E78.
                        Text(
                            text = "Core Insight",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontStyle = FontStyle.Italic,
                            lineHeight = 15.sp,
                            letterSpacing = 0.sp,
                            color = Color(0xFF335E78),
                            maxLines = 1,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = 33.dp, top = 16.dp)
                                .width(96.dp)
                                .height(15.dp)
                        )
                        // E1E1E3: 226 wide, top 46, left 17, 20/26,
                        // #184159. Live core insight (first sentence).
                        Text(
                            text = coreInsight,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 26.sp,
                            letterSpacing = 0.sp,
                            color = Color(0xFF184159),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = 17.dp, top = 46.dp)
                                .width(226.dp)
                                .heightIn(min = 42.dp)
                        )
                    }
                    // E1E2: live insight body (web paragraphs = [insightText]).
                    Text(
                        text = insightBody,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 20.sp,
                        letterSpacing = 0.sp,
                        color = Color(0xFF535353),
                        modifier = Modifier.width(338.dp)
                    )
                }
                // E2 nav row 59dp below e1: prev + dots + next.
                // 477 + 59 + 30 = 566 second frame.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .width(338.dp)
                        .height(30.dp)
                ) {
                    // Prev: 30 x 30, radius 16, white.
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
                    // Dots: 74 x 10, gap 6 — third highlighted.
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
                                        if (index == 2) Color(0xFF335E78)
                                        else Color(0xFFB3B3B3)
                                    )
                            )
                        }
                    }
                    // Next: 30 x 30, radius 16, white, arrow rotated -180.
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onNext
                            )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = "Next",
                            tint = Color(0xFF335E78),
                            modifier = Modifier
                                .size(16.dp)
                                .rotate(180f)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun JourneyScreenThreePreview() {
    LampStandTheme(dynamicColor = false) {
        JourneyScreenThree()
    }
}
