package app.quiet.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.quiet.ui.theme.QuietThemeAccessor

@Composable
fun SecondaryStatPair(
    leftValue: String,
    leftLabel: String,
    rightValue: String,
    rightLabel: String,
    modifier: Modifier = Modifier,
) {
    val spacing = QuietThemeAccessor.spacing
    val type = QuietThemeAccessor.typography
    val colors = QuietThemeAccessor.colors
    Row(modifier = modifier.padding(top = spacing.space6)) {
        SecondaryStat(value = leftValue, label = leftLabel, modifier = Modifier.weight(1f))
        SecondaryStat(value = rightValue, label = rightLabel, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SecondaryStat(value: String, label: String, modifier: Modifier = Modifier) {
    val spacing = QuietThemeAccessor.spacing
    val type = QuietThemeAccessor.typography
    val colors = QuietThemeAccessor.colors
    Column(modifier = modifier) {
        Text(text = value, style = type.title, color = colors.textPrimary)
        Text(
            text = label,
            style = type.label,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = spacing.space1),
        )
    }
}
