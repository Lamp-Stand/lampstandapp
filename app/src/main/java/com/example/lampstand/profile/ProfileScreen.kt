package com.example.lampstand.profile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lampstand.R
import com.example.lampstand.dashboard.BottomNavBar
import com.example.lampstand.ui.theme.LampStandTheme

private val Blue = Color(0xFF335E78)
private val BodyGrey = Color(0xFF5A5A5A)

@Composable
private fun ProfileRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 18.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 26.sp,
                color = Blue
            )
            Text(
                text = subtitle,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 20.sp,
                color = BodyGrey
            )
        }
        Icon(
            painter = painterResource(id = R.drawable.ic_arrow_back),
            contentDescription = null,
            tint = Blue,
            modifier = Modifier.size(20.dp).rotate(180f)
        )
    }
}

/**
 * Profile screen (design/profilescreen.png).
 */
@Composable
fun ProfileScreen(
    name: String = "Anibe Achema",
    email: String = "achemaanibe@gmail.com",
    initials: String = "AA",
    onItemSelected: (Int) -> Unit = {},
    onBack: () -> Unit = {},
    onPersonalInfoClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {},
    onTermsClick: () -> Unit = {},
    onLogOutClick: () -> Unit = {}
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
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Profile",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 30.sp,
                color = Blue
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Manage your Lampstand Profile from here.",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 22.sp,
                color = BodyGrey
            )
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Blue)
                ) {
                    Text(
                        text = initials,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 26.sp,
                        color = Blue
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "\u2709",
                            fontSize = 18.sp,
                            color = Blue
                        )
                        Text(
                            text = email,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 20.sp,
                            color = Blue
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            ProfileRow(
                title = "Personal Information",
                subtitle = "Edit your personal details.",
                onClick = onPersonalInfoClick
            )
            HorizontalDivider(thickness = 1.dp, color = Color(0xFF3A3A3A))
            ProfileRow(
                title = "Privacy Policy",
                subtitle = "How your data is handled.",
                onClick = onPrivacyClick
            )
            HorizontalDivider(thickness = 1.dp, color = Color(0xFF3A3A3A))
            ProfileRow(
                title = "Terms of Service",
                subtitle = "The rules for using Lampstand.",
                onClick = onTermsClick
            )
            HorizontalDivider(thickness = 1.dp, color = Color(0xFF3A3A3A))
            // "Manage Password" intentionally absent — Convex auth is OTP/Google
            // only, so the web profile page hides it too (app/profile/page.tsx).
            ProfileRow(
                title = "Log Out",
                subtitle = "Sign out of your account.",
                onClick = onLogOutClick
            )
            HorizontalDivider(thickness = 1.dp, color = Color(0xFF3A3A3A))
        }
        BottomNavBar(selectedIndex = 3, onItemSelected = onItemSelected)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfileScreenPreview() {
    LampStandTheme(dynamicColor = false) {
        ProfileScreen()
    }
}
