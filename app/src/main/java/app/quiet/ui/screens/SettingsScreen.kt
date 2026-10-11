package app.quiet.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.quiet.ui.components.QuietTextButton
import app.quiet.ui.theme.QuietThemeAccessor
import app.quiet.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsState()
    val spacing = QuietThemeAccessor.spacing
    val type = QuietThemeAccessor.typography
    val colors = QuietThemeAccessor.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.space5),
    ) {
        QuietTextButton(text = "Back", onClick = onBack, accent = true)
        Text(text = "Settings", style = type.headline, color = colors.textPrimary)
        Text(
            text = state.diagnostics,
            style = type.label,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = spacing.space6),
        )
        Text(
            text = "Unknown screens: ${state.unknownCount}",
            style = type.body,
            color = colors.textPrimary,
        )
        QuietTextButton(text = "Export log", onClick = vm::exportLog, accent = true)
        QuietTextButton(text = "Delete all data", onClick = vm::deleteAll)
    }
}
