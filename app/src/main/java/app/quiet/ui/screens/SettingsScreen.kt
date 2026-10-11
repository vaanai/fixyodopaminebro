package app.quiet.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.quiet.ui.components.QuietOutlineButton
import app.quiet.ui.components.QuietTextButton
import app.quiet.ui.theme.QuietThemeAccessor
import app.quiet.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.runBlocking

@Composable
fun SettingsScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsState()
    val spacing = QuietThemeAccessor.spacing
    val type = QuietThemeAccessor.typography
    val colors = QuietThemeAccessor.colors
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing.space5),
        ) {
            QuietTextButton(text = "Back", onClick = onBack, accent = true)
            Text(text = "Settings", style = type.headline, color = colors.textPrimary)
            Text(text = "Diagnostics", style = type.label, color = colors.textSecondary, modifier = Modifier.padding(top = spacing.space6))
            Text(text = "Unknown screens: ${state.unknownCount}", style = type.body, color = colors.textPrimary)
            Text(text = "Last active: ${state.lastActive}", style = type.body, color = colors.textPrimary)
            Text(text = state.unverifiedWarning, style = type.label, color = colors.textSecondary, modifier = Modifier.padding(top = spacing.space2))
            QuietOutlineButton(text = "Capture screen", onClick = vm::startCapture, modifier = Modifier.padding(top = spacing.space4))
            QuietOutlineButton(
                text = "Export log",
                onClick = {
                    val json = runBlocking { vm.exportLogJson() }
                    val share = Intent(Intent.ACTION_SEND).apply {
                        setType("application/json")
                        putExtra(Intent.EXTRA_TEXT, json)
                    }
                    context.startActivity(Intent.createChooser(share, "Export log"))
                },
                modifier = Modifier.padding(top = spacing.space2),
            )
            QuietTextButton(text = "Delete all data", onClick = vm::deleteAll, modifier = Modifier.padding(top = spacing.space8))
            state.captureMessage?.let {
                Text(text = it, style = type.body, color = colors.textSecondary, modifier = Modifier.padding(top = spacing.space4))
            }
        }
        state.captureCountdown?.let { n ->
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            ) {
                Text(text = n.toString(), style = type.display, color = colors.textPrimary)
                Text(text = "Switch to the app.", style = type.label, color = colors.textSecondary)
            }
        }
    }
}
