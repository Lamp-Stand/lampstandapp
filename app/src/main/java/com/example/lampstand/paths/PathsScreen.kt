package com.example.lampstand.paths

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lampstand.dashboard.BottomNavBar
import com.example.lampstand.ui.theme.LampStandTheme

@Composable
fun PathsScreen(
    onBack: () -> Unit = {},
    onItemSelected: (Int) -> Unit = {},
    onViewPathsClick: () -> Unit = {}
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        // Header immediately after native status bar: 375 x 64
        PathsTopBar(onBackClick = onBack)

        Spacer(modifier = Modifier.height(10.dp))

        // Section label: 217 x 24, top 118, left 21
        Text(
            text = "Available Categories",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 24.sp,
            letterSpacing = 0.sp,
            color = Color(0xFF335E78),
            maxLines = 1,
            modifier = Modifier
                .padding(start = 21.dp)
                .width(217.dp)
                .height(24.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Subtitle: 262 x 30, top 152, left 21
        Text(
            text = "Start your journey by exploring themes around life and purpose.",
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 15.sp,
            letterSpacing = 0.sp,
            color = Color(0xFF535353),
            maxLines = 2,
            modifier = Modifier
                .padding(start = 21.dp)
                .width(262.dp)
                .height(30.dp)
        )

        Spacer(modifier = Modifier.height(42.dp))

        // Category card: 335 x 111, top 224, left 20
        CategoryCard(
            onButtonClick = onViewPathsClick
        )

        // Blank content for now
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
        )

        BottomNavBar(
            selectedIndex = 1,
            onItemSelected = onItemSelected
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PathsScreenPreview() {
    LampStandTheme(dynamicColor = false) {
        PathsScreen()
    }
}
