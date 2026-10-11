package app.quiet.ui.screens

import android.app.TimePickerDialog
import android.content.Intent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.quiet.ui.components.IntSegment
import app.quiet.ui.components.QuietOutlineButton
import app.quiet.ui.components.QuietStepper
import app.quiet.ui.components.QuietTextButton
import app.quiet.ui.theme.QuietThemeAccessor
import app.quiet.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.runBlocking

@Composable
fun SettingsScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsState()
    val spacing = QuietThemeAccessor.spacing
    val typography = QuietThemeAccessor.typography
    val colors = QuietThemeAccessor.colors
    val radius = QuietThemeAccessor.radius
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.space5),
    ) {
        QuietTextButton(text = "Back", onClick = onBack, accent = true)
        Text(text = "Settings", style = typography.headline, color = colors.textPrimary)

        SectionLabel("Quiet hours")
        SettingsGroup {
            SettingsSwitchRow(
                title = "Quiet hours",
                checked = state.quietHoursEnabled,
                onCheckedChange = vm::setQuietHoursEnabled,
            )
            Text(
                text = if (state.quietHoursEnabled) {
                    "During quiet hours, Gate becomes Block."
                } else {
                    "Off"
                },
                style = typography.label,
                color = colors.textSecondary,
                modifier = Modifier.padding(start = spacing.space4, end = spacing.space4, bottom = spacing.space2),
            )
            if (state.quietHoursEnabled) {
                SettingsActionRow(
                    title = "From",
                    value = state.quietHoursStartLabel,
                    onClick = {
                        val parts = state.quietHoursStartLabel.split(":")
                        showTimePicker(context, parts[0].toInt(), parts[1].toInt()) { h, m ->
                            vm.setQuietHoursStart(SettingsViewModel.parseTimeToMinutes(h, m))
                        }
                    },
                )
                SettingsActionRow(
                    title = "To",
                    value = state.quietHoursEndLabel,
                    onClick = {
                        val parts = state.quietHoursEndLabel.split(":")
                        showTimePicker(context, parts[0].toInt(), parts[1].toInt()) { h, m ->
                            vm.setQuietHoursEnd(SettingsViewModel.parseTimeToMinutes(h, m))
                        }
                    },
                )
            }
        }

        SectionLabel("Breaks")
        SettingsGroup {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = spacing.rowMin)
                    .padding(horizontal = spacing.space4, vertical = spacing.space2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Breaks per day",
                    style = typography.title,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                QuietStepper(
                    value = state.breaksPerDay,
                    range = 0..5,
                    onValueChange = vm::setBreaksPerDay,
                )
            }
            Text(
                text = "Each break lasts 5 minutes and covers every app.",
                style = typography.label,
                color = colors.textSecondary,
                modifier = Modifier.padding(
                    start = spacing.space4,
                    end = spacing.space4,
                    bottom = spacing.space4,
                ),
            )
        }

        SectionLabel("Gate")
        SettingsGroup {
            Text(
                text = "Gate length",
                style = typography.title,
                color = colors.textPrimary,
                modifier = Modifier.padding(
                    start = spacing.space4,
                    end = spacing.space4,
                    top = spacing.space4,
                ),
            )
            Text(
                text = "Minutes",
                style = typography.label,
                color = colors.textSecondary,
                modifier = Modifier.padding(
                    start = spacing.space4,
                    end = spacing.space4,
                    bottom = spacing.space2,
                ),
            )
            IntSegment(
                options = listOf(5, 10, 15, 20),
                selected = state.gateMinutes,
                onSelect = vm::setGateMinutes,
                modifier = Modifier.padding(
                    start = spacing.space4,
                    end = spacing.space4,
                    bottom = spacing.space4,
                ),
            )
        }

        SectionLabel("Permissions")
        SettingsGroup {
            PermissionRow(
                title = "Accessibility",
                status = if (state.accessibilityOn) "On" else "Off",
                statusEmphasis = state.accessibilityOn,
                actionLabel = "Open settings",
                onAction = vm::openAccessibilitySettings,
            )
            PermissionRow(
                title = "Battery",
                status = state.batteryStatus,
                statusEmphasis = state.batteryStatus == "Unrestricted",
                actionLabel = "Open settings",
                onAction = vm::openBatterySettings,
            )
        }

        SectionLabel("Diagnostics")
        Text(text = "Unknown screens: ${state.unknownCount}", style = typography.body, color = colors.textPrimary)
        Text(text = "Last active: ${state.lastActive}", style = typography.body, color = colors.textPrimary)
        Text(
            text = state.unverifiedWarning,
            style = typography.label,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = spacing.space2),
        )
        QuietOutlineButton(text = "Capture screen", onClick = vm::startCapture, modifier = Modifier.padding(top = spacing.space4))

        SectionLabel("Data")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space2),
        ) {
            QuietOutlineButton(
                text = "Export library",
                onClick = {
                    val json = runBlocking { vm.exportLibraryJson() }
                    val share = Intent(Intent.ACTION_SEND).apply {
                        setType("application/json")
                        putExtra(Intent.EXTRA_TEXT, json)
                    }
                    context.startActivity(Intent.createChooser(share, "Export library"))
                },
                modifier = Modifier.weight(1f),
            )
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
                modifier = Modifier.weight(1f),
            )
        }
        QuietTextButton(text = "Delete all data", onClick = vm::deleteAll, modifier = Modifier.padding(top = spacing.space8))
        state.captureMessage?.let {
            Text(text = it, style = typography.body, color = colors.textSecondary, modifier = Modifier.padding(top = spacing.space4))
        }
    }
    state.captureCountdown?.let { n ->
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = n.toString(), style = typography.display, color = colors.textPrimary)
            Text(text = "Switch to the app.", style = typography.label, color = colors.textSecondary)
        }
    }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val spacing = QuietThemeAccessor.spacing
    val typography = QuietThemeAccessor.typography
    val colors = QuietThemeAccessor.colors
    Text(
        text = text,
        style = typography.label,
        color = colors.textSecondary,
        modifier = Modifier.padding(top = spacing.space6, bottom = spacing.space2),
    )
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    val colors = QuietThemeAccessor.colors
    val radius = QuietThemeAccessor.radius
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(radius.borderWidth, colors.border, RoundedCornerShape(radius.radiusL)),
    ) {
        content()
    }
}

@Composable
private fun SettingsSwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val spacing = QuietThemeAccessor.spacing
    val typography = QuietThemeAccessor.typography
    val colors = QuietThemeAccessor.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = spacing.rowMin)
            .padding(horizontal = spacing.space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = typography.title, color = colors.textPrimary, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun SettingsActionRow(title: String, value: String, onClick: () -> Unit) {
    val spacing = QuietThemeAccessor.spacing
    val typography = QuietThemeAccessor.typography
    val colors = QuietThemeAccessor.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = spacing.rowMin)
            .padding(horizontal = spacing.space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = typography.title, color = colors.textPrimary, modifier = Modifier.weight(1f))
        QuietTextButton(text = value, onClick = onClick, accent = true)
    }
}

@Composable
private fun PermissionRow(
    title: String,
    status: String,
    statusEmphasis: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
) {
    val spacing = QuietThemeAccessor.spacing
    val typography = QuietThemeAccessor.typography
    val colors = QuietThemeAccessor.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = spacing.rowMin)
            .padding(horizontal = spacing.space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = typography.title, color = colors.textPrimary, modifier = Modifier.weight(1f))
        Text(
            text = status,
            style = typography.label,
            color = if (statusEmphasis) colors.textPrimary else colors.textSecondary,
            modifier = Modifier.padding(end = spacing.space2),
        )
        QuietTextButton(text = actionLabel, onClick = onAction, accent = true)
    }
}

private fun showTimePicker(context: android.content.Context, hour: Int, minute: Int, onPicked: (Int, Int) -> Unit) {
    TimePickerDialog(context, { _, h, m -> onPicked(h, m) }, hour, minute, true).show()
}
