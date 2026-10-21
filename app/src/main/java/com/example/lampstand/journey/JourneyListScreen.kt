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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.lampstand.ui.ScreenStatusBanner
import com.example.lampstand.ui.theme.LampStandTheme

/**
 * Web app/my-journey/page.tsx — the Journey tab's entry screen.
 *
 * Heading + one card per path, or the empty state when there is no active
 * path. Card tap pushes the path detail (web's /my-journey/[id]).
 */
@Composable
fun JourneyListScreen(
    onItemSelected: (Int) -> Unit = {},
    onPathClick: (String) -> Unit = {},
    onGoToPaths: () -> Unit = {},
    pathId: String? = null,
    pathTitle: String = "",
    pathDescription: String = "",
    durationDays: Int = 7,
    imageRes: Int = R.drawable.shipsail,
    isLoading: Boolean = false,
    loadError: String? = null,
    onRetry: (() -> Unit)? = null
) {
    val hasPath = pathId != null && pathTitle.isNotBlank()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        ScreenStatusBanner(
            message = loadError,
            loading = isLoading,
            onRetry = onRetry
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            if (hasPath) {
                Column(
                    modifier = Modifier
                        .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 20.dp)
                        .fillMaxWidth()
                ) {
                    // Web h1: text-lg semibold, leading 26, mb 20.
                    Text(
                        text = "My Journey",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 26.sp,
                        letterSpacing = 0.sp,
                        color = Color(0xFF184159),
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                    JourneyPathCard(
                        title = pathTitle,
                        description = pathDescription,
                        durationDays = durationDays,
                        imageRes = imageRes,
                        onClick = { pathId?.let(onPathClick) }
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 36.dp)
                ) {
                    Text(
                        text = "No path selected",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 24.sp,
                        letterSpacing = 0.sp,
                        color = Color(0xFF184159)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sorry you do not have an active path selected.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 20.sp,
                        letterSpacing = 0.sp,
                        color = Color(0xFF535353),
                        modifier = Modifier.padding(bottom = 78.dp)
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .width(275.dp)
                            .height(52.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF335E78))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onGoToPaths
                            )
                    ) {
                        Text(
                            text = "Go to Paths",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        BottomNavBar(
            selectedIndex = 2,
            onItemSelected = onItemSelected
        )
    }
}

/**
 * Web PathCard: bordered row, text column on the left (duration badge,
 * title, description) and an 85x85 thumbnail inset by 9 on the right.
 */
@Composable
fun JourneyPathCard(
    title: String,
    description: String,
    durationDays: Int,
    imageRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .width(335.dp)
            .height(103.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFE8E8E8), RoundedCornerShape(8.dp))
            .background(Color.White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 13.dp, top = 9.dp, end = 6.dp, bottom = 9.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.calendar),
                    contentDescription = null,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "$durationDays days",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 15.sp,
                    letterSpacing = 0.sp,
                    color = Color(0xFF335E78)
                )
            }
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 20.sp,
                letterSpacing = 0.sp,
                color = Color(0xFF535353),
                maxLines = 1
            )
            Text(
                text = description,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 15.sp,
                letterSpacing = 0.sp,
                color = Color(0xFF535353),
                maxLines = 2
            )
        }




        Image(
            painter = painterResource(id = imageRes),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .padding(9.dp)
                .size(85.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF1E1E1E))
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun JourneyListScreenPreview() {
    LampStandTheme(dynamicColor = false) {
        JourneyListScreen(
            pathId = "1",
            pathTitle = "Trust in Uncertainty",
            pathDescription = "Grow in understanding God's character through these curated paths"
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 375)
@Composable
fun JourneyListEmptyPreview() {
    LampStandTheme(dynamicColor = false) {
        JourneyListScreen()
    }
}
