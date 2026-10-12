package app.quiet.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.quiet.ui.components.AppCard
import app.quiet.ui.components.PresetPicker
import app.quiet.ui.components.QuietScreenColumn
import app.quiet.ui.components.QuietTopBar
import app.quiet.ui.theme.QuietThemeAccessor
import app.quiet.ui.viewmodel.AppsViewModel

@Composable
fun AppsScreen(vm: AppsViewModel, onOpenApp: (String) -> Unit) {
    val state by vm.state.collectAsState()
    val spacing = QuietThemeAccessor.spacing
    val type = QuietThemeAccessor.typography
    val colors = QuietThemeAccessor.colors
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.reload()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    QuietScreenColumn {
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
            Text(
                text = "No apps yet.",
                style = type.title,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = spacing.space12),
            )
            Text(
                text = "Supported apps appear here when their rules load.",
                style = type.body,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = spacing.space2),
            )
        }
    }
}
