package net.lampstandapp.app.journey

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
import net.lampstandapp.app.R
import net.lampstandapp.app.dashboard.BottomNavBar
import net.lampstandapp.app.ui.theme.LampStandTheme

private val Navy = Color(0xFF184159)
private val Blue = Color(0xFF335E78)

@Composable
private fun NextThemeCard(
    title: String,
    selected: Boolean,
    onToggle: () -> Unit
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
                onClick = onToggle
            )
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 20.sp,
            color = Blue,
            modifier = Modifier.weight(1f).padding(end = 12.dp)
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(24.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(if (selected) Blue else Color.Transparent)
                .border(1.5.dp, Blue, androidx.compose.foundation.shape.CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggle
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
 * Next journey screen (design/nextjourney.png):
 * "What themes would you like to explore next?"
 * Multi-select — user can pick as many as they like.
 */
@Composable
fun JourneyNextJourneyScreen(
    onItemSelected: (Int) -> Unit = {},
    onBack: () -> Unit = {},
    onSubmit: (Set<Int>) -> Unit = {}
) {
    // Web looking-forward starts with nothing selected — it is multi-select
    // and submission does not require a choice.
    var selected by remember { mutableStateOf(setOf<Int>()) }
    val themes = listOf(
        "Loss & Grief",
        "Identity & Worth",
        "Pressure & Endurance",
        "Relationships & Community"
    )

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
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_read_cv_logo),
                    contentDescription = null,
                    tint = Color(0xFF92ADBE),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "LOOKING FORWARD",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 14.sp,
                    letterSpacing = 1.sp,
                    color = Color(0xFF92ADBE)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "What themes would you like to explore next!",
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 34.sp,
                color = Navy
            )
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                text = "Please select as many as you would like",
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 20.sp,
                color = Blue
            )
            Spacer(modifier = Modifier.height(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                themes.forEachIndexed { index, title ->
                    NextThemeCard(
                        title = title,
                        selected = selected.contains(index),
                        onToggle = {
                            selected = if (selected.contains(index)) selected - index
                            else selected + index
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = { onSubmit(selected) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blue),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Submit", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
        BottomNavBar(selectedIndex = 2, onItemSelected = onItemSelected)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun JourneyNextJourneyScreenPreview() {
    LampStandTheme(dynamicColor = false) {
        JourneyNextJourneyScreen()
    }
}
