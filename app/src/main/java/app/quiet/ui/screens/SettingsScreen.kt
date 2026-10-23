package app.quiet.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.quiet.BuildConfig
import app.quiet.LocalEditor
import app.quiet.LocalNav
import app.quiet.QuietApp
import app.quiet.Route
import app.quiet.service.QuietService
import app.quiet.ui.components.Chip
import app.quiet.ui.components.Divider
import app.quiet.ui.components.NavRow
import app.quiet.ui.components.QCard
import app.quiet.ui.components.ScreenHeader
import app.quiet.ui.components.SectionTitle
import app.quiet.ui.components.Segmented
import app.quiet.ui.components.StepperRow
import app.quiet.ui.components.SwitchRow
import app.quiet.ui.theme.Q
import app.quiet.ui.theme.accentColor
import quiet.core.settings.Accent
import quiet.core.settings.CoverStyle
import quiet.core.settings.Defaults
import quiet.core.settings.Preset
import java.io.File

@Composable
fun SettingsScreen() {
    val store = QuietApp.store
    val s by store.settings.collectAsStateWithLifecycle()
    val rules by store.rules.collectAsStateWithLifecycle()
    val editor = LocalEditor.current
    val nav = LocalNav.current
    val context = LocalContext.current
    val c = Q.colors
    var confirmPreset by remember { mutableStateOf<Preset?>(null) }
    var confirmReset by remember { mutableStateOf(false) }

    Page {
        ScreenHeader("Settings")

        QCard {
            SwitchRow("Protection", s.protectionOn, if (s.protectionOn) "Feeds are being watched." else "Paused. Time is still counted.", Icons.Rounded.Shield) { on ->
                editor.edit { it.copy(protectionOn = on) }
            }
        }

        SectionTitle("Daily allowance")
        QCard {
            StepperRow(
                "Short-form minutes",
                s.budgetMinutes,
                "Shared by every feed set to Limit.",
                options = listOf(0, 5, 10, 15, 20, 30, 45, 60, 90, 120),
                format = { "${it}m" },
            ) { v -> editor.edit { it.copy(budgetMinutes = v) } }
        }

        SectionTitle("Unlocks")
        QCard {
            SwitchRow("Allow unlocks", s.unlocksEnabled, if (s.strictMode) "Off while strict mode is on." else "Watch a bit more from the block screen.", enabled = !s.strictMode) { on ->
                editor.edit { it.copy(unlocksEnabled = on) }
            }
            if (s.unlocksEnabled && !s.strictMode) {
                StepperRow("Unlocks per day", s.unlocksPerDay, options = listOf(1, 2, 3, 4, 5, 8, 10, 20), format = { "$it" }) { v -> editor.edit { it.copy(unlocksPerDay = v) } }
                StepperRow("Watch N more", s.unlockVideos, "Swipes per \"watch more\" unlock.", options = listOf(0, 1, 2, 3, 5, 8, 10, 15, 20), format = { if (it == 0) "Off" else "$it" }) { v -> editor.edit { it.copy(unlockVideos = v) } }
                StepperRow("Or for N minutes", s.unlockMinutes, options = listOf(1, 2, 3, 5, 10, 15, 20, 30), format = { "${it}m" }) { v -> editor.edit { it.copy(unlockMinutes = v) } }
                StepperRow("Wait before unlocking", s.unlockWaitSeconds, "A moment to change your mind.", options = listOf(0, 5, 10, 15, 20, 30, 45, 60), format = { "${it}s" }) { v -> editor.edit { it.copy(unlockWaitSeconds = v) } }
            }
        }

        SectionTitle("Pause screen")
        QCard {
            Text("For feeds set to Pause.", style = MaterialTheme.typography.bodySmall, color = c.textDim)
            StepperRow("Breathe for", s.pauseSeconds, options = listOf(3, 5, 10, 15, 20, 30), format = { "${it}s" }) { v -> editor.edit { it.copy(pauseSeconds = v) } }
            StepperRow("Then open for", s.pauseMinutes, options = listOf(1, 2, 3, 5, 10, 15, 20, 30), format = { "${it}m" }) { v -> editor.edit { it.copy(pauseMinutes = v) } }
        }

        SectionTitle("Focus")
        QCard {
            NavRow(
                "Focus schedule",
                "Times when every feed is off.",
                Icons.Rounded.Bedtime,
                value = s.focusWindows.count { it.enabled }.let { if (it == 0) "None" else "$it on" },
            ) { nav.go(Route.Focus) }
            SwitchRow("Unlocks during focus", s.focusAllowsUnlocks) { on -> editor.edit { it.copy(focusAllowsUnlocks = on) } }
        }

        SectionTitle("Block screen")
        QCard {
            Text("Cover", style = MaterialTheme.typography.titleSmall, color = c.text)
            Spacer(Modifier.height(8.dp))
            Segmented(CoverStyle.entries, s.coverStyle, { if (it == CoverStyle.ABOVE_TABS) "Keep tab bar" else "Full screen" }) { v ->
                editor.edit { it.copy(coverStyle = v) }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (s.coverStyle == CoverStyle.ABOVE_TABS) "The app's own tabs stay tappable under the cover." else "Nothing of the app shows through.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textDim,
            )
            Spacer(Modifier.height(8.dp))
            Divider()
            SwitchRow("Silence the video behind it", s.pauseVideo, "Pauses or mutes what's playing under the cover.", Icons.Rounded.VolumeOff) { on -> editor.edit { it.copy(pauseVideo = on) } }
            SwitchRow("Haptic tap", s.haptics, "A small buzz when a block appears.", Icons.Rounded.Vibration) { on -> editor.edit { it.copy(haptics = on) } }
            SwitchRow("Timer pill", s.showTimerPill, "Shows time or videos left while something is unlocked or limited.") { on -> editor.edit { it.copy(showTimerPill = on) } }
            StepperRow(
                "Check-in reminders",
                s.nudgeEveryMinutes,
                "A gentle note after this long in any tracked app.",
                options = listOf(0, 5, 10, 15, 20, 30, 45, 60),
                format = { if (it == 0) "Off" else "${it}m" },
            ) { v -> editor.edit { it.copy(nudgeEveryMinutes = v) } }
            SwitchRow("Keep chats open after limits", s.keepMessagesOpen, "Daily app limits never cover your messages.") { on -> editor.edit { it.copy(keepMessagesOpen = on) } }
        }

        SectionTitle("Strict mode")
        QCard {
            SwitchRow("Strict mode", s.strictMode, "No unlocks, and anything that loosens Quiet makes you wait first.", Icons.Rounded.Gavel) { on ->
                editor.edit { it.copy(strictMode = on) }
            }
            if (s.strictMode) {
                StepperRow("Wait before loosening", s.strictDelaySeconds, options = listOf(10, 20, 30, 60, 120, 300), format = { if (it >= 60) "${it / 60}m" else "${it}s" }) { v ->
                    editor.edit { it.copy(strictDelaySeconds = v) }
                }
            }
        }

        SectionTitle("Apps")
        QCard {
            NavRow("Tracked apps", "Which apps count as social, and their limits.", Icons.Rounded.Apps) { nav.go(Route.Tracked) }
        }

        SectionTitle("Presets")
        QCard {
            Text("Start over from a ready-made setup. Your focus schedule and tracked apps stay.", style = MaterialTheme.typography.bodySmall, color = c.textDim)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (p in Preset.entries) Chip(p.title, false) { confirmPreset = p }
            }
        }

        SectionTitle("Look")
        QCard {
            Text("Accent", style = MaterialTheme.typography.titleSmall, color = c.text)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (a in Accent.entries) {
                    val on = a == s.accent
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(accentColor(a))
                            .border(if (on) 3.dp else 0.dp, c.text, CircleShape)
                            .clickable { store.updateSettings { it.copy(accent = a) } },
                    )
                }
            }
        }

        SectionTitle("Keep it running")
        QCard {
            val exempt = isBatteryExempt(context)
            NavRow(
                "Battery optimisation",
                if (exempt) "Quiet is exempt. Good." else "Exempt Quiet so Android doesn't put it to sleep.",
                Icons.Rounded.BatteryChargingFull,
                value = if (exempt) "Done" else null,
            ) { requestBatteryExemption(context) }
            NavRow("Accessibility settings", if (QuietService.isEnabled(context)) "Quiet is on." else "Quiet is off.", Icons.Rounded.Shield) {
                openAccessibilitySettings(context)
            }
        }

        SectionTitle("Diagnostics")
        QCard {
            NavRow("Inspector", "See what Quiet sees right now, and save screen dumps.", Icons.Rounded.BugReport) { nav.go(Route.Inspector) }
            NavRow("Reload detection rules", rules.sources.entries.joinToString { "${it.key}: ${it.value}" }, Icons.Rounded.Refresh) {
                store.reloadRules()
                QuietService.instance?.refresh()
            }
            if (rules.errors.isNotEmpty()) {
                Text(rules.errors.values.joinToString("\n"), style = MaterialTheme.typography.bodySmall, color = c.danger)
            }
            SwitchRow("Verbose logging", s.verboseLogging, "Writes screen changes to logcat (tag Quiet).") { on -> store.updateSettings { it.copy(verboseLogging = on) } }
            NavRow("Export my data", "Settings and stats as JSON.", Icons.Rounded.Download) {
                val dir = File(context.cacheDir, "exports").apply { mkdirs() }
                val file = File(dir, "quiet-export.json").apply { writeText(store.exportJson()) }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                val send = Intent(Intent.ACTION_SEND).setType("application/json").putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(Intent.createChooser(send, "Export Quiet data").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            NavRow("Reset stats", "Clears every number and the activity log.") { confirmReset = true }
        }

        Spacer(Modifier.height(18.dp))
        Text("Quiet ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelSmall, color = c.textFaint)
    }

    confirmPreset?.let { p ->
        AlertDialog(
            onDismissRequest = { confirmPreset = null },
            containerColor = c.surface,
            title = { Text("Switch to ${p.title}?", color = c.text) },
            text = { Text(p.blurb, color = c.textDim) },
            confirmButton = {
                TextButton(onClick = {
                    editor.edit { cur -> Defaults.preset(p, cur) }
                    confirmPreset = null
                }) { Text("Switch", color = c.accent) }
            },
            dismissButton = { TextButton(onClick = { confirmPreset = null }) { Text("Cancel", color = c.text) } },
        )
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = c.surface,
            title = { Text("Reset all stats?", color = c.text) },
            text = { Text("Times, counts, streaks and the activity log are deleted. Settings stay.", color = c.textDim) },
            confirmButton = {
                TextButton(onClick = {
                    store.clearStats()
                    confirmReset = false
                }) { Text("Reset", color = c.danger) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel", color = c.text) } },
        )
    }
}
