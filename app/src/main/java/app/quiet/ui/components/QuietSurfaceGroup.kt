package app.quiet.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.quiet.ui.theme.QuietThemeAccessor

@Composable
fun QuietSurfaceGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = QuietThemeAccessor.colors
    val radius = QuietThemeAccessor.radius
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(radius.borderWidth, colors.border, RoundedCornerShape(radius.radiusL)),
        content = content,
    )
}
