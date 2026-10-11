package app.quiet.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class AppearanceMode { System, Dark, Light }

val LocalQuietColors = staticCompositionLocalOf { darkQuietColors() }
val LocalQuietSpacing = staticCompositionLocalOf { QuietSpacing() }
val LocalQuietRadius = staticCompositionLocalOf { QuietRadius() }
val LocalQuietMotion = staticCompositionLocalOf { QuietMotion() }
val LocalQuietTypography = staticCompositionLocalOf { quietTypography(InterFontFamily) }

fun darkQuietColors() = QuietColors(
    background = QuietColorDark.background,
    surface = QuietColorDark.surface,
    surfaceRaised = QuietColorDark.surfaceRaised,
    border = QuietColorDark.border,
    textPrimary = QuietColorDark.textPrimary,
    textSecondary = QuietColorDark.textSecondary,
    textTertiary = QuietColorDark.textTertiary,
    accent = QuietColorDark.accent,
    onAccent = QuietColorDark.onAccent,
    scrim = QuietColorDark.scrim,
    scrimAlpha = 0.72f,
    numberGlowAlpha = 0.40f,
    buttonGlowAlpha = 0.16f,
    ringGlowAlpha = 0.30f,
)

fun lightQuietColors() = QuietColors(
    background = QuietColorLight.background,
    surface = QuietColorLight.surface,
    surfaceRaised = QuietColorLight.surfaceRaised,
    border = QuietColorLight.border,
    textPrimary = QuietColorLight.textPrimary,
    textSecondary = QuietColorLight.textSecondary,
    textTertiary = QuietColorLight.textTertiary,
    accent = QuietColorLight.accent,
    onAccent = QuietColorLight.onAccent,
    scrim = QuietColorLight.scrim,
    scrimAlpha = 0.40f,
    numberGlowAlpha = 0f,
    buttonGlowAlpha = 0f,
    ringGlowAlpha = 0f,
)

@Composable
fun QuietTheme(
    appearance: AppearanceMode = AppearanceMode.System,
    content: @Composable () -> Unit,
) {
    val dark = when (appearance) {
        AppearanceMode.Dark -> true
        AppearanceMode.Light -> false
        AppearanceMode.System -> isSystemInDarkTheme()
    }
    val quietColors = if (dark) darkQuietColors() else lightQuietColors()
    val material = if (dark) {
        darkColorScheme(
            primary = quietColors.accent,
            onPrimary = quietColors.onAccent,
            primaryContainer = quietColors.accent,
            onPrimaryContainer = quietColors.onAccent,
            background = quietColors.background,
            onBackground = quietColors.textPrimary,
            surface = quietColors.surface,
            onSurface = quietColors.textPrimary,
            surfaceVariant = Color(0xFF1C1C1C),
            onSurfaceVariant = quietColors.textSecondary,
            outline = quietColors.border,
            outlineVariant = quietColors.border,
            error = quietColors.textPrimary,
            onError = quietColors.surface,
            errorContainer = quietColors.surfaceRaised,
            surfaceTint = Color.Transparent,
            scrim = quietColors.scrim,
        )
    } else {
        lightColorScheme(
            primary = quietColors.accent,
            onPrimary = quietColors.onAccent,
            primaryContainer = quietColors.accent,
            onPrimaryContainer = quietColors.onAccent,
            background = quietColors.background,
            onBackground = quietColors.textPrimary,
            surface = quietColors.surface,
            onSurface = quietColors.textPrimary,
            surfaceVariant = Color(0xFFFFFFFF),
            onSurfaceVariant = quietColors.textSecondary,
            outline = quietColors.border,
            outlineVariant = quietColors.border,
            error = quietColors.textPrimary,
            onError = quietColors.surface,
            errorContainer = quietColors.surfaceRaised,
            surfaceTint = Color.Transparent,
            scrim = quietColors.scrim,
        )
    }
    CompositionLocalProvider(
        LocalQuietColors provides quietColors,
        LocalQuietSpacing provides QuietSpacing(),
        LocalQuietRadius provides QuietRadius(),
        LocalQuietMotion provides QuietMotion(),
        LocalQuietTypography provides quietTypography(InterFontFamily),
    ) {
        MaterialTheme(
            colorScheme = material,
            content = content,
        )
    }
}

object QuietThemeAccessor {
    val colors: QuietColors
        @Composable get() = LocalQuietColors.current
    val spacing: QuietSpacing
        @Composable get() = LocalQuietSpacing.current
    val radius: QuietRadius
        @Composable get() = LocalQuietRadius.current
    val motion: QuietMotion
        @Composable get() = LocalQuietMotion.current
    val typography: QuietTypography
        @Composable get() = LocalQuietTypography.current
}
