package app.quiet.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.quiet.data.prefs.PresetIds
import app.quiet.ui.components.PresetPicker
import app.quiet.ui.theme.QuietThemeAccessor
import app.quiet.ui.viewmodel.AppsViewModel

@Composable
fun AppsScreen(vm: AppsViewModel) {
    val state by vm.state.collectAsState()
    val spacing = QuietThemeAccessor.spacing
    val type = QuietThemeAccessor.typography
    val colors = QuietThemeAccessor.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.space5),
    ) {
        Text(text = "Apps", style = type.headline, color = colors.textPrimary)
        PresetPicker(
            selected = state.preset,
            onSelect = vm::applyPreset,
        )
        Text(text = "Instagram", style = type.title, modifier = Modifier.padding(top = spacing.space6))
        Text(text = "Home feed blocked · Reels blocked", style = type.label, color = colors.textSecondary)
        Text(text = "TikTok", style = type.title, modifier = Modifier.padding(top = spacing.space3))
        Text(text = "For You blocked · Following blocked", style = type.label, color = colors.textSecondary)
    }
}
