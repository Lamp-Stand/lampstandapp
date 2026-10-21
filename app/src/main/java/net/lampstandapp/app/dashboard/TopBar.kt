package net.lampstandapp.app.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.lampstandapp.app.R
import net.lampstandapp.app.ui.theme.LampStandTheme

/**
 * Top bar placed immediately after the native status bar (top: 44px).
 *
 * Figma spec:
 * - width: 375 (fillMaxWidth, edge-to-edge, left: -1px)
 * - height: 64
 * - padding: 20 (horizontal 20dp; vertical centered to preserve 64dp height
 *   with two-line greeting text)
 * - gap: 35 (between greeting block and action block)
 */
@Composable
fun TopBar(
    greeting: String = "HI, USER",
    title: String = "Welcome Back",
    notificationCount: Int = 1,
    onNotificationClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(35.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = greeting,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 14.sp,
                color = Color(0xFF92ADBE)
            )
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 24.sp,
                color = Color(0xFF335E78),
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.clickable(onClick = onNotificationClick)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_notification),
                contentDescription = "Notifications",
                tint = Color(0xFF335E78),
                modifier = Modifier.size(24.dp)
            )
            if (notificationCount > 0) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-4).dp, y = (-4).dp)
                        .size(14.dp)
                        .background(Color(0xFFE5484D), CircleShape)
                ) {
                    Text(
                        text = notificationCount.toString(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 10.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 375, heightDp = 64)
@Composable
fun TopBarPreview() {
    LampStandTheme(dynamicColor = false) {
        TopBar()
    }
}
