package app.quiet.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.quiet.ui.components.HealthBanner
import app.quiet.ui.components.PresetPicker
import app.quiet.ui.components.QuietPrimaryButton
import app.quiet.ui.components.QuietTextButton
import app.quiet.ui.theme.QuietThemeAccessor
import app.quiet.ui.viewmodel.OnboardingViewModel

@Composable
fun OnboardingScreen(vm: OnboardingViewModel, onDone: () -> Unit) {
    val state by vm.state.collectAsState()
    val spacing = QuietThemeAccessor.spacing
    val type = QuietThemeAccessor.typography
    val colors = QuietThemeAccessor.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.space5),
    ) {
        when (state.step) {
            1 -> {
                Text(text = "Quiet pauses feeds. Messages stay open.", style = type.headline, color = colors.textPrimary)
                PresetPicker(selected = state.preset, onSelect = vm::selectPreset)
            }
            2 -> {
                Text(text = "Allow accessibility", style = type.headline, color = colors.textPrimary)
                if (state.showRestrictedHelp) {
                    Text(
                        text = "If the toggle is greyed out: open Quiet's app info, then Allow restricted settings.",
                        style = type.body,
                        color = colors.textSecondary,
                    )
                }
            }
            3 -> Text(text = "Let Quiet keep running", style = type.headline, color = colors.textPrimary)
            else -> {
                if (state.showHealthBanner) {
                    HealthBanner("Quiet isn't running, so nothing is paused.", onOpenSettings = {})
                }
                Text(text = "Ready", style = type.headline, color = colors.textPrimary)
            }
        }
        QuietPrimaryButton(
            text = if (state.step < 4) "Continue" else "Go to Today",
            onClick = {
                if (state.step < 4) vm.next() else {
                    vm.finish()
                    onDone()
                }
            },
            modifier = Modifier.padding(top = spacing.space8),
        )
        if (state.step in 2..3) {
            QuietTextButton(text = "Not now", onClick = vm::next)
        }
    }
}
