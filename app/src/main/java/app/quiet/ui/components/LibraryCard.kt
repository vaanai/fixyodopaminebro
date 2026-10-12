package app.quiet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.quiet.data.db.LibraryItemEntity
import app.quiet.data.library.LibraryTitleFormatter
import app.quiet.ui.theme.QuietThemeAccessor

@Composable
fun LibraryCard(item: LibraryItemEntity, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = QuietThemeAccessor.colors
    val spacing = QuietThemeAccessor.spacing
    val radius = QuietThemeAccessor.radius
    val type = QuietThemeAccessor.typography
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .border(radius.borderWidth, colors.border, RoundedCornerShape(radius.radiusL))
            .clickable(onClick = onClick)
            .padding(spacing.space4),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = LibraryTitleFormatter.normalize(item.title),
                style = type.title,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            StatusPill(text = item.status)
        }
        if (item.progress.isNotBlank()) {
            Text(
                text = item.progress,
                style = type.label,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = spacing.space1),
            )
        }
        if (item.rating in 1..5) {
            RatingDotsDisplay(rating = item.rating, modifier = Modifier.padding(top = spacing.space2))
        }
    }
}

@Composable
fun RatingDotsDisplay(rating: Int, modifier: Modifier = Modifier) {
    val colors = QuietThemeAccessor.colors
    val spacing = QuietThemeAccessor.spacing
    val radius = QuietThemeAccessor.radius
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(spacing.space1)) {
        (1..5).forEach { level ->
            val filled = level <= rating
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .then(
                        if (filled) {
                            Modifier.background(colors.accent, CircleShape)
                        } else {
                            Modifier.border(radius.borderWidth, colors.border, CircleShape)
                        },
                    ),
            )
        }
    }
}

@Composable
private fun StatusPill(text: String) {
    val colors = QuietThemeAccessor.colors
    val spacing = QuietThemeAccessor.spacing
    val radius = QuietThemeAccessor.radius
    val type = QuietThemeAccessor.typography
    val now = text == "Now"
    Box(
        modifier = Modifier
            .padding(start = spacing.space2)
            .then(
                if (now) {
                    Modifier.background(colors.accent, RoundedCornerShape(radius.radiusPill))
                } else {
                    Modifier
                        .border(radius.borderWidth, colors.border, RoundedCornerShape(radius.radiusPill))
                        .background(colors.surface, RoundedCornerShape(radius.radiusPill))
                },
            )
            .padding(horizontal = spacing.space2, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = type.label,
            color = if (now) colors.onAccent else colors.textSecondary,
        )
    }
}
