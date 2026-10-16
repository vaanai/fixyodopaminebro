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
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import app.quiet.ui.theme.QuietThemeAccessor
import quiet.core.rules.Policy

@Composable
fun PolicySegment(
    options: List<Policy>,
    selected: Policy,
    onSelect: (Policy) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
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
            .padding(spacing.space1)
            .semantics { contentDescription = "Policy $selected" },
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Text(
                text = label(option),
                style = type.label,
                textAlign = TextAlign.Center,
                maxLines = 2,
                color = when {
                    isSelected -> colors.onAccent
                    !enabled -> colors.textTertiary
                    else -> colors.textSecondary
                },
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (isSelected) colors.accent else colors.surface,
                        RoundedCornerShape(radius.radiusS),
                    )
                    .clickable(enabled = enabled && !isSelected) { onSelect(option) }
                    .padding(horizontal = spacing.space1, vertical = spacing.space2),
            )
        }
    }
}

private fun label(policy: Policy): String = policySegmentLabel(policy)
