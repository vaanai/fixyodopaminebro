package app.quiet.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.quiet.ui.theme.QuietThemeAccessor
import app.quiet.ui.theme.heroNumberTextStyle

@Composable
fun HealthBanner(message: String, onOpenSettings: () -> Unit) {
    val colors = QuietThemeAccessor.colors
    val spacing = QuietThemeAccessor.spacing
    val radius = QuietThemeAccessor.radius
    val type = QuietThemeAccessor.typography
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = spacing.space3)
            .border(radius.borderWidth, colors.border, RoundedCornerShape(radius.radiusL))
            .padding(spacing.space4),
    ) {
        Text(text = message, style = type.body, color = colors.textPrimary)
        QuietTextButton(text = "Open settings", onClick = onOpenSettings, accent = true)
    }
}

@Composable
fun HeroStat(value: String, label: String) {
    val colors = QuietThemeAccessor.colors
    val spacing = QuietThemeAccessor.spacing
    val type = QuietThemeAccessor.typography
    Column(modifier = Modifier.padding(vertical = spacing.space4)) {
        Text(text = value, style = heroNumberTextStyle(type.display), color = colors.accent)
        Text(text = label, style = type.label, color = colors.textSecondary)
    }
}
