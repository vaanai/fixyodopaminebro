package app.quiet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.quiet.ui.theme.QuietThemeAccessor

@Composable
fun RatingDotsRow(rating: Int, onRatingChange: (Int) -> Unit) {
    val colors = QuietThemeAccessor.colors
    val spacing = QuietThemeAccessor.spacing
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.space1)) {
        (1..5).forEach { level ->
            val filled = level <= rating && rating > 0
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable { onRatingChange(if (rating == level) 0 else level) },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .then(
                            if (filled) {
                                Modifier.background(colors.accent, CircleShape)
                            } else {
                                Modifier.border(1.dp, colors.border, CircleShape)
                            },
                        ),
                )
            }
        }
    }
}
