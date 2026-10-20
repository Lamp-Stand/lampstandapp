package com.example.lampstand.journey

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
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lampstand.R
import com.example.lampstand.ui.theme.LampStandTheme

/**
 * Thought input: 336 x 80, radius 8, white, 0.5px #335E78 at 30%.
 * Placeholder 101 x 18 at top 14 / left 14, 14/18, #B3B3B3 at 73%.
 */
@Composable
private fun ThoughtInput(
    modifier: Modifier = Modifier
) {
    var value by remember { mutableStateOf("") }
    Box(
        modifier = modifier
            .width(336.dp)
            .height(80.dp)
            .border(0.5.dp, Color(0x4D335E78), RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(start = 14.dp, top = 14.dp, end = 14.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = { value = it },
            cursorBrush = SolidColor(Color(0xFF335E78)),
            textStyle = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 18.sp,
                letterSpacing = 0.sp,
                color = Color(0xFF1E1E1E)
            ),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = "Your thoughts..",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 18.sp,
                            letterSpacing = 0.sp,
                            color = Color(0xBBB3B3B3),
                            maxLines = 1,
                            modifier = Modifier
                                .width(101.dp)
                                .height(18.dp)
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

/**
 * One formation question block: question + input + hint.
 * Question text wraps (spec heights 35/29 clip two 18dp lines).
 */
@Composable
private fun FormationQuestion(
    question: String,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.width(336.dp)
    ) {
        // Question: 336 wide, Inter 600 SemiBold 14/18, #184159.
        Text(
            text = question,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 18.sp,
            letterSpacing = 0.sp,
            color = Color(0xFF184159),
            modifier = Modifier.width(336.dp)
        )
        ThoughtInput()
        // Hint: 170 x 18, Italic 12/18, #828282.
        Text(
            text = "Write your thoughts (optional)",
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            fontStyle = FontStyle.Italic,
            lineHeight = 18.sp,
            letterSpacing = 0.sp,
            color = Color(0xFF828282),
            maxLines = 1,
            modifier = Modifier
                .width(170.dp)
                .height(18.dp)
        )
    }
}

@Composable
private fun NavCircleButton(
    contentDescription: String,
    rotated: Boolean = false,
    onClick: () -> Unit = {}
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
                onClick = onClick
            )
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_arrow_back),
            contentDescription = contentDescription,
            tint = Color(0xFF335E78),
            modifier = if (rotated) Modifier.size(16.dp).rotate(180f)
            else Modifier.size(16.dp)
        )
    }
}

@Composable
private fun PageDots(
    highlighted: Int,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .width(74.dp)
            .height(10.dp)
    ) {
        repeat(5) { index ->
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (index == highlighted) Color(0xFF335E78)
                        else Color(0xFFB3B3B3)
                    )
            )
        }
    }
}

/**
 * Formation Questions (Journey screen 4 / web ApplicationSection):
 * personal application with thought inputs and page nav.
 * Questions come from live reflectionQuestions (web fallback trio
 * when the backend has none).
 */
@Composable
fun JourneyFormationScreen(
    onBack: () -> Unit = {},
    onNext: () -> Unit = {},
    // Web: top-left back links to path detail; footer arrow steps back
    // a section.
    onBackToPath: () -> Unit = {},
    questions: List<String> = emptyList()
) {
    // Web fallbacks when reflectionQuestions is empty.
    val effectiveQuestions = questions.filter { it.isNotBlank() }.ifEmpty {
        listOf(
            "What area of your life feels most uncertain right now?",
            "How do you usually respond when you don't have control?",
            "What would it look like to trust God in this situation?"
        )
    }
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
            // M2: 264 wide, top 125, left 19, gap 9 (24 below M1).
            // Height wraps so the heading never clips.
            Column(
                verticalArrangement = Arrangement.spacedBy(9.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 19.dp, top = 125.dp)
                    .width(264.dp)
            ) {
                // M2E1: 264 x 14, gap 4.
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
                        text = "PERSONAL APPLICATION",
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
                // M2E2: 264 wide, 20/26, #184159.
                Text(
                    text = "Formation Questions",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 26.sp,
                    letterSpacing = 0.sp,
                    color = Color(0xFF184159),
                    maxLines = 1,
                    modifier = Modifier.width(264.dp)
                )
            }
            // M3 + M4 flow: starts top 214 (44 below M2), left 19.
            // M3 blocks gap 20, M4 60 below M3, 139 bottom space.
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 19.dp, top = 214.dp)
                    .width(336.dp)
            ) {
                // M3: one block per live reflection question
                // (web ApplicationSection maps content.questions).
                effectiveQuestions.forEach { question ->
                    FormationQuestion(question = question)
                }
                // M3E4 nav row: prev + dots (4th) + next, 30 tall.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .width(336.dp)
                        .height(30.dp)
                ) {
                    NavCircleButton(contentDescription = "Previous", onClick = onBack)
                    PageDots(highlighted = 3)
                    NavCircleButton(contentDescription = "Next", rotated = true, onClick = onNext)
                }
                // M4 button 60 below M3 content: 336 x 42, radius 8,
                // padding 10/12, #335E78.
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(top = 40.dp)
                        .width(336.dp)
                        .height(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF335E78))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onNext
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Continue to Prayer",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 22.sp,
                        letterSpacing = 0.sp,
                        textAlign = TextAlign.Center,
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier
                            .width(143.dp)
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
fun JourneyFormationScreenPreview() {
    LampStandTheme(dynamicColor = false) {
        JourneyFormationScreen()
    }
}
