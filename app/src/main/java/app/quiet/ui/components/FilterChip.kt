package app.quiet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import app.quiet.ui.theme.QuietThemeAccessor

@Composable
fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = QuietThemeAccessor.colors
    val radius = QuietThemeAccessor.radius
    val spacing = QuietThemeAccessor.spacing
    val type = QuietThemeAccessor.typography
    Text(
        text = text,
        style = type.label,
        color = if (selected) colors.onAccent else colors.textSecondary,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        modifier = Modifier
            .padding(end = spacing.space2)
            .defaultMinSize(minHeight = spacing.targetMin)
            .then(
                if (selected) {
                    Modifier.background(colors.accent, RoundedCornerShape(radius.radiusPill))
                } else {
                    Modifier
                        .background(colors.surface, RoundedCornerShape(radius.radiusPill))
                        .border(radius.borderWidth, colors.border, RoundedCornerShape(radius.radiusPill))
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.space4, vertical = spacing.space2),
    )
}
