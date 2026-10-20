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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lampstand.R
import com.example.lampstand.dashboard.BottomNavBar
import com.example.lampstand.ui.theme.LampStandTheme

private val Navy = Color(0xFF184159)
private val Blue = Color(0xFF335E78)

@Composable
private fun CompletionOptionRow(
    text: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, Color(0xFFB3B3B3), RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFFCFCFC))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSelect
            )
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            color = Blue,
            modifier = Modifier.weight(1f).padding(end = 12.dp)
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(22.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(if (selected) Blue else Color.Transparent)
                .border(1.5.dp, Blue, androidx.compose.foundation.shape.CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSelect
                )
        ) {
            if (selected) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(12.dp)) {
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

@Composable
private fun CompletionQuestion(
    question: String,
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Text(
        text = question,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 17.sp,
        color = Navy
    )
    Spacer(modifier = Modifier.height(10.dp))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEachIndexed { index, text ->
            CompletionOptionRow(
                text = text,
                selected = selected == index,
                onSelect = { onSelect(index) }
            )
        }
    }
}

@Composable
fun JourneyCompletionReviewScreen(
    onItemSelected: (Int) -> Unit = {},
    onBack: () -> Unit = {},
    onSubmit: (q1: Int, q2: Int, q3: Int, notes: String) -> Unit = { _, _, _, _ -> }
) {
    var q1 by remember { mutableStateOf(-1) }
    var q2 by remember { mutableStateOf(-1) }
    var q3 by remember { mutableStateOf(-1) }
    var notes by remember { mutableStateOf("") }

    // Web parity: advancing requires the Finish tap, not auto-submit.

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
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
            Spacer(modifier = Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Navy)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "JOURNEY COMPLETE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Text(
                    "DAY 7/7",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Navy
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "You've completed your first journey.",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 26.sp,
                color = Navy
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "We'd love to hear from you. This takes under two minutes.",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = Color(0xFF535353)
            )
            Spacer(modifier = Modifier.height(20.dp))
            CompletionQuestion(
                question = "After 7 days, which of these feels most true for you?",
                options = listOf(
                    "I'm more grounded",
                    "Much deeper than anything I've used before",
                    "I understand God more",
                    "I'm not sure",
                    "I don't notice a difference"
                ),
                selected = q1,
                onSelect = { q1 = it }
            )
            Spacer(modifier = Modifier.height(24.dp))
            CompletionQuestion(
                question = "How does Lampstand compare with other ways you've engaged with scripture?",
                options = listOf(
                    "Much deeper than anything I've used before",
                    "Similar to other things I've tried",
                    "Not quite what I expected",
                    "This is relatively new for me"
                ),
                selected = q2,
                onSelect = { q2 = it }
            )
            Spacer(modifier = Modifier.height(24.dp))
            CompletionQuestion(
                question = "What would you like to do next?",
                options = listOf(
                    "Start another formation path",
                    "I need a break — I'll come back",
                    "I'm not sure yet"
                ),
                selected = q3,
                onSelect = { q3 = it }
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Is there anything you'd like to share about your experience — what helped, what didn't, or what you're still carrying?",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 17.sp,
                color = Navy
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                placeholder = { Text("Share my thoughts...", color = Color(0xFFB3B3B3), fontSize = 13.sp) },
                minLines = 4,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF92ADBE),
                    unfocusedBorderColor = Color(0xFFD0D7DC),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().height(130.dp)
            )
            // Finish CTA: 35dp below thoughts box, 109dp above bottom nav.
            Spacer(modifier = Modifier.height(35.dp))
            Button(
                onClick = { onSubmit(q1, q2, q3, notes) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blue),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Finish", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(109.dp))
        }
        BottomNavBar(selectedIndex = 2, onItemSelected = onItemSelected)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun JourneyCompletionReviewPreview() {
    LampStandTheme(dynamicColor = false) {
        JourneyCompletionReviewScreen()
    }
}
