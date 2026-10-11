package app.quiet.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun heroNumberTextStyle(base: TextStyle): TextStyle {
    val colors = QuietThemeAccessor.colors
    val alpha = colors.numberGlowAlpha
    if (alpha <= 0f) return base
    return base.copy(
        shadow = Shadow(
            color = colors.accent.copy(alpha = alpha),
            offset = Offset.Zero,
            blurRadius = 18.dp.value * 3f,
        ),
    )
}

@Composable
fun Modifier.quietPrimaryButtonGlow(enabled: Boolean, cornerRadius: Dp): Modifier {
    if (!enabled) return this
    val colors = QuietThemeAccessor.colors
    val alpha = colors.buttonGlowAlpha
    if (alpha <= 0f) return this
    val spread = 4.dp
    return drawBehind {
        val corner = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
        val inset = spread.toPx()
        drawRoundRect(
            color = Color.White.copy(alpha = alpha),
            topLeft = Offset(-inset, -inset),
            size = Size(size.width + inset * 2, size.height + inset * 2),
            cornerRadius = corner,
        )
    }
}
