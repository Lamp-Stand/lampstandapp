package net.lampstandapp.app.journey

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun SessionUnavailableScreen(
    message: String,
    onBack: () -> Unit,
    onRetry: (() -> Unit)? = null
) {
    // Bright surface so the popup pops off the session screen (matches
    // the Restart Journey dialog styling).
    AlertDialog(
        onDismissRequest = onBack,
        containerColor = Color.White,
        titleContentColor = Color(0xFF184159),
        textContentColor = Color(0xFF3A3A3A),
        confirmButton = {
            TextButton(
                onClick = {
                    if (onRetry != null) onRetry() else onBack()
                }
            ) {
                Text(
                    if (onRetry != null) "Retry" else "OK",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF335E78)
                )
            }
        },
        dismissButton = if (onRetry != null) {
            {
                TextButton(onClick = onBack) {
                    Text(
                        "Back to Journey",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF535353)
                    )
                }
            }
        } else {
            null
        },
        title = {
            Text(
                "Session unavailable",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color(0xFF184159)
            )
        },
        text = {
            Text(
                message,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = Color(0xFF3A3A3A)
            )
        }
    )
}
