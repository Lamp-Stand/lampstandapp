package net.lampstandapp.app.journey

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.lampstandapp.app.R
import net.lampstandapp.app.ui.theme.LampStandTheme

@Composable
fun JourneyScreen(
    // Web S0 back button → path detail (/my-journey/[id]).
    onBack: () -> Unit = {},
    onNext: () -> Unit = {},
    // Live session (web TodaysJourneySection). Defaults = previous hardcoded UI.
    sessionTitle: String = "Finding Stillness",
    scriptureVerse: String = "“Be still, and know that I am God; I will be exalted among the nations, I will be exalted in the earth.”",
    scriptureReference: String = "Psalm 46:10 (NIV)",
    introText: String = "Finding Stillness invites you to quiet your heart, slow your mind, and become aware of God’s steady presence in the midst of life’s noise.",
    dayLabel: String = "TODAY’S JOURNEY",
    // Web 5-section pager: 0 Today's Journey · 1 Context · 2 Insight
    // · 3 Application · 4 Guided Prayer.
    currentSection: Int = 0,
    onDotClick: (Int) -> Unit = {}
) {
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
            // Back button: 30 x 30, top 40, left 19 — web S0 back → path detail.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 19.dp, top = 40.dp)
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
                    contentDescription = "Back",
                    tint = Color(0xFF335E78),
                    modifier = Modifier.size(16.dp)
                )
            }
            // Hero image: 335 x 148 — top 88, 20 both sides
            // (fillMaxWidth minus 40), 36 gap to the frame below (ends 236).
            Image(
                painter = painterResource(id = R.drawable.shipanchor),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 88.dp, end = 20.dp)
                    .height(148.dp)
            )
            // Frame underneath image: 263 x 14, top 272, left 21, gap 4.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 21.dp, top = 272.dp)
                    .width(263.dp)
                    .heightIn(min = 18.dp)
            ) {
                // ReadCV logo: 12 x 12.
                Icon(
                    painter = painterResource(id = R.drawable.ic_read_cv_logo),
                    contentDescription = null,
                    tint = Color(0xFF92ADBE),
                    modifier = Modifier.size(12.dp)
                )
                // Title: 247 x 14, Inter 600 SemiBold 12/14, #92ADBE.
                Text(
                    text = dayLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 14.sp,
                    letterSpacing = 0.sp,
                    color = Color(0xFF92ADBE),
                    maxLines = 1,
                    modifier = Modifier
                        .width(247.dp)
                        .heightIn(min = 18.dp)
                )
            }
            // Heading + body flow as one column so long titles wrap instead
            // of being clipped by the old fixed one-line box.
            Column(
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 20.dp, top = 291.dp, bottom = 40.dp)
                    .width(335.dp)
            ) {
                // Heading: Inter 600 SemiBold 28/32, #184159.
                Text(
                    text = sessionTitle,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 32.sp,
                    letterSpacing = 0.sp,
                    color = Color(0xFF184159),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                // Intro: 335 x 60, Inter 400 Regular 14/20, #535353.
                Text(
                    text = introText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 20.sp,
                    letterSpacing = 0.sp,
                    color = Color(0xFF535353),
                    modifier = Modifier
                        .width(335.dp)
                        .heightIn(min = 72.dp)
                )
                // Scripture card: 335 x 127, radius 4, padding 8/16,
                // 3px left border #335E78, background #335E78 at 10%.
                Box(
                    modifier = Modifier
                        .width(335.dp)
                        .heightIn(min = 150.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x1A335E78))
                ) {
                    // Left border bar: 3dp wide, rounded on the left
                    // to follow the card's corners.
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
                    Column(
                        verticalArrangement = Arrangement.spacedBy(11.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp)
                    ) {
                        // Title: 300 x 15, Inter 600 SemiBold 14/15, #335E78.
                        Text(
                            text = "CORE SCRIPTURE",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 15.sp,
                            letterSpacing = 0.sp,
                            color = Color(0xFF335E78),
                            maxLines = 1,
                            modifier = Modifier
                                .width(300.dp)
                                .heightIn(min = 18.dp)
                        )
                        // Verse frame: 300 x 85, gap 7.
                        Column(
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                            modifier = Modifier
                                .width(300.dp)
                                .heightIn(min = 102.dp)
                        ) {
                            // Verse: 300 x 60, Inter 400 Regular 14/20, #535353.
                            Text(
                                text = scriptureVerse,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                lineHeight = 20.sp,
                                letterSpacing = 0.sp,
                                color = Color(0xFF535353),
                                modifier = Modifier
                                    .width(300.dp)
                                    .heightIn(min = 72.dp)
                            )
                            // Reference: 300 x 18, Inter 600 SemiBold 14/18, #335E78.
                            Text(
                                text = scriptureReference,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 18.sp,
                                letterSpacing = 0.sp,
                                color = Color(0xFF335E78),
                                maxLines = 1,
                                modifier = Modifier
                                    .width(300.dp)
                                    .heightIn(min = 22.dp)
                            )
                        }
                    }
                }
                // Nav row: 215 x 30, flush right — dots + next arrow
                // (web footer: dots centered, arrow right).
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .padding(start = 120.dp)
                            .width(215.dp)
                            .height(30.dp)
                    ) {
                        // Dots: 74 x 10, gap 6, five 10px dots radius 20.
                        // First highlighted #335E78, rest #B3B3B3.
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
                                            if (index == currentSection) Color(0xFF335E78)
                                            else Color(0xFFB3B3B3)
                                        )
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = { onDotClick(index) }
                                        )
                                )
                            }
                        }
                        // Next arrow: 24 x 24, back-arrow image rotated -180.
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = "Next",
                            tint = Color(0xFF335E78),
                            modifier = Modifier
                                .size(24.dp)
                                .rotate(180f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onNext
                                )
                        )
                    }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun JourneyScreenPreview() {
    LampStandTheme(dynamicColor = false) {
        JourneyScreen()
    }
}
