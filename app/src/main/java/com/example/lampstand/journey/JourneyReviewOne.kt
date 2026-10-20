package com.example.lampstand.journey

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lampstand.R
import com.example.lampstand.dashboard.BottomNavBar
import com.example.lampstand.ui.theme.LampStandTheme

private data class ReviewOption(
    val imageRes: Int,
    val title: String,
    val titleWidth: Int,
    val subtitle: String,
    val subtitleWidth: Int
)

/**
 * One selectable review card: 336 x 64, radius 4, #FCFCFC,
 * 0.5px #B3B3B3 border, padding 8/20. Emoji 33 + text + radio 24.
 */
@Composable
private fun ReviewOptionCard(
    option: ReviewOption,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .width(336.dp)
            .height(64.dp)
            .border(0.5.dp, Color(0xFFB3B3B3), RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFFCFCFC))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSelect
            )
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = option.imageRes),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(33.dp)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Title: 14sp Medium, 100% line-height, #335E78.
                Text(
                    text = option.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 14.sp,
                    letterSpacing = 0.sp,
                    color = Color(0xFF335E78),
                    maxLines = 1,
                    modifier = Modifier
                        .width(option.titleWidth.dp)
                        .height(17.dp)
                )
                // Subtitle: 12sp Regular, 100% line-height, #535353.
                Text(
                    text = option.subtitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 12.sp,
                    letterSpacing = 0.sp,
                    color = Color(0xFF535353),
                    maxLines = 1,
                    modifier = Modifier
                        .width(option.subtitleWidth.dp)
                        .height(15.dp)
                )
            }
        }
        // Radio: 24 x 24 — empty ring, or filled + white check when selected
        // (matches design/reviewone.png).
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(24.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(if (selected) Color(0xFF335E78) else Color.Transparent)
                .border(
                    1.5.dp,
                    Color(0xFF335E78),
                    androidx.compose.foundation.shape.CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSelect
                )
        ) {
            if (selected) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(14.dp)) {
                    val stroke = size.minDimension * 0.18f
                    drawLine(
                        color = androidx.compose.ui.graphics.Color.White,
                        start = androidx.compose.ui.geometry.Offset(size.width * 0.2f, size.height * 0.55f),
                        end = androidx.compose.ui.geometry.Offset(size.width * 0.45f, size.height * 0.75f),
                        strokeWidth = stroke,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    drawLine(
                        color = androidx.compose.ui.graphics.Color.White,
                        start = androidx.compose.ui.geometry.Offset(size.width * 0.43f, size.height * 0.75f),
                        end = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.3f),
                        strokeWidth = stroke,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
        }
    }
}

/**
 * Journey review page 1: "One quick question before you go".
 */
@Composable
fun JourneyReviewScreen(
    onItemSelected: (Int) -> Unit = {},
    onBack: () -> Unit = {},
    onOptionSelected: (Int) -> Unit = {},
    onBackToHome: () -> Unit = {}
) {
    var selected by remember { mutableStateOf(-1) }
    var thoughts by remember { mutableStateOf("") }
    val options = listOf(
        ReviewOption(
            imageRes = R.drawable.sad_solid,
            title = "Didn't connect",
            titleWidth = 109,
            subtitle = "It wasn't what I needed today",
            subtitleWidth = 168
        ),
        ReviewOption(
            imageRes = R.drawable.smile,
            title = "It was helpful",
            titleWidth = 90,
            subtitle = "It provided some quiet clarity",
            subtitleWidth = 165
        ),
        ReviewOption(
            imageRes = R.drawable.happy_fill,
            title = "It landed deeply",
            titleWidth = 108,
            subtitle = "Exactly what my soul needed",
            subtitleWidth = 166
        )
    )

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
            // M1 back: 30 x 30, top 71, left 19, radius 16.
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
            // M2: 263 x 14, top 125, left 20, gap 4.
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
                    text = "HOW DID IT GO?",
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
            // Web: the h1 is only "How did today's session land for you?" —
            // there is no "One quick question before you go" heading above it.
            Text(
                text = "How did today's session land for you?",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 26.sp,
                letterSpacing = 0.sp,
                color = Color(0xFF184159),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 20.dp, top = 148.dp)
                    .width(335.dp)
            )
            // M5: 336 x 216, top 242 (web leaves 42 after the h1), left 20, gap 12.
            // 3 x 64 cards + 2 x 12 gaps = 216.
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 20.dp, top = 242.dp)
                    .width(336.dp)
                    .height(216.dp)
            ) {
                options.forEachIndexed { index, option ->
                    ReviewOptionCard(
                        option = option,
                        selected = selected == index,
                        onSelect = {
                            selected = index
                            onOptionSelected(index)
                        }
                    )
                }
            }
            // Web only renders the optional thoughts block once an option
            // is chosen (`{selected && (…)}`).
            if (selected >= 0) {
                // M6: optional prompt, below cards.
                Text(
                    text = "Is there anything else you want to tell us about today? (Optional)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 18.sp,
                    color = Color(0xFF184159),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 20.dp, top = 482.dp)
                        .width(316.dp)
                )
                // M7: thoughts input.
                OutlinedTextField(
                    value = thoughts,
                    onValueChange = { thoughts = it },
                    placeholder = { Text("Your thoughts..", color = Color(0xFFB3B3B3), fontSize = 14.sp) },
                    minLines = 5,
                    maxLines = 6,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF92ADBE),
                        unfocusedBorderColor = Color(0xFFD0D7DC),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 20.dp, top = 554.dp)
                        .width(336.dp)
                        .height(150.dp)
                )
            }
            // M8: feedback CTA (web "Continue to Completed" → /completed).
            Button(
                onClick = onBackToHome,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF335E78)),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 20.dp, top = 728.dp)
                    .width(336.dp)
                    .height(52.dp)
            ) {
                Text("Continue to Completed", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
            // Bottom breathing room so CTA never sits under navbar.
            Spacer(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 780.dp)
                    .height(40.dp)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun JourneyReviewScreenPreview() {
    LampStandTheme(dynamicColor = false) {
        JourneyReviewScreen()
    }
}
