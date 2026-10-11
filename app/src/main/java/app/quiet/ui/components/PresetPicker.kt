package app.quiet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import app.quiet.data.prefs.PresetIds
import app.quiet.ui.theme.QuietThemeAccessor

@Composable
fun PresetPicker(selected: String, onSelect: (String) -> Unit) {
    val colors = QuietThemeAccessor.colors
    val spacing = QuietThemeAccessor.spacing
    val radius = QuietThemeAccessor.radius
    val type = QuietThemeAccessor.typography
    val options = listOf(
        PresetIds.STRICT,
        PresetIds.BALANCED,
        PresetIds.LIGHT,
        PresetIds.CUSTOM,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(spacing.targetMin)
            .border(radius.borderWidth, colors.border, RoundedCornerShape(radius.radiusM))
            .padding(spacing.space1),
    ) {
        options.forEach { option ->
            val isSelected = selected == option
            val enabled = option != PresetIds.CUSTOM
            Text(
                text = option.replaceFirstChar { it.uppercase() },
                style = type.label,
                color = when {
                    isSelected -> colors.onAccent
                    !enabled -> colors.textTertiary
                    else -> colors.textSecondary
                },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(radius.radiusS))
                    .background(if (isSelected) colors.accent else colors.surface)
                    .clickable(enabled = enabled && !isSelected) { onSelect(option) }
                    .padding(vertical = spacing.space2),
            )
        }
    }
    val helper = when (selected) {
        PresetIds.STRICT -> "Feeds, reels, stories, and explore stay paused."
        PresetIds.LIGHT -> "Feeds and reels ask first. Stories and explore stay open."
        PresetIds.CUSTOM -> "Individual surfaces are set on their own."
        else -> "Feeds and reels stay paused. Stories and explore ask first."
    }
    Text(
        text = helper,
        style = type.label,
        color = colors.textSecondary,
        modifier = Modifier.padding(top = spacing.space2),
    )
}
