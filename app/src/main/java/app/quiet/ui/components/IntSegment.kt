package app.quiet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import app.quiet.ui.theme.QuietThemeAccessor

@Composable
fun IntSegment(
    options: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
    labelFor: (Int) -> String = { it.toString() },
    modifier: Modifier = Modifier,
) {
    val colors = QuietThemeAccessor.colors
    val spacing = QuietThemeAccessor.spacing
    val radius = QuietThemeAccessor.radius
    val type = QuietThemeAccessor.typography
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(spacing.targetMin)
            .border(radius.borderWidth, colors.border, RoundedCornerShape(radius.radiusM))
            .padding(spacing.space1),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(radius.radiusS))
                    .background(if (isSelected) colors.accent else colors.surface)
                    .clickable(enabled = !isSelected) { onSelect(option) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = labelFor(option),
                    style = type.label,
                    textAlign = TextAlign.Center,
                    color = if (isSelected) colors.onAccent else colors.textSecondary,
                )
            }
        }
    }
}
