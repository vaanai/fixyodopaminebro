package app.quiet.ui.screens

import android.app.TimePickerDialog
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import app.quiet.ui.components.QuietAlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.quiet.ui.components.IntSegment
import app.quiet.ui.components.QuietScreenColumn
import app.quiet.ui.components.QuietStepper
import app.quiet.ui.components.QuietSurfaceGroup
import app.quiet.ui.components.QuietTextButton
import app.quiet.ui.components.QuietTopBar
import app.quiet.ui.theme.QuietThemeAccessor
import app.quiet.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsState()
    val spacing = QuietThemeAccessor.spacing
    val typography = QuietThemeAccessor.typography
    val colors = QuietThemeAccessor.colors
    val context = LocalContext.current
    var deleteAllConfirm by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.onResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        QuietScreenColumn {
            QuietTopBar(
                title = "Settings",
                leading = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
            SectionLabel("Quiet hours")
            QuietSurfaceGroup {
                SettingsSwitchRow(
                    title = "Quiet hours",
                    checked = state.quietHoursEnabled,
                    onCheckedChange = vm::setQuietHoursEnabled,
                )
                Text(
                    text = if (state.quietHoursEnabled) {
                        "During quiet hours, Gate becomes Block on surfaces already set to Gate."
                    } else {
                        "Off"
                    },
                    style = typography.label,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(
                        start = spacing.space4,
                        end = spacing.space4,
                        bottom = spacing.space2,
                    ),
                )
                if (state.quietHoursEnabled) {
                    SettingsValueRow(
                        title = "From",
                        value = state.quietHoursStartLabel,
                        onClick = {
                            val parts = state.quietHoursStartLabel.split(":")
                            showTimePicker(context, parts[0].toInt(), parts[1].toInt()) { h, m ->
                                vm.setQuietHoursStart(SettingsViewModel.parseTimeToMinutes(h, m))
                            }
                        },
                    )
                    SettingsValueRow(
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
            QuietSurfaceGroup {
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
            QuietSurfaceGroup {
                GateSettingBlock(
                    title = "Gate length",
                    sublabel = "Minutes",
                    options = listOf(5, 10, 15, 20),
                    selected = state.gateMinutes,
                    onSelect = vm::setGateMinutes,
                )
                GateSettingBlock(
                    title = "Hold time",
                    sublabel = "Seconds",
                    options = listOf(2, 3, 5),
                    selected = state.holdSeconds,
                    onSelect = vm::setHoldSeconds,
                )
            }

            SectionLabel("Appearance")
            QuietSurfaceGroup {
                AppearanceBlock(
                    selected = state.appearance,
                    onSelect = vm::setAppearance,
                )
            }

            SectionLabel("Permissions")
            QuietSurfaceGroup {
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
            QuietSurfaceGroup {
                SettingsValueRow(
                    title = "Unknown screens",
                    value = state.unknownCount.toString(),
                    onClick = {},
                    valueOnly = true,
                )
                SettingsValueRow(
                    title = "Last active",
                    value = state.lastActive,
                    onClick = {},
                    valueOnly = true,
                )
                state.unverifiedWarning.lines().forEach { line ->
                    Text(
                        text = line,
                        style = typography.label,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(
                            start = spacing.space4,
                            end = spacing.space4,
                            bottom = spacing.space2,
                        ),
                    )
                }
                QuietTextButton(
                    text = "Capture screen",
                    onClick = vm::startCapture,
                    accent = true,
                    modifier = Modifier.padding(start = spacing.space4, bottom = spacing.space2),
                )
                QuietTextButton(
                    text = "Export log",
                    onClick = {
                        val json = runBlocking { vm.exportLogJson() }
                        shareJson(context, json, "Export log")
                    },
                    accent = true,
                    modifier = Modifier.padding(start = spacing.space4, bottom = spacing.space4),
                )
            }

            SectionLabel("Data")
            QuietSurfaceGroup {
                QuietTextButton(
                    text = "Export library",
                    onClick = {
                        val json = runBlocking { vm.exportLibraryJson() }
                        shareJson(context, json, "Export library")
                    },
                    accent = true,
                    modifier = Modifier.padding(spacing.space4),
                )
                QuietTextButton(
                    text = "Delete all data",
                    onClick = { deleteAllConfirm = true },
                    modifier = Modifier.padding(start = spacing.space4, bottom = spacing.space4),
                )
            }

            SectionLabel("About")
            QuietSurfaceGroup {
                Text(
                    text = "Version ${state.versionName}",
                    style = typography.body,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(spacing.space4),
                )
            }

            state.captureMessage?.let {
                Text(
                    text = it,
                    style = typography.body,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = spacing.space4, bottom = spacing.space8),
                )
            }
        }
        state.captureCountdown?.let { n ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.background),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(text = n.toString(), style = typography.display, color = colors.textPrimary)
                Text(text = "Switch to the app.", style = typography.label, color = colors.textSecondary)
            }
        }
    }
    if (deleteAllConfirm) {
        QuietAlertDialog(
            title = "Delete all data?",
            body = "Events, library, and settings on this device will be removed.",
            confirmLabel = "Delete",
            dismissLabel = "Cancel",
            onConfirm = {
                vm.deleteAll()
                deleteAllConfirm = false
            },
            onDismiss = { deleteAllConfirm = false },
        )
    }
}

@Composable
private fun AppearanceBlock(selected: String, onSelect: (String) -> Unit) {
    val spacing = QuietThemeAccessor.spacing
    val typography = QuietThemeAccessor.typography
    val colors = QuietThemeAccessor.colors
    val options = listOf("system", "dark", "light")
    Text(
        text = "Theme",
        style = typography.title,
        color = colors.textPrimary,
        modifier = Modifier.padding(start = spacing.space4, end = spacing.space4, top = spacing.space4),
    )
    Text(
        text = "System, dark, or light",
        style = typography.label,
        color = colors.textSecondary,
        modifier = Modifier.padding(start = spacing.space4, end = spacing.space4, bottom = spacing.space2),
    )
    IntSegment(
        options = options.indices.toList(),
        selected = options.indexOf(selected).coerceAtLeast(0),
        onSelect = { onSelect(options[it]) },
        labelFor = { options[it].replaceFirstChar { c -> c.uppercase() } },
        modifier = Modifier.padding(
            start = spacing.space4,
            end = spacing.space4,
            bottom = spacing.space4,
        ),
    )
}

@Composable
private fun GateSettingBlock(
    title: String,
    sublabel: String,
    options: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    val spacing = QuietThemeAccessor.spacing
    val typography = QuietThemeAccessor.typography
    val colors = QuietThemeAccessor.colors
    Text(
        text = title,
        style = typography.title,
        color = colors.textPrimary,
        modifier = Modifier.padding(start = spacing.space4, end = spacing.space4, top = spacing.space4),
    )
    Text(
        text = sublabel,
        style = typography.label,
        color = colors.textSecondary,
        modifier = Modifier.padding(start = spacing.space4, end = spacing.space4, bottom = spacing.space2),
    )
    IntSegment(
        options = options,
        selected = selected,
        onSelect = onSelect,
        modifier = Modifier.padding(
            start = spacing.space4,
            end = spacing.space4,
            bottom = spacing.space4,
        ),
    )
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
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsValueRow(
    title: String,
    value: String,
    onClick: () -> Unit,
    valueOnly: Boolean = false,
) {
    val spacing = QuietThemeAccessor.spacing
    val typography = QuietThemeAccessor.typography
    val colors = QuietThemeAccessor.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = spacing.rowMin)
            .then(if (!valueOnly) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = spacing.space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = typography.title, color = colors.textPrimary, modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = typography.label,
            color = if (valueOnly) colors.textPrimary else colors.accent,
        )
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

private fun shareJson(context: android.content.Context, json: String, title: String) {
    val share = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_TEXT, json)
    }
    context.startActivity(Intent.createChooser(share, title))
}
