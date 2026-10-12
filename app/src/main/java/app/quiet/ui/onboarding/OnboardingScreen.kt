package app.quiet.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.quiet.ui.components.OnboardingStepIndicator
import app.quiet.ui.components.PresetPicker
import app.quiet.ui.components.QuietPrimaryButton
import app.quiet.ui.components.QuietSurfaceGroup
import app.quiet.ui.components.QuietTextButton
import app.quiet.ui.theme.QuietThemeAccessor
import app.quiet.ui.viewmodel.OnboardingViewModel
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding

@Composable
fun OnboardingScreen(vm: OnboardingViewModel, onDone: () -> Unit) {
    val state by vm.state.collectAsState()
    val spacing = QuietThemeAccessor.spacing
    val type = QuietThemeAccessor.typography
    val colors = QuietThemeAccessor.colors
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.refreshDeviceState()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .navigationBarsPadding()
            .padding(horizontal = spacing.space5),
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
                        modifier = Modifier.padding(top = spacing.space6),
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
                        QuietSurfaceGroup(modifier = Modifier.padding(top = spacing.space6)) {
                            Text(
                                text = "If the toggle is greyed out",
                                style = type.title,
                                color = colors.textPrimary,
                                modifier = Modifier.padding(spacing.space4),
                            )
                            Text(
                                text = "Open Quiet's app info, then the menu in the top corner, then Allow restricted settings. Menu names vary by phone.",
                                style = type.body,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(
                                    start = spacing.space4,
                                    end = spacing.space4,
                                    bottom = spacing.space4,
                                ),
                            )
                        }
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
                    Text(text = "Ready", style = type.headline, color = colors.textPrimary)
                    Text(
                        text = if (state.accessibilityOn) {
                            "Open Instagram to start."
                        } else {
                            "Nothing is paused until accessibility is on. You can turn it on later in Settings."
                        },
                        style = type.body,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = spacing.space2),
                    )
                }
            }
        }
        val primaryLabel = when (state.step) {
            1 -> "Continue"
            2, 3 -> "Open settings"
            else -> "Go to Today"
        }
        QuietPrimaryButton(
            text = primaryLabel,
            onClick = {
                when (state.step) {
                    1 -> vm.next()
                    2 -> vm.openAccessibilitySettings()
                    3 -> vm.openBatterySettings()
                    else -> {
                        vm.finish()
                        onDone()
                    }
                }
            },
        )
        if (state.step in 2..3) {
            QuietTextButton(text = "Not now", onClick = vm::next)
        }
        if (state.step == 4 && !state.accessibilityOn) {
            QuietTextButton(text = "Open settings", onClick = vm::openAccessibilitySettings, accent = true)
        }
        if (state.step == 4 && state.accessibilityOn && state.instagramInstalled) {
            QuietTextButton(text = "Open Instagram", onClick = vm::openInstagram)
        }
        Spacer(modifier = Modifier.padding(bottom = spacing.space2))
    }
}
