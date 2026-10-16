package app.quiet.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = spacing.space6),
            verticalArrangement = Arrangement.spacedBy(spacing.space6),
        ) {
            when (state.step) {
                1 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
                        Text(
                            text = "Quiet pauses feeds. Messages stay open.",
                            style = type.headline,
                            color = colors.textPrimary,
                        )
                        Text(
                            text = "Instagram and TikTok stay on your phone. Quiet never closes them. Messages stay reachable.",
                            style = type.body,
                            color = colors.textSecondary,
                        )
                    }
                    PresetPicker(
                        selected = state.preset,
                        onSelect = vm::selectPreset,
                    )
                    QuietSurfaceGroup(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(spacing.space4),
                            verticalArrangement = Arrangement.spacedBy(spacing.space2),
                        ) {
                            Text(
                                text = "What Quiet pauses in v1",
                                style = type.title,
                                color = colors.textPrimary,
                            )
                            Text(
                                text = "Instagram Home and Reels, and TikTok For You and Following. In-chat reels are not detected yet.",
                                style = type.body,
                                color = colors.textSecondary,
                            )
                        }
                    }
                }
                2 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
                        Text(text = "Allow accessibility", style = type.headline, color = colors.textPrimary)
                        Text(
                            text = "Quiet reads the layout of supported apps to find feeds. It never stores text from other apps.",
                            style = type.body,
                            color = colors.textSecondary,
                        )
                    }
                    QuietSurfaceGroup(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(spacing.space4),
                            verticalArrangement = Arrangement.spacedBy(spacing.space3),
                        ) {
                            Text(
                                text = "In accessibility settings",
                                style = type.title,
                                color = colors.textPrimary,
                            )
                            Text(
                                text = "Find Quiet in the list, open it, and turn the service on.",
                                style = type.body,
                                color = colors.textSecondary,
                            )
                            Text(
                                text = if (state.accessibilityOn) {
                                    "Status: On"
                                } else {
                                    "Status: Off"
                                },
                                style = type.label,
                                color = colors.textSecondary,
                            )
                        }
                    }
                    if (state.showRestrictedHelp) {
                        QuietSurfaceGroup(modifier = Modifier.fillMaxWidth()) {
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
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
                        Text(text = "Let Quiet keep running", style = type.headline, color = colors.textPrimary)
                        Text(
                            text = "Turn off battery optimization so Android doesn't stop Quiet.",
                            style = type.body,
                            color = colors.textSecondary,
                        )
                        Text(
                            text = "On some phones, also lock Quiet in the recent-apps list, and turn on Auto-start if you see it.",
                            style = type.body,
                            color = colors.textSecondary,
                        )
                    }
                    QuietSurfaceGroup(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(spacing.space4),
                            verticalArrangement = Arrangement.spacedBy(spacing.space3),
                        ) {
                            Text(
                                text = "In battery settings",
                                style = type.title,
                                color = colors.textPrimary,
                            )
                            Text(
                                text = "Choose Unrestricted or Don't optimize for Quiet.",
                                style = type.body,
                                color = colors.textSecondary,
                            )
                        }
                    }
                }
                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
                        Text(text = "Ready", style = type.headline, color = colors.textPrimary)
                        Text(
                            text = if (state.accessibilityOn) {
                                "Open Instagram to start."
                            } else {
                                "Nothing is paused until accessibility is on. You can turn it on later in Settings."
                            },
                            style = type.body,
                            color = colors.textSecondary,
                        )
                    }
                    QuietSurfaceGroup(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(spacing.space4),
                            verticalArrangement = Arrangement.spacedBy(spacing.space2),
                        ) {
                            Text(
                                text = "Accessibility",
                                style = type.title,
                                color = colors.textPrimary,
                            )
                            Text(
                                text = if (state.accessibilityOn) "On — feeds can pause in supported apps." else "Off — turn it on when you are ready.",
                                style = type.body,
                                color = colors.textSecondary,
                            )
                        }
                    }
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = spacing.space2),
            verticalArrangement = Arrangement.spacedBy(spacing.space2),
        ) {
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
        }
    }
}
