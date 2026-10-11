package app.quiet.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.quiet.ui.theme.QuietThemeAccessor
import quiet.core.rules.Policy

@Composable
fun SurfaceRow(
    label: String,
    sublabel: String?,
    policy: Policy,
    options: List<Policy>,
    onChange: (Policy) -> Unit,
) {
    val spacing = QuietThemeAccessor.spacing
    val type = QuietThemeAccessor.typography
    val colors = QuietThemeAccessor.colors
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
}

@Composable
fun ProtectedRow() {
    val spacing = QuietThemeAccessor.spacing
    val type = QuietThemeAccessor.typography
    val colors = QuietThemeAccessor.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.space4, vertical = spacing.space2),
    ) {
        Text(text = "Messages", style = type.title, color = colors.textPrimary, modifier = Modifier.weight(1f))
        Text(text = "Always on", style = type.label, color = colors.textSecondary)
    }
}
