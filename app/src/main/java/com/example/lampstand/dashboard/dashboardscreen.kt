package com.example.lampstand.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lampstand.R
import com.example.lampstand.ui.ScreenStatusBanner
import com.example.lampstand.ui.theme.LampStandTheme

private val Primary = Color(0xFF335E78)
private val PrimaryDark = Color(0xFF264E68)
private val MutedBlue = Color(0xFF8AA0B2)
private val LightPillBg = Color(0xFFE8F3FA)
private val BodyGray = Color(0xFF6B7280)
private val NearBlack = Color(0xFF1F2937)
private val SubGray = Color(0xFF9CA3AF)
private val ScriptureBg = Color(0xFFEDF2F7)
private val CardBorder = Color(0xFFE5E7EB)

@Composable
fun DashboardScreen(
    onItemSelected: (Int) -> Unit = {},
    userName: String = "there",
    onContinueClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onRecommendedClick: () -> Unit = {},
    // Live data from paths:getHomeState (web parity). Defaults = previous hardcoded UI.
    welcomeText: String = "Welcome Back",
    pillText: String = "ACTIVE PATH . DAY 4 OF 7",
    pathTitle: String = "Trust in Uncertainty",
    dayChipText: String = "Day 4: Naming the Struggle . 7 min",
    primaryButtonText: String = "Continue Today’s Session",
    recommendationCount: Int = 6,
    isLoading: Boolean = false,
    loadError: String? = null,
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .padding(top = 16.dp)
    ) {
        TopBar(
            greeting = "HI, ${userName.uppercase()}",
            title = welcomeText,
            notificationCount = 1,
            onNotificationClick = onNotificationClick
        )
        ScreenStatusBanner(
            message = loadError,
            loading = isLoading,
            onRetry = onRetry
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ActivePathCard(
                onContinueClick = onContinueClick,
                pillText = pillText,
                pathTitle = pathTitle,
                dayChipText = dayChipText,
                primaryButtonText = primaryButtonText
            )
            ScriptureCard()
            RecommendedSection(count = recommendationCount, onClick = onRecommendedClick)
            Spacer(modifier = Modifier.height(4.dp))
        }

        BottomNavBar(
            selectedIndex = 0,
            onItemSelected = onItemSelected
        )
    }
}

@Composable
private fun ActivePathCard(
    onContinueClick: () -> Unit,
    pillText: String,
    pathTitle: String,
    dayChipText: String,
    primaryButtonText: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
    ) {
        // Hero illustration — matches design/Dashboardscreen.png top banner
        Image(
            painter = painterResource(id = R.drawable.shipsail),
            contentDescription = "Trust in Uncertainty illustration",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(175.dp)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(Color(0xFFDCE7EF))
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .background(LightPillBg, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = pillText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp,
                    color = Primary,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.heightIn(min = 14.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_read_cv_logo),
                    contentDescription = null,
                    tint = MutedBlue,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "FORMATION IN PROGRESS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    color = MutedBlue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = pathTitle,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 26.sp,
                color = PrimaryDark,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .background(Primary, RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = dayChipText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "When things are uncertain, it's natural to want control and try to figure everything out before moving forward. But this scripture challenges that habit.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 20.sp,
                color = BodyGray,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(Primary, RoundedCornerShape(10.dp))
                    .clickable(onClick = onContinueClick)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = primaryButtonText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ScriptureCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ScriptureBg, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(140.dp)
                .align(Alignment.CenterStart)
                .background(Primary, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, top = 14.dp, end = 14.dp, bottom = 14.dp)
        ) {
            Text(
                text = "TODAY\u2019S SCRIPTURE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp,
                color = Primary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "\"Be still, and know that I am God; I will be exalted among the nations, I will be exalted in the earth.\"",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 20.sp,
                color = NearBlack,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Psalm 46:10 (NIV)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Primary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RecommendedSection(count: Int = 6, onClick: () -> Unit = {}) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Recommended For You",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = NearBlack,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 20.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Image(
            painter = painterResource(id = R.drawable.recommended),
            contentDescription = "Fear & Anxiety",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(160.dp)
                .height(120.dp)
                .background(Color(0xFFDCE7EF), RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier.widthIn(max = 180.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Fear & Anxiety",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = NearBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$count Formation Paths",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = SubGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DashboardScreenPreview() {
    LampStandTheme(dynamicColor = false) {
        // Preview-only offset: @Preview reports 0 status-bar insets, so the
        // simulated system bar would cover TopBar. On a real device
        // statusBarsPadding() inside DashboardScreen handles this.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(top = 28.dp)
        ) {
            DashboardScreen()
        }
    }
}
