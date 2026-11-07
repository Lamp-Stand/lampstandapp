package net.lampstandapp.app.journey

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

@Composable
fun SessionRestartDialog(
    message: String,
    onRestart: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onRestart) {
                Text("Restart Session", fontWeight = FontWeight.SemiBold, color = Color(0xFF335E78))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Maybe Later", fontWeight = FontWeight.SemiBold, color = Color(0xFF535353))
            }
        },
        title = {
            Text("Restart Session", fontWeight = FontWeight.SemiBold, color = Color(0xFF184159))
        },
        text = {
            Text(message, color = Color(0xFF535353))
        }
    )
}
