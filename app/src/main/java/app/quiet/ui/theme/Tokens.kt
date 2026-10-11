package app.quiet.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.quiet.R

object QuietColorDark {
    val background = Color(0xFF000000)
    val surface = Color(0xFF141414)
    val surfaceRaised = Color(0xFF1C1C1C)
    val border = Color(0xFF2C2C2C)
    val textPrimary = Color(0xFFEDEDED)
    val textSecondary = Color(0xFFA3A3A3)
    val textTertiary = Color(0xFF8A8A8A)
    val accent = Color(0xFFFFFFFF)
    val onAccent = Color(0xFF000000)
    val scrim = Color(0xFF000000)
}

object QuietColorLight {
    val background = Color(0xFFF4F4F4)
    val surface = Color(0xFFFFFFFF)
    val surfaceRaised = Color(0xFFFFFFFF)
    val border = Color(0xFFD9D9D9)
    val textPrimary = Color(0xFF141414)
    val textSecondary = Color(0xFF5C5C5C)
    val textTertiary = Color(0xFF5E5E5E)
    val accent = Color(0xFF000000)
    val onAccent = Color(0xFFFFFFFF)
    val scrim = Color(0xFF000000)
}

data class QuietColors(
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val onAccent: Color,
    val scrim: Color,
    val scrimAlpha: Float,
    val numberGlowAlpha: Float,
    val buttonGlowAlpha: Float,
    val ringGlowAlpha: Float,
)

data class QuietSpacing(
    val space1: androidx.compose.ui.unit.Dp = 4.dp,
    val space2: androidx.compose.ui.unit.Dp = 8.dp,
    val space3: androidx.compose.ui.unit.Dp = 12.dp,
    val space4: androidx.compose.ui.unit.Dp = 16.dp,
    val space5: androidx.compose.ui.unit.Dp = 20.dp,
    val space6: androidx.compose.ui.unit.Dp = 24.dp,
    val space8: androidx.compose.ui.unit.Dp = 32.dp,
    val space12: androidx.compose.ui.unit.Dp = 48.dp,
    val targetMin: androidx.compose.ui.unit.Dp = 48.dp,
    val buttonHeight: androidx.compose.ui.unit.Dp = 52.dp,
    val rowMin: androidx.compose.ui.unit.Dp = 56.dp,
    val topBarHeight: androidx.compose.ui.unit.Dp = 64.dp,
    val bottomBarHeight: androidx.compose.ui.unit.Dp = 64.dp,
    val columnMax: androidx.compose.ui.unit.Dp = 560.dp,
)

data class QuietRadius(
    val radiusS: androidx.compose.ui.unit.Dp = 10.dp,
    val radiusM: androidx.compose.ui.unit.Dp = 14.dp,
    val radiusL: androidx.compose.ui.unit.Dp = 20.dp,
    val radiusSheet: androidx.compose.ui.unit.Dp = 28.dp,
    val radiusPill: androidx.compose.ui.unit.Dp = 999.dp,
    val borderWidth: androidx.compose.ui.unit.Dp = 1.dp,
    val ringStroke: androidx.compose.ui.unit.Dp = 3.dp,
    val ringSize: androidx.compose.ui.unit.Dp = 88.dp,
)

data class QuietMotion(
    val durationFast: Int = 120,
    val durationBase: Int = 200,
    val durationEnter: Int = 280,
    val durationExit: Int = 160,
    val holdMs: Int = 3000,
)

data class QuietTypography(
    val display: TextStyle,
    val headline: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
)

fun quietTypography(fontFamily: FontFamily): QuietTypography {
    val tnum = "tnum"
    return QuietTypography(
        display = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 48.sp,
            lineHeight = 52.sp,
            letterSpacing = (-1.5).sp,
            fontFeatureSettings = tnum,
        ),
        headline = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            letterSpacing = (-0.3).sp,
            fontFeatureSettings = tnum,
        ),
        title = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 17.sp,
            lineHeight = 22.sp,
            fontFeatureSettings = tnum,
        ),
        body = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            fontFeatureSettings = tnum,
        ),
        label = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.1.sp,
            fontFeatureSettings = tnum,
        ),
    )
}

val InterFontFamily: FontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)
