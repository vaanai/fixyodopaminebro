package app.quiet.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.quiet.ui.components.QuietPrimaryButton
import app.quiet.ui.theme.QuietThemeAccessor
import app.quiet.ui.viewmodel.LibraryViewModel

@Composable
fun LibraryScreen(vm: LibraryViewModel) {
    val state by vm.state.collectAsState()
    val spacing = QuietThemeAccessor.spacing
    val type = QuietThemeAccessor.typography
    val colors = QuietThemeAccessor.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.space5),
    ) {
        Text(text = "Library", style = type.headline, color = colors.textPrimary)
        if (state.items.isEmpty()) {
            Text(
                text = state.emptyMessage ?: "Track what you watch and read.",
                style = type.title,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = spacing.space12),
            )
            QuietPrimaryButton(
                text = "Add item",
                onClick = { },
                modifier = Modifier.padding(top = spacing.space6),
            )
        } else {
            state.items.forEach { item ->
                Text(text = item.title, style = type.title, color = colors.textPrimary)
            }
        }
    }
}
