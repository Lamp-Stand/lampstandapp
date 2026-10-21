package net.lampstandapp.app.paths

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.lampstandapp.app.R
import net.lampstandapp.app.ui.theme.LampStandTheme

/**
 * Paths header, placed immediately after the native status bar.
 *
 * Container spec: 375 x 64, top 44, left -1, padding 20, gap 35.
 * Math: 375 - 20*2 = 335 content; 24 (back) + 35 (gap) + 217 (title)
 * + 35 (gap) + 24 (balance spacer) = 335, so the title stays truly centered.
 *
 * Left icon spec: 24 x 24.
 * Center content spec: 217 x 24, "Formation Paths", Inter Medium 500,
 * 18sp / 24sp, center, #335E78.
 */
@Composable
fun PathsTopBar(
    title: String = "Formation Paths",
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .background(Color.White)
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(35.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_arrow_back),
            contentDescription = "Back",
            tint = Color(0xFF335E78),
            modifier = Modifier
                .size(24.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onBackClick
                )
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 30.dp)
        ) {
            if (title.isNotEmpty()) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 24.sp,
                letterSpacing = 0.sp,
                textAlign = TextAlign.Center,
                color = Color(0xFF335E78),
                maxLines = 1,
                modifier = Modifier.width(217.dp)
            )
            }
        }

        // Invisible balance so the 217-wide title stays optically centered
        Spacer(modifier = Modifier.size(24.dp))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 375, heightDp = 64)
@Composable
fun PathsTopBarPreview() {
    LampStandTheme(dynamicColor = false) {
        PathsTopBar()
    }
}
