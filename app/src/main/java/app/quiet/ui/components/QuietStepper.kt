package app.quiet.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.quiet.ui.theme.QuietThemeAccessor

@Composable
fun QuietStepper(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = QuietThemeAccessor.colors
    val spacing = QuietThemeAccessor.spacing
    val radius = QuietThemeAccessor.radius
    val type = QuietThemeAccessor.typography
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        TextButton(
            onClick = { if (value > range.first) onValueChange(value - 1) },
            enabled = value > range.first,
            modifier = Modifier
                .height(spacing.targetMin)
                .border(radius.borderWidth, colors.border, RoundedCornerShape(radius.radiusM)),
        ) {
            Text(
                text = "−",
                style = type.title,
                color = if (value > range.first) colors.textPrimary else colors.textTertiary,
            )
        }
        Text(
            text = value.toString(),
            style = type.title,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 32.dp),
        )
        TextButton(
            onClick = { if (value < range.last) onValueChange(value + 1) },
            enabled = value < range.last,
            modifier = Modifier
                .height(spacing.targetMin)
                .border(radius.borderWidth, colors.border, RoundedCornerShape(radius.radiusM)),
        ) {
            Text(
                text = "+",
                style = type.title,
                color = if (value < range.last) colors.textPrimary else colors.textTertiary,
            )
        }
    }
}
