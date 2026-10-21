package net.lampstandapp.app.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.lampstandapp.app.R
import net.lampstandapp.app.dashboard.BottomNavBar
import net.lampstandapp.app.ui.theme.LampStandTheme

private val Navy = Color(0xFF184159)
private val Blue = Color(0xFF335E78)

/**
 * Signed-out dashboard (design/dashboardsignedout.png).
 *
 * Generic greeting ("HI, THERE") with no user data — the entry
 * home shown before sign-in. Lives in the splash package so it can
 * be shown straight after [LoadingScreen].
 */
@Composable
fun DashboardSignedOutScreen(
    onItemSelected: (Int) -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onContinueReading: () -> Unit = {},
    onRecommendedClick: () -> Unit = {},
    onSignInClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // Header: generic greeting + sign-in entry + notification bell.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "HI, THERE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 14.sp,
                        letterSpacing = 1.sp,
                        color = Color(0xFF92ADBE)
                    )
                    Text(
                        text = "Welcome",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 18.sp,
                        color = Blue,
                        modifier = Modifier.clickable(onClick = onSignInClick)
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.clickable(onClick = onNotificationClick)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_notification),
                        contentDescription = "Notifications",
                        tint = Blue,
                        modifier = Modifier.size(24.dp)
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = (-4).dp, y = (-4).dp)
                            .size(14.dp)
                            .background(Color(0xFFE5484D), CircleShape)
                    ) {
                        Text(
                            text = "1",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 10.sp,
                            color = Color.White
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hero formation-path card.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.shipsail),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    )
                    Column(modifier = Modifier.padding(16.dp)) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .background(Color(0xFFE7F6FF), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "NO ACTIVE PATH",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 14.sp,
                                color = Blue
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_read_cv_logo),
                                contentDescription = null,
                                tint = Color(0xFF92ADBE),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "FORMATION IN PROGRESS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 14.sp,
                                letterSpacing = 1.sp,
                                color = Color(0xFF92ADBE)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Trust in Uncertainty",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 28.sp,
                            color = Navy
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .background(Blue, RoundedCornerShape(4.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "Day 1: Naming the Struggle . 7 min",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 15.sp,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "When things are uncertain, it's natural to want control and try to figure everything out before moving forward. But this scripture challenges that habit.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 19.sp,
                            color = Color(0xFF535353)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onContinueReading,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Blue),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text(
                                "Explore Formation Paths",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Today's scripture card.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x1A335E78))
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(3.dp)
                            .fillMaxHeight()
                            .background(
                                Blue,
                                RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)
                            )
                    )
                    Column(modifier = Modifier.padding(start = 16.dp, top = 14.dp, end = 12.dp, bottom = 14.dp)) {
                        Text(
                            text = "TODAY'S SCRIPTURE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 15.sp,
                            color = Blue
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "\"Be still, and know that I am God; I will be exalted among the nations, I will be exalted in the earth.\"",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 19.sp,
                            color = Color(0xFF535353)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Psalm 46:10 (NIV)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 16.sp,
                            color = Blue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Recommended For You",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 20.sp,
                    color = Color(0xFF1E1E1E),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Image(
                    painter = painterResource(id = R.drawable.recommended),
                    contentDescription = "Recommended",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .width(146.dp)
                        .height(121.dp)
                        .background(Color(0xFFD4DDE4), RoundedCornerShape(4.dp))
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(onClick = onRecommendedClick)
                        .align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier.width(146.dp).align(Alignment.Start),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Fear & Anxiety",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 16.sp,
                        color = Color(0xFF1E1E1E)
                    )
                    Text(
                        text = "6 Formation Paths",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 14.sp,
                        color = Color(0xFF828282)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        BottomNavBar(selectedIndex = 0, onItemSelected = onItemSelected)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DashboardSignedOutScreenPreview() {
    LampStandTheme(dynamicColor = false) {
        DashboardSignedOutScreen()
    }
}
