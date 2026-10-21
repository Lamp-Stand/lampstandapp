package net.lampstandapp.app.paths

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.lampstandapp.app.R
import net.lampstandapp.app.ui.theme.LampStandTheme
import kotlinx.coroutines.delay

/**
 * Search bar: 339 x 40, radius 4, border 1px #E8E8E8.
 * Icon 18x18 (top 11, left 11), placeholder 111x15 "Search Topics..."
 * 14sp Regular #B3B3B3.
 */
@Composable
fun PathSearchBar(
    query: String = "",
    onQueryChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .width(339.dp)
            .height(40.dp)
            .border(1.dp, Color(0xFFE8E8E8), RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White)
            .padding(start = 11.dp, end = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.search),
            contentDescription = "Search",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(18.dp)
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            cursorBrush = SolidColor(Color(0xFF335E78)),
            textStyle = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 15.sp,
                letterSpacing = 0.sp,
                color = Color(0xFF1E1E1E)
            ),
            modifier = Modifier
                .width(111.dp)
                .heightIn(min = 18.dp),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search Topics...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 15.sp,
                            letterSpacing = 0.sp,
                            color = Color(0xFFB3B3B3),
                            maxLines = 1
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

/**
 * Featured path card: 339 x 124, padding 12, gap 10, radius 8,
 * border 1px #E8E8E8. Inner content 205 x 98 holding the
 * 145 x 40 block with the calendar + "7 days" badge.
 */
@Composable
fun FeaturedPathCard(
    imageRes: Int? = null,
    modifier: Modifier = Modifier,
    badgeText: String = "7 days",
    title: String = "Trust in Uncertainty",
    description: String = "For when the future feels unclear and you\u2019re trying to trust God one step at a time.",
    availableBadge: Boolean = false,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .width(339.dp)
            .heightIn(min = 124.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFE8E8E8), RoundedCornerShape(8.dp))
            .background(Color.White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Smaller container: 205 x 98, gap 10
            Column(
                modifier = Modifier
                    .width(205.dp)
                    .heightIn(min = 98.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Even smaller container: 185 x 40 (widened from 145 so
                // longer headings like "When Anxiety Won't Stop" fit in one line)
                Column(
                    modifier = Modifier
                        .width(185.dp)
                        .heightIn(min = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // Single badge row (not duplicated): icon + text, wraps content
                    // so "7 days" is never clipped to just "7".
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.heightIn(min = 18.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.calendar),
                            contentDescription = null,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = badgeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 15.sp,
                            letterSpacing = 0.sp,
                            color = Color(0xFF335E78),
                            maxLines = 1
                        )
                    }
                    // Title under the badge: 185 x 20, Inter 600 SemiBold 15/20, #535353.
                    // Longer titles auto-shrink so they still enter in one line.
                    Text(
                        text = title,
                        fontSize = if (title.length > 22) 12.sp else 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 20.sp,
                        letterSpacing = 0.sp,
                        color = Color(0xFF535353),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier
                            .width(185.dp)
                            .heightIn(min = 24.dp)
                    )
                }
                // Description beneath title: 205 x 48, Inter 400 Regular 12/16, #535353.
                Text(
                    text = description,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 16.sp,
                    letterSpacing = 0.sp,
                    color = Color(0xFF535353),
                    modifier = Modifier
                        .width(205.dp)
                        .heightIn(min = 57.dp)
                )
            }

            // Right side: 98 x 98 image, radius 4, on #1E1E1E
            if (imageRes != null) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(98.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF1E1E1E))
                )
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
        }

        // Web: live paths carry an "Available Now" chip pinned to the card's
        // top-right corner instead of a text line above the card.
        if (availableBadge) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(Color(0xFF335E78), RoundedCornerShape(bottomStart = 4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
                Text(
                    text = "Available Now",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 15.sp,
                    letterSpacing = 0.sp,
                    color = Color.White,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Coming-soon path card: same 339-wide shell as the live card, but the web's
 * structure — interest label above the title, live counter under the
 * description, tap-to-express-interest, and an inline confirmation strip
 * pinned to the bottom of the card for 3.5s.
 *
 * Height wraps (min 124) because the label line can wrap, exactly as it does
 * on the web card.
 */
@Composable
fun ComingSoonPathCard(
    imageRes: Int? = null,
    modifier: Modifier = Modifier,
    title: String = "Trust in Uncertainty",
    description: String = "For when the future feels unclear and you\u2019re trying to trust God one step at a time.",
    totalTaps: Int = 0,
    userHasTapped: Boolean = false,
    onTap: (onResult: (Boolean) -> Unit) -> Unit = { it(false) }
) {
    var tapped by remember { mutableStateOf(userHasTapped) }
    var localCount by remember { mutableStateOf(totalTaps) }
    var showConfirmation by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Web: the confirmation strip hides itself after 3.5s.
    LaunchedEffect(showConfirmation) {
        if (showConfirmation) {
            delay(3500)
            showConfirmation = false
        }
    }

    Box(
        modifier = modifier
            .width(339.dp)
            .heightIn(min = 124.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                1.dp,
                if (tapped) Color(0xFFE5E5E5) else Color(0xFFE8E8E8),
                RoundedCornerShape(8.dp)
            )
            .background(if (tapped) Color(0xFFFAFAFA) else Color.White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (!tapped && !isLoading) {
                        isLoading = true
                        // Optimistic, then rolled back if the mutation fails.
                        tapped = true
                        localCount += 1
                        onTap { ok ->
                            if (ok) {
                                showConfirmation = true
                            } else {
                                tapped = false
                                localCount -= 1
                            }
                            isLoading = false
                        }
                    }
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.width(205.dp)) {
                Text(
                    text = "Coming Soon — Tap to Express Interest",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 15.sp,
                    letterSpacing = 0.sp,
                    color = Color(0xFF335E78),
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = title,
                    fontSize = if (title.length > 22) 12.sp else 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 20.sp,
                    letterSpacing = 0.sp,
                    color = Color(0xFF535353),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 16.sp,
                    letterSpacing = 0.sp,
                    color = Color(0xFF535353),
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_people),
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = if (localCount > 0) {
                            "$localCount ${if (localCount == 1) "person is" else "people are"} waiting"
                        } else {
                            "Be the first to express interest"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 15.sp,
                        letterSpacing = 0.sp,
                        color = Color(0xFF335E78),
                        maxLines = 1
                    )
                }
            }

            if (imageRes != null) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(86.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E1E1E))
                )
            }
        }

        // Web: inline confirmation toast anchored to the bottom of the card.
        if (showConfirmation) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                    .background(Color(0xFF335E78))
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_check),
                    contentDescription = null,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "We've noted your interest. You'll be notified.",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 15.sp,
                    letterSpacing = 0.sp,
                    color = Color.White,
                    maxLines = 1
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 375)
@Composable
fun FandAPathContentPreview() {
    LampStandTheme(dynamicColor = false) {
        Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
            PathSearchBar()
            FeaturedPathCard(availableBadge = true)
            ComingSoonPathCard()
        }
    }
}
