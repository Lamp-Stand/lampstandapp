package com.example.lampstand.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScreenStatusBanner(
    message: String? = null,
    loading: Boolean = false,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (message == null && !loading) return
    val isError = message != null
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (isError) Color(0xFFFEF3C7) else Color(0xFFE8F3FA))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message ?: "Loading...",
            color = if (isError) Color(0xFF92400E) else Color(0xFF335E78),
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )
        if (isError && onRetry != null) {
            Text(
                text = "Retry",
                color = Color(0xFF335E78),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable { onRetry() }
                    .padding(start = 12.dp)
            )
        }
    }
}
