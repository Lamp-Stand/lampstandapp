package com.example.lampstand.journey

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lampstand.R
import com.example.lampstand.dashboard.BottomNavBar
import com.example.lampstand.ui.ScreenStatusBanner
import com.example.lampstand.ui.theme.LampStandTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val Primary = Color(0xFF335E78)
private val PrimaryInk = Color(0xFF184159)
private val BodyText = Color(0xFF535353)
private val SurfaceDark = Color(0xFF1E1E1E)
private val PageBackground = Color(0xFFFCFCFC)
private val LockedText = Color(0xFFB3B3B3)
private val ActiveChip = Color(0xFFD4DDE2)
private val ActiveChipText = Color(0xFF888888)
private val SuccessDeep = Color(0xFF2A7A5A)
private val SuccessSoft = Color(0xFFE8F4F0)

data class JourneyDashboardSession(
    val sessionId: String,
    val dayNumber: Int,
    val title: String,
    val status: String
)

private val defaultJourneyDashboardSessions = listOf(
    JourneyDashboardSession("1", 1, "God Sees the Path Ahead", "available"),
    JourneyDashboardSession("2", 2, "His Timing Is Never Late", "available"),
    JourneyDashboardSession("3", 3, "Guidance in the Wilderness", "available"),
    JourneyDashboardSession("4", 4, "Finding Stillness", "available"),
    JourneyDashboardSession("5", 5, "Trusting Beyond What You Feel", "locked"),
    JourneyDashboardSession("6", 6, "Letting Go of Self-Control", "locked"),
    JourneyDashboardSession("7", 7, "Resting in God\u2019s Sovereignty", "locked")
)

private data class JourneyScheduleDay(
    val dayName: String,
    val dateNumber: String,
    val isCurrent: Boolean
)

@Composable
private fun rememberScheduleDays(
    sessions: List<JourneyDashboardSession>,
    activeDay: Int
): List<JourneyScheduleDay> {
    return remember(sessions, activeDay) {
        val ordered = sessions.sortedBy { it.dayNumber }
        val firstDayNumber = ordered.firstOrNull()?.dayNumber ?: 1
        val baseCalendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, -(activeDay - firstDayNumber))
        }
        val dayFormatter = SimpleDateFormat("EEE", Locale.ENGLISH)
        ordered.map { session ->
            val dayCalendar = baseCalendar.clone() as Calendar
            dayCalendar.add(Calendar.DAY_OF_MONTH, session.dayNumber - firstDayNumber)
            JourneyScheduleDay(
                dayName = dayFormatter.format(dayCalendar.time),
                dateNumber = dayCalendar.get(Calendar.DAY_OF_MONTH).toString(),
                isCurrent = session.dayNumber == activeDay
            )
        }
    }
}

@Composable
private fun SessionStatusIcon(status: String) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val width = size.width
        val height = size.height
        when (status) {
            "completed" -> {
                drawCircle(color = SuccessSoft)
                drawLine(
                    color = SuccessDeep,
                    start = androidx.compose.ui.geometry.Offset(width * 0.30f, height * 0.52f),
                    end = androidx.compose.ui.geometry.Offset(width * 0.44f, height * 0.66f),
                    strokeWidth = width * 0.09f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = SuccessDeep,
                    start = androidx.compose.ui.geometry.Offset(width * 0.44f, height * 0.66f),
                    end = androidx.compose.ui.geometry.Offset(width * 0.72f, height * 0.36f),
                    strokeWidth = width * 0.09f,
                    cap = StrokeCap.Round
                )
            }
            "locked" -> {
                drawCircle(color = Color(0xFFD9D9D9))
                drawPath(
                    path = Path().apply {
                        moveTo(width * 0.38f, height * 0.30f)
                        lineTo(width * 0.70f, height * 0.50f)
                        lineTo(width * 0.38f, height * 0.70f)
                        close()
                    },
                    color = LockedText
                )
            }
            else -> {
                drawCircle(color = Primary)
                drawPath(
                    path = Path().apply {
                        moveTo(width * 0.38f, height * 0.30f)
                        lineTo(width * 0.70f, height * 0.50f)
                        lineTo(width * 0.38f, height * 0.70f)
                        close()
                    },
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun JourneySessionRow(
    session: JourneyDashboardSession,
    onClick: () -> Unit
) {
    val locked = session.status == "locked"
    val iconTint = if (locked) LockedText else Primary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 49.dp)
            .clickable(
                enabled = !locked,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(end = 24.dp, top = 4.dp, bottom = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
            modifier = Modifier.weight(1f)
        ) {
            Image(
                painter = painterResource(id = R.drawable.asterisk),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(iconTint),
                modifier = Modifier.size(12.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Session ${session.dayNumber}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 15.sp,
                    color = BodyText
                )
                Text(
                    text = session.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 17.sp,
                    color = if (locked) LockedText else SurfaceDark,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (session.status == "completed") {
                    Text(
                        text = "Completed",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 12.sp,
                        color = SuccessDeep
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        SessionStatusIcon(status = session.status)
    }
}

@Composable
fun JourneyDashboardScreen(
    onBack: () -> Unit = {},
    onItemSelected: (Int) -> Unit = {},
    onContinueClick: () -> Unit = {},
    onSessionClick: (String) -> Unit = {},
    title: String = "Trust in Uncertainty",
    description: String = "Learn to rest in God\u2019s unchanging character even when the path ahead is unclear or unsettling.",
    overviewText: String? = null,
    totalDays: Int = 7,
    sessions: List<JourneyDashboardSession> = defaultJourneyDashboardSessions,
    currentDay: Int? = null,
    isReviewing: Boolean = false,
    imageRes: Int = R.drawable.journey_illustration,
    selectedIndex: Int = 1,
    isLoading: Boolean = false,
    loadError: String? = null,
    onRetry: (() -> Unit)? = null
) {
    val resolvedActiveDay = currentDay
        ?: sessions.firstOrNull { it.status == "available" }?.dayNumber
        ?: sessions.firstOrNull()?.dayNumber
        ?: 1
    val scheduleDays = rememberScheduleDays(sessions = sessions, activeDay = resolvedActiveDay)
    val overviewBody = overviewText
        ?: "This $totalDays-day path is designed to help you rely on God\u2019s steady presence and character when life feels unclear or unpredictable."

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(466.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 0.dp,
                            topEnd = 0.dp,
                            bottomStart = 32.dp,
                            bottomEnd = 32.dp
                        )
                    )
                    .background(Color(0x26335E78))
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 20.dp, top = 20.dp)
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
                        tint = Primary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 106.dp)
                        .width(186.dp)
                        .height(206.dp)
                        .background(PageBackground)
                ) {
                    Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 24.sp,
                    letterSpacing = 0.sp,
                    textAlign = TextAlign.Center,
                    color = PrimaryInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 342.dp)
                        .width(300.dp)
                        .height(24.dp)
                )

                Text(
                    text = description,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 15.sp,
                    letterSpacing = 0.sp,
                    textAlign = TextAlign.Center,
                    color = BodyText,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 367.dp)
                        .width(205.dp)
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 59.dp, top = 445.dp)
                    .width(275.dp)
                    .height(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Primary)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onContinueClick
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(
                        12.dp,
                        Alignment.CenterHorizontally
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isReviewing) "Review Session" else "Continue Reading",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 22.sp,
                        letterSpacing = 0.sp,
                        color = Color.White,
                        maxLines = 1
                    )
                    Image(
                        painter = painterResource(id = R.drawable.ion_play_back),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(19.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 19.dp, top = 529.dp)
                    .width(358.dp)
            ) {
                Text(
                    text = "Reading Schedule",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 15.sp,
                    letterSpacing = 0.sp,
                    color = BodyText,
                    maxLines = 1
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .width(358.dp)
                        .height(59.dp)
                        .horizontalScroll(rememberScrollState())
                ) {
                    scheduleDays.forEach { day ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(1.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(53.dp)
                                .height(59.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (day.isCurrent) ActiveChip else Primary)
                                .padding(top = 8.dp)
                        ) {
                            Text(
                                text = day.dayName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                lineHeight = 15.sp,
                                letterSpacing = 0.sp,
                                color = if (day.isCurrent) ActiveChipText else Color.White,
                                maxLines = 1
                            )
                            Text(
                                text = day.dateNumber,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 15.sp,
                                letterSpacing = 0.sp,
                                color = if (day.isCurrent) ActiveChipText else Color.White,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 19.dp, top = 649.dp)
                    .width(334.dp)
            ) {
                Text(
                    text = "Overview",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 15.sp,
                    letterSpacing = 0.sp,
                    color = BodyText,
                    maxLines = 1
                )
                Text(
                    text = overviewBody,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 18.sp,
                    letterSpacing = 0.sp,
                    color = BodyText
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 19.dp, top = 773.dp, bottom = 48.dp)
                    .width(334.dp)
            ) {
                Text(
                    text = "Sessions",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 15.sp,
                    letterSpacing = 0.sp,
                    color = BodyText,
                    maxLines = 1
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.width(334.dp)
                ) {
                    if (sessions.isEmpty()) {
                        Text(
                            text = "No sessions available yet.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 18.sp,
                            color = BodyText
                        )
                    } else {
                        sessions.sortedBy { it.dayNumber }.forEach { session ->
                            JourneySessionRow(
                                session = session,
                                onClick = { onSessionClick(session.sessionId) }
                            )
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = LockedText,
                                modifier = Modifier.width(334.dp)
                            )
                        }
                    }
                }
            }
        }

        BottomNavBar(
            selectedIndex = selectedIndex,
            onItemSelected = onItemSelected
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun JourneyDashboardScreenPreview() {
    LampStandTheme(dynamicColor = false) {
        JourneyDashboardScreen(currentDay = 2)
    }
}
