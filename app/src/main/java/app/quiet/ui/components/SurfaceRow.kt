package app.quiet.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.quiet.ui.theme.QuietThemeAccessor
import quiet.core.rules.Policy

@Composable
fun SurfaceRow(
    label: String,
    sublabel: String?,
    policy: Policy,
    options: List<Policy>,
    onChange: (Policy) -> Unit,
    showDivider: Boolean = true,
) {
    val spacing = QuietThemeAccessor.spacing
    val type = QuietThemeAccessor.typography
    val colors = QuietThemeAccessor.colors
    val radius = QuietThemeAccessor.radius
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(spacing.space4),
    ) {
        Text(text = label, style = type.title, color = colors.textPrimary)
        if (!sublabel.isNullOrBlank()) {
            Text(
                text = sublabel,
                style = type.body,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = spacing.space1),
            )
        }
        PolicySegment(
            options = options,
            selected = policy,
            onSelect = onChange,
            modifier = Modifier.padding(top = spacing.space2),
        )
    }
    if (showDivider) {
        HorizontalDivider(
            color = colors.border,
            thickness = radius.borderWidth,
            modifier = Modifier.padding(horizontal = spacing.space4),
        )
    }
}

@Composable
fun ProtectedRow() {
    val spacing = QuietThemeAccessor.spacing
    val type = QuietThemeAccessor.typography
    val colors = QuietThemeAccessor.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = spacing.rowMin)
            .padding(horizontal = spacing.space4, vertical = spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = "Protected",
            tint = colors.textSecondary,
            modifier = Modifier
                .padding(end = spacing.space2)
                .size(20.dp),
        )
        Text(text = "Messages", style = type.title, color = colors.textPrimary, modifier = Modifier.weight(1f))
        Text(text = "Always on", style = type.label, color = colors.textSecondary)
    }
}
