package app.quiet.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
            .padding(spacing.space5),
    ) {
        Text(text = "Today", style = type.headline, color = colors.textPrimary)
        IconButton(onClick = onSettings, modifier = Modifier) {
            Icon(Icons.Outlined.Settings, contentDescription = "Settings")
        }
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
        if (state.recent.isEmpty()) {
            Text(
                text = "Nothing blocked yet today.",
                style = type.title,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = spacing.space8),
            )
        } else {
            state.recent.forEach { line ->
                Text(text = line, style = type.body, color = colors.textPrimary)
            }
        }
    }
}
