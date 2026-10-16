package app.quiet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.quiet.ui.theme.QuietThemeAccessor

@Composable
fun QuietScreenColumn(
    modifier: Modifier = Modifier,
    scroll: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = QuietThemeAccessor.colors
    val spacing = QuietThemeAccessor.spacing
    val scrollState = rememberScrollState()
    val base = modifier
        .fillMaxSize()
        .background(colors.background)
        .windowInsetsPadding(WindowInsets.statusBars)
        .padding(horizontal = spacing.space5)
        .padding(bottom = spacing.space3)
    if (scroll) {
        Column(
            modifier = base.verticalScroll(scrollState),
            content = content,
        )
    } else {
        Column(modifier = base, content = content)
    }
}
