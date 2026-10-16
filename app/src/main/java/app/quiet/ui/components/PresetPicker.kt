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
import app.quiet.data.prefs.PresetIds
import app.quiet.ui.copy.QuietCopy
import app.quiet.ui.theme.QuietThemeAccessor

@Composable
fun PresetPicker(selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
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
        modifier = modifier
            .fillMaxWidth()
            .height(spacing.targetMin)
            .border(radius.borderWidth, colors.border, RoundedCornerShape(radius.radiusM))
            .padding(spacing.space1),
    ) {
        options.forEach { option ->
            val isSelected = selected == option
            val enabled = option != PresetIds.CUSTOM
            val label = option.replaceFirstChar { it.uppercase() }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(radius.radiusS))
                    .background(if (isSelected) colors.accent else colors.surface)
                    .clickable(enabled = enabled && !isSelected) { onSelect(option) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = type.label,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    color = when {
                        isSelected -> colors.onAccent
                        !enabled -> colors.textTertiary
                        else -> colors.textSecondary
                    },
                    modifier = Modifier.padding(horizontal = spacing.space1, vertical = spacing.space1),
                )
            }
        }
    }
    val helper = when (selected) {
        PresetIds.STRICT -> QuietCopy.PRESET_HELPER_STRICT
        PresetIds.LIGHT -> QuietCopy.PRESET_HELPER_LIGHT
        PresetIds.CUSTOM -> QuietCopy.PRESET_HELPER_CUSTOM
        else -> QuietCopy.PRESET_HELPER_BALANCED
    }
    Text(
        text = helper,
        style = type.label,
        color = colors.textSecondary,
        modifier = Modifier.padding(top = spacing.space2),
    )
}
