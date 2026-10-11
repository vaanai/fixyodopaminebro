package app.quiet.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.quiet.ui.components.HealthBanner
import app.quiet.ui.components.OnboardingStepIndicator
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
        OnboardingStepIndicator(currentStep = state.step)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = spacing.space6),
        ) {
            when (state.step) {
                1 -> {
                    Text(
                        text = "Quiet pauses feeds. Messages stay open.",
                        style = type.headline,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = "Instagram and TikTok stay on your phone. Quiet never closes them. Messages stay reachable.",
                        style = type.body,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = spacing.space2),
                    )
                    PresetPicker(
                        selected = state.preset,
                        onSelect = vm::selectPreset,
                    )
                }
                2 -> {
                    Text(text = "Allow accessibility", style = type.headline, color = colors.textPrimary)
                    Text(
                        text = "Quiet reads the layout of supported apps to find feeds. It never stores text from other apps.",
                        style = type.body,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = spacing.space2),
                    )
                    if (state.showRestrictedHelp) {
                        Text(
                            text = "If the toggle is greyed out: open Quiet's app info, then Allow restricted settings.",
                            style = type.body,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = spacing.space4),
                        )
                    }
                }
                3 -> {
                    Text(text = "Let Quiet keep running", style = type.headline, color = colors.textPrimary)
                    Text(
                        text = "Turn off battery optimization so Android doesn't stop Quiet.",
                        style = type.body,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = spacing.space2),
                    )
                    Text(
                        text = "On some phones, also lock Quiet in the recent-apps list, and turn on Auto-start if you see it.",
                        style = type.body,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = spacing.space2),
                    )
                }
                else -> {
                    if (state.showHealthBanner) {
                        HealthBanner("Quiet isn't running, so nothing is paused.", onOpenSettings = {})
                    }
                    Text(text = "Ready", style = type.headline, color = colors.textPrimary)
                }
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
        )
        if (state.step in 2..3) {
            QuietTextButton(text = "Not now", onClick = vm::next)
        }
    }
}
