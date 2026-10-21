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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.lampstandapp.app.R
import net.lampstandapp.app.ui.theme.LampStandTheme

/**
 * Category card: 335 x 111, radius 8, border 1px #E8E8E8.
 */
@Composable
fun CategoryCard(
    title: String = "Fear & Anxiety",
    subtitle: String = "6 formation paths",
    badge: String = "NOW AVAILABLE - START YOUR JOURNEY",
    buttonText: String = "View paths",
    onButtonClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .heightIn(min = 111.dp)
            .border(1.dp, Color(0xFFE8E8E8), RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top row: image 60x60 + middle texts + button 75x27
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, top = 8.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.available),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1E1E1E))
            )

            Column(
                modifier = Modifier
                    .padding(start = 7.dp)
                    .width(140.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 15.sp,
                    letterSpacing = 0.sp,
                    color = Color(0xFF184159),
                    maxLines = 1,
                    modifier = Modifier
                        .width(140.dp)
                        .heightIn(min = 18.dp)
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Light,
                    lineHeight = 15.sp,
                    letterSpacing = (-0.24).sp,
                    color = Color(0xFF184159),
                    maxLines = 1,
                    modifier = Modifier
                        .width(140.dp)
                        .heightIn(min = 18.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(75.dp)
                    .heightIn(min = 32.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF335E78))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onButtonClick
                    )
                    .padding(6.dp)
            ) {
                Text(
                    text = buttonText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 15.sp,
                    letterSpacing = 0.sp,
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier
                        .width(63.dp)
                        .heightIn(min = 18.dp)
                )
            }
        }

        // Divider: full-bleed inside card, 0.5px #E8E8E8
        HorizontalDivider(
            thickness = 0.5.dp,
            color = Color(0xFFE8E8E8)
        )

        Text(
            text = badge,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 15.sp,
            letterSpacing = 0.sp,
            color = Color(0xFF184159),
            maxLines = 1,
            modifier = Modifier
                .padding(start = 15.dp, top = 6.dp, bottom = 6.dp)
                .width(218.dp)
                .heightIn(min = 18.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 375)
@Composable
fun CategoryCardPreview() {
    LampStandTheme(dynamicColor = false) {
        CategoryCard()
    }
}
