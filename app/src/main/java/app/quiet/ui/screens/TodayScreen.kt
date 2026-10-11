package app.quiet.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.quiet.ui.components.HealthBanner
import app.quiet.ui.components.HeroStat
import app.quiet.ui.components.QuietPrimaryButton
import app.quiet.ui.components.QuietTextButton
import app.quiet.ui.components.QuietTopBar
import app.quiet.ui.components.SecondaryStatPair
import app.quiet.ui.theme.QuietThemeAccessor
import app.quiet.ui.viewmodel.TodayViewModel

@Composable
fun TodayScreen(vm: TodayViewModel, onSettings: () -> Unit) {
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
        QuietTopBar(
            title = "Today",
            trailing = {
                IconButton(onClick = onSettings) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                }
            },
        )
        if (state.showHealthBanner) {
            HealthBanner(
                message = "Quiet isn't running, so nothing is paused.",
                onOpenSettings = onSettings,
            )
        }
        HeroStat(value = state.blockedToday.toString(), label = "blocked today")
        Text(
            text = "${state.presetLabel} · Quiet hours ${if (state.quietHoursOn) "on" else "off"}",
            style = type.label,
            color = colors.textSecondary,
        )
        SecondaryStatPair(
            leftValue = state.gatedMinutes,
            leftLabel = "gated",
            rightValue = state.breaksLeft,
            rightLabel = "breaks left",
        )
        when {
            state.breakActive && state.breakUntilLabel != null -> {
                Text(
                    text = "Break until ${state.breakUntilLabel}",
                    style = type.title,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(top = spacing.space6),
                )
                QuietTextButton(
                    text = "End break",
                    onClick = { vm.endBreak() },
                    modifier = Modifier.padding(top = spacing.space2),
                )
            }
            state.breaksRemaining > 0 -> {
                QuietPrimaryButton(
                    text = "Take a 5-minute break",
                    onClick = { vm.startBreak() },
                    modifier = Modifier.padding(top = spacing.space6),
                )
            }
            else -> {
                QuietPrimaryButton(
                    text = "No breaks left today",
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.padding(top = spacing.space6),
                )
            }
        }
        Text(
            text = "Recent",
            style = type.label,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = spacing.space8, bottom = spacing.space2),
        )
        if (state.recent.isEmpty()) {
            Text(
                text = "Nothing blocked yet today.",
                style = type.title,
                color = colors.textPrimary,
            )
        } else {
            state.recent.forEach { line ->
                Text(text = line, style = type.body, color = colors.textPrimary)
            }
        }
    }
}
