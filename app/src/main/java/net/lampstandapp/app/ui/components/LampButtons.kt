package net.lampstandapp.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.lampstandapp.app.ui.theme.LampStandTheme

val LampBlue = Color(0xFF335E78)
val LampGrey = Color(0xFFB3B3B3)

/** Darkened variant shown while the button is pressed. */
private fun Color.onPress(): Color =
    copy(red = red * 0.78f, green = green * 0.78f, blue = blue * 0.78f)

/** Lightened variant shown on hover (desktop/mouse). */
private fun Color.onHover(): Color = copy(
    red = red + (1f - red) * 0.18f,
    green = green + (1f - green) * 0.18f,
    blue = blue + (1f - blue) * 0.18f
)

/**
 * App-wide filled button: [LampBlue] when enabled, grey when
 * disabled, darkened while pressed, lightened on hover.
 */
@Composable
fun LampActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = LampBlue,
    shape: Shape = RoundedCornerShape(12.dp),
    content: @Composable RowScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val container = when {
        !enabled -> LampGrey
        pressed -> containerColor.onPress()
        hovered -> containerColor.onHover()
        else -> containerColor
    }
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        interactionSource = interaction,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = Color.White,
            disabledContainerColor = LampGrey,
            disabledContentColor = Color.White
        ),
        modifier = modifier,
        content = content
    )
}

/** Text convenience wrapper around [LampActionButton]. */
@Composable
fun LampButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = LampBlue,
    shape: Shape = RoundedCornerShape(12.dp),
    fontWeight: FontWeight = FontWeight.SemiBold
) {
    LampActionButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        containerColor = containerColor,
        shape = shape
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = fontWeight,
            color = Color.White
        )
    }
}

/** App-wide outlined button with matching press/hover/disabled states. */
@Composable
fun LampOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp),
    content: @Composable RowScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        interactionSource = interaction,
        border = BorderStroke(
            1.dp,
            if (!enabled) LampGrey else if (pressed) LampBlue.onPress() else LampBlue
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (!enabled) {
                Color.White
            } else if (pressed || hovered) {
                Color(0x14335E78)
            } else {
                Color.White
            },
            contentColor = LampBlue,
            disabledContentColor = LampGrey
        ),
        modifier = modifier,
        content = content
    )
}

@Preview(showBackground = true)
@Composable
fun LampButtonsPreview() {
    LampStandTheme(dynamicColor = false) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(20.dp)
        ) {
            LampButton(
                text = "Continue",
                onClick = {},
                modifier = Modifier.fillMaxWidth().height(52.dp)
            )
            LampButton(
                text = "Continue",
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            )
            LampOutlinedButton(
                onClick = {},
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Maybe Later", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
