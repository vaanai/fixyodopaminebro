package app.quiet.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.quiet.ui.components.PresetPicker
import app.quiet.ui.theme.QuietThemeAccessor
import app.quiet.ui.viewmodel.AppsViewModel

@Composable
fun AppsScreen(vm: AppsViewModel, onOpenApp: (String) -> Unit) {
    val state by vm.state.collectAsState()
    val spacing = QuietThemeAccessor.spacing
    val type = QuietThemeAccessor.typography
    val colors = QuietThemeAccessor.colors
    val radius = QuietThemeAccessor.radius
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.space5),
    ) {
        Text(text = "Apps", style = type.headline, color = colors.textPrimary)
        PresetPicker(selected = state.preset, onSelect = vm::applyPreset)
        state.apps.forEach { app ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.space3)
                    .clickable { onOpenApp(app.appId) }
                    .padding(spacing.space4),
            ) {
                Text(text = app.name, style = type.title, color = colors.textPrimary)
                Text(text = app.summary, style = type.label, color = colors.textSecondary, maxLines = 1)
            }
        }
        if (state.apps.isEmpty()) {
            Text(
                text = "No apps yet.",
                style = type.title,
                modifier = Modifier.padding(top = spacing.space12),
            )
        }
    }
}
