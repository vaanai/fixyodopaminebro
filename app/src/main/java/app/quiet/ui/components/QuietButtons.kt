package app.quiet.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.quiet.ui.theme.QuietThemeAccessor

@Composable
fun QuietPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = QuietThemeAccessor.colors
    val radius = QuietThemeAccessor.radius
    val spacing = QuietThemeAccessor.spacing
    val type = QuietThemeAccessor.typography
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(spacing.buttonHeight),
        shape = RoundedCornerShape(radius.radiusM),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (enabled) colors.accent else colors.surfaceRaised,
            contentColor = if (enabled) colors.onAccent else colors.textTertiary,
            disabledContainerColor = colors.surfaceRaised,
            disabledContentColor = colors.textTertiary,
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
    ) {
        Text(text = text, style = type.title)
    }
}

@Composable
fun QuietOutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = QuietThemeAccessor.colors
    val radius = QuietThemeAccessor.radius
    val spacing = QuietThemeAccessor.spacing
    val type = QuietThemeAccessor.typography
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(spacing.buttonHeight)
            .border(radius.borderWidth, colors.border, RoundedCornerShape(radius.radiusM)),
    ) {
        Text(text = text, style = type.title, color = colors.textPrimary)
    }
}

@Composable
fun QuietTextButton(
    text: String,
    onClick: () -> Unit,
    accent: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = QuietThemeAccessor.colors
    val spacing = QuietThemeAccessor.spacing
    val type = QuietThemeAccessor.typography
    TextButton(
        onClick = onClick,
        modifier = modifier.height(spacing.targetMin),
    ) {
        Text(
            text = text,
            style = type.title,
            color = if (accent) colors.accent else colors.textSecondary,
        )
    }
}
