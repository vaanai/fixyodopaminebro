package app.quiet.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.quiet.ui.components.AppCard
import app.quiet.ui.components.PresetPicker
import app.quiet.ui.components.QuietTopBar
import app.quiet.ui.theme.QuietThemeAccessor
import app.quiet.ui.viewmodel.AppsViewModel

@Composable
fun AppsScreen(vm: AppsViewModel, onOpenApp: (String) -> Unit) {
    val state by vm.state.collectAsState()
    val spacing = QuietThemeAccessor.spacing
    val type = QuietThemeAccessor.typography
    val colors = QuietThemeAccessor.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.space5),
    ) {
        QuietTopBar(title = "Apps")
        PresetPicker(selected = state.preset, onSelect = vm::applyPreset)
        state.apps.forEach { app ->
            AppCard(
                name = app.name,
                summary = app.summary,
                onClick = { onOpenApp(app.appId) },
                modifier = Modifier.padding(top = spacing.space3),
            )
        }
        if (state.apps.isEmpty()) {
            androidx.compose.material3.Text(
                text = "No apps yet.",
                style = type.title,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = spacing.space12),
            )
        }
    }
}
