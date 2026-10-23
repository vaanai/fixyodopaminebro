package app.quiet.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.quiet.R
import quiet.core.settings.Accent

@Immutable
data class QuietColors(
    val bg: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val outline: Color,
    val text: Color,
    val textDim: Color,
    val textFaint: Color,
    val accent: Color,
    val accentDim: Color,
    val onAccent: Color,
    val warn: Color,
    val danger: Color,
    val good: Color,
)

fun accentColor(accent: Accent): Color = when (accent) {
    Accent.MINT -> Color(0xFF7CE0C3)
    Accent.VIOLET -> Color(0xFFB69CFF)
    Accent.AMBER -> Color(0xFFF5B85A)
    Accent.ROSE -> Color(0xFFF68FB0)
    Accent.SKY -> Color(0xFF7CC4F8)
}

fun quietColors(accent: Accent): QuietColors {
    val a = accentColor(accent)
    return QuietColors(
        bg = Color(0xFF0A0C0F),
        surface = Color(0xFF13161B),
        surfaceHigh = Color(0xFF1B1F26),
        outline = Color(0xFF262B33),
        text = Color(0xFFF2F4F7),
        textDim = Color(0xFFA3ABB7),
        textFaint = Color(0xFF6B7380),
        accent = a,
        accentDim = a.copy(alpha = 0.16f),
        onAccent = Color(0xFF07130F),
        warn = Color(0xFFF5B85A),
        danger = Color(0xFFFF7A7A),
        good = Color(0xFF7CE0A0),
    )
}

val LocalQuietColors = staticCompositionLocalOf { quietColors(Accent.MINT) }

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_semibold, FontWeight.Bold),
)

private fun style(size: Int, weight: FontWeight, line: Int, spacing: Double = 0.0) = TextStyle(
    fontFamily = Inter,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = spacing.sp,
)

private val QuietTypography = Typography(
    displayLarge = style(48, FontWeight.SemiBold, 54, -1.5),
    displayMedium = style(36, FontWeight.SemiBold, 42, -1.0),
    headlineLarge = style(28, FontWeight.SemiBold, 34, -0.6),
    headlineMedium = style(24, FontWeight.SemiBold, 30, -0.4),
    titleLarge = style(20, FontWeight.SemiBold, 26, -0.2),
    titleMedium = style(16, FontWeight.SemiBold, 22),
    titleSmall = style(14, FontWeight.Medium, 20),
    bodyLarge = style(16, FontWeight.Normal, 23),
    bodyMedium = style(14, FontWeight.Normal, 20),
    bodySmall = style(12, FontWeight.Normal, 16),
    labelLarge = style(14, FontWeight.Medium, 18),
    labelMedium = style(12, FontWeight.Medium, 16, 0.2),
    labelSmall = style(11, FontWeight.Medium, 14, 0.4),
)

object Q {
    val colors: QuietColors
        @Composable get() = LocalQuietColors.current
}

@Composable
fun QuietTheme(accent: Accent = Accent.MINT, content: @Composable () -> Unit) {
    val c = quietColors(accent)
    val scheme = darkColorScheme(
        primary = c.accent,
        onPrimary = c.onAccent,
        primaryContainer = c.accentDim,
        onPrimaryContainer = c.accent,
        secondary = c.accent,
        onSecondary = c.onAccent,
        background = c.bg,
        onBackground = c.text,
        surface = c.surface,
        onSurface = c.text,
        surfaceVariant = c.surfaceHigh,
        onSurfaceVariant = c.textDim,
        surfaceContainer = c.surface,
        surfaceContainerHigh = c.surfaceHigh,
        surfaceContainerHighest = c.surfaceHigh,
        surfaceContainerLow = c.surface,
        surfaceContainerLowest = c.bg,
        outline = c.outline,
        outlineVariant = c.outline,
        error = c.danger,
    )
    CompositionLocalProvider(LocalQuietColors provides c) {
        MaterialTheme(colorScheme = scheme, typography = QuietTypography, content = content)
    }
}
