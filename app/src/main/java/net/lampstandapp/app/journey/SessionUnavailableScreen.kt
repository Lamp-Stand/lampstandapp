package net.lampstandapp.app.journey

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

@Composable
fun SessionUnavailableScreen(
    message: String,
    onBack: () -> Unit,
    onRetry: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onBack,
        confirmButton = {
            TextButton(onClick = onBack) {
                Text("OK", fontWeight = FontWeight.SemiBold, color = Color(0xFF335E78))
            }
        },
        title = {
            Text("Session unavailable", fontWeight = FontWeight.SemiBold, color = Color(0xFF184159))
        },
        text = {
            Text(message, color = Color(0xFF535353))
        }
    )
}
