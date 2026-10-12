package app.quiet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.quiet.ui.theme.QuietThemeAccessor

@Composable
fun AppCard(
    name: String,
    summary: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = QuietThemeAccessor.colors
    val spacing = QuietThemeAccessor.spacing
    val radius = QuietThemeAccessor.radius
    val type = QuietThemeAccessor.typography
    val monogram = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp)
            .border(radius.borderWidth, colors.border, RoundedCornerShape(radius.radiusL))
            .clickable(onClick = onClick)
            .padding(spacing.space4)
            .semantics { contentDescription = "$name. $summary" },
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(colors.surfaceRaised, RoundedCornerShape(radius.radiusS))
                .border(radius.borderWidth, colors.border, RoundedCornerShape(radius.radiusS)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = monogram, style = type.title, color = colors.textPrimary)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = spacing.space3),
        ) {
            Text(text = name, style = type.title, color = colors.textPrimary)
            Text(
                text = summary,
                style = type.label,
                color = colors.textSecondary,
            )
        }
        Icon(
            imageVector = Icons.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier
                .size(20.dp)
                .padding(top = spacing.space1),
        )
    }
}
