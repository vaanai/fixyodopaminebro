package app.quiet.ui.screens

import app.quiet.ads.AdConsent
import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.quiet.BuildConfig
import app.quiet.LocalEditor
import app.quiet.LocalNav
import app.quiet.QuietApp
import app.quiet.Route
import app.quiet.Tab
import app.quiet.data.RulesState
import app.quiet.service.QuietService
import app.quiet.ui.components.Bullet
import app.quiet.ui.components.Chip
import app.quiet.ui.components.ChoiceRow
import app.quiet.ui.components.Divider
import app.quiet.ui.components.NavRow
import app.quiet.ui.components.Note
import app.quiet.ui.components.QCard
import app.quiet.ui.components.ScreenHeader
import app.quiet.ui.components.SectionTitle
import app.quiet.ui.components.Segmented
import app.quiet.ui.components.StepperRow
import app.quiet.ui.components.SwitchRow
import app.quiet.ui.components.plural
import app.quiet.ui.components.unlockLabel
import app.quiet.ui.theme.Q
import app.quiet.ui.theme.accentColor
import quiet.core.settings.Accent
import quiet.core.settings.CoverStyle
import quiet.core.settings.Defaults
import quiet.core.settings.Mode
import quiet.core.settings.Preset
import quiet.core.settings.Settings
import quiet.core.settings.SettingsLock
import quiet.core.settings.UnlockKind
import java.io.File

/** The pages behind the Settings list. Each groups the options for one thing. */
enum class SettingsSection { UNLOCKS, ALLOWANCE, PAUSE, LOCK, BLOCK_SCREEN, PRIVACY, ADVANCED }

fun unlockSummary(s: Settings): String = when {
    !s.unlocksEnabled -> "Off"
    s.unlockKind == UnlockKind.CHOOSE -> "Pick each time · ${s.unlocksPerDay} a day"
    else -> "${unlockLabel(s.unlockKind, s.unlockVideos, s.unlockMinutes)} · ${s.unlocksPerDay} a day"
}

/** "Instagram Reels tab, TikTok For You", for feeds currently set to [mode]. */
private fun feedsIn(mode: Mode, rules: RulesState, s: Settings): String =
    rules.apps.values
        .filter { s.app(it.app).enabled }
        .flatMap { app -> feeds(app).filter { s.modeFor(app.app, it.id) == mode }.map { "${app.displayName} ${it.label}" } }
        .joinToString()

@Composable
fun SettingsScreen() {
    val store = QuietApp.store
    val s by store.settings.collectAsStateWithLifecycle()
    val rules by store.rules.collectAsStateWithLifecycle()
    val editor = LocalEditor.current
    val nav = LocalNav.current
    val context = LocalContext.current
    val c = Q.colors
    val now = rememberNow()
    var choosingPreset by remember { mutableStateOf(false) }

    Page {
        ScreenHeader("Settings")

        QCard {
            SwitchRow(
                "Protection",
                s.protectionOn,
                if (s.protectionOn) "Feeds are being watched." else "Paused. Time is still counted.",
                Icons.Rounded.Shield,
            ) { on -> editor.edit { it.copy(protectionOn = on) } }
            Divider()
            Spacer(Modifier.height(8.dp))
            LockStatus(s, now) { nav.go(Route.Section(SettingsSection.LOCK)) }
            Spacer(Modifier.height(4.dp))
        }

        SectionTitle("What's blocked")
        QCard {
            val managed = rules.apps.values.filter { s.app(it.app).enabled }.joinToString { it.displayName }
            NavRow("Apps and feeds", managed.ifEmpty { "No apps managed" }, Icons.Rounded.Apps) { nav.tab(Tab.APPS) }
            NavRow(
                "Focus schedule",
                "Times when video feeds are blocked.",
                Icons.Rounded.Bedtime,
                value = s.focusWindows.count { it.enabled }.let { if (it == 0) "None" else "$it on" },
            ) { nav.go(Route.Focus) }
            NavRow(
                "Tracked apps",
                "Count time in any app and give it a daily limit.",
                Icons.Rounded.Timer,
                value = s.tracked.count { it.dailyLimitMinutes > 0 }.let { if (it > 0) "$it limited" else null },
            ) { nav.go(Route.Tracked) }
            SwitchRow(
                "Links from other apps play",
                s.linksPlay,
                "A TikTok or reel someone texts you plays, even if it opens in a blocked feed. Swiping on counts like a video sent in a chat.",
                Icons.Rounded.Link,
            ) { on -> editor.edit { it.copy(linksPlay = on) } }
        }

        SectionTitle("When something's blocked")
        QCard {
            NavRow("Unlocks", unlockSummary(s), Icons.Rounded.LockOpen) { nav.go(Route.Section(SettingsSection.UNLOCKS)) }
            NavRow("Daily allowance", "${s.budgetMinutes} min a day for feeds set to Limit", Icons.Rounded.HourglassBottom) {
                nav.go(Route.Section(SettingsSection.ALLOWANCE))
            }
            NavRow("Pause screen", "${s.pauseSeconds}s to breathe, then ${s.pauseMinutes} min", Icons.Rounded.SelfImprovement) {
                nav.go(Route.Section(SettingsSection.PAUSE))
            }
            NavRow(
                "Block screen",
                if (s.coverStyle == CoverStyle.ABOVE_TABS) "Keeps the app's tab bar" else "Covers the whole screen",
                Icons.Rounded.Layers,
            ) { nav.go(Route.Section(SettingsSection.BLOCK_SCREEN)) }
        }

        SectionTitle("Staying on track")
        QCard {
            NavRow("Settings lock", SettingsLock.formatWait(s.lockSeconds).let { if (s.lockSeconds > 0) "$it wait to loosen" else it }, Icons.Rounded.Lock) {
                nav.go(Route.Section(SettingsSection.LOCK))
            }
            StepperRow(
                "Check-in reminders",
                s.nudgeEveryMinutes,
                "A gentle note after this long in any tracked app.",
                options = listOf(0, 5, 10, 15, 20, 30, 45, 60),
                format = { if (it == 0) "Off" else "${it}m" },
            ) { v -> editor.edit { it.copy(nudgeEveryMinutes = v) } }
            NavRow("Start over from a preset", "Gentle, Balanced or Strict.", Icons.Rounded.RestartAlt) { choosingPreset = true }
        }

        SectionTitle("Keep it running")
        QCard {
            val on = QuietService.isEnabled(context)
            NavRow("Accessibility", if (on) "Quiet is on." else "Quiet is off. Tap to turn it on.", Icons.Rounded.Shield, value = if (on) "On" else null) {
                if (on || s.disclosureAccepted) openAccessibilitySettings(context) else nav.go(Route.Disclosure)
            }
            NavRow(
                "Battery",
                if (isBatteryExempt(context)) "Quiet can run in the background. Good."
                else "In App info, open App battery usage and allow background use.",
                Icons.Rounded.BatteryChargingFull,
                value = if (isBatteryExempt(context)) "Done" else null,
            ) { openAppInfo(context) }
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
                            .clickable { store.updateSettings { it.copy(accent = a) } }
                            .semantics { contentDescription = "${a.name.lowercase()} accent" },
                    )
                }
            }
        }

        SectionTitle("Privacy and help")
        QCard {
            NavRow("Privacy and your data", "Everything stays on your phone.", Icons.Rounded.Policy) {
                nav.go(Route.Section(SettingsSection.PRIVACY))
            }
            NavRow("Advanced", "Inspector, detection rules, logs.", Icons.Rounded.BugReport) {
                nav.go(Route.Section(SettingsSection.ADVANCED))
            }
        }

        Spacer(Modifier.height(18.dp))
        Text("Quiet ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelSmall, color = c.textFaint)
    }

    if (choosingPreset) PresetDialog(onDismiss = { choosingPreset = false }) { p ->
        editor.edit { cur -> Defaults.preset(p, cur) }
        choosingPreset = false
    }
}

@Composable
private fun PresetDialog(onDismiss: () -> Unit, onPick: (Preset) -> Unit) {
    val c = Q.colors
    var picked by remember { mutableStateOf(Preset.BALANCED) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text("Start over from a preset", color = c.text) },
        text = {
            Column {
                Note("Feeds, unlocks and the allowance are reset. Your focus schedule, tracked apps and settings lock stay.")
                Spacer(Modifier.height(10.dp))
                for (p in Preset.entries) ChoiceRow(p.title, p.blurb, p == picked) { picked = p }
            }
        },
        confirmButton = { TextButton(onClick = { onPick(picked) }) { Text("Use ${picked.title}", color = c.accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.text) } },
    )
}

@Composable
fun SettingsSectionScreen(section: SettingsSection) {
    when (section) {
        SettingsSection.UNLOCKS -> UnlocksPage()
        SettingsSection.ALLOWANCE -> AllowancePage()
        SettingsSection.PAUSE -> PausePage()
        SettingsSection.LOCK -> LockPage()
        SettingsSection.BLOCK_SCREEN -> BlockScreenPage()
        SettingsSection.PRIVACY -> PrivacyPage()
        SettingsSection.ADVANCED -> AdvancedPage()
    }
}

@Composable
private fun UnlocksPage() {
    val s by QuietApp.store.settings.collectAsStateWithLifecycle()
    val editor = LocalEditor.current
    val nav = LocalNav.current
    Page {
        ScreenHeader(
            "Unlocks",
            "When something's blocked, an unlock opens it for a bit. Unlock from the block screen or from Today.",
            onBack = { nav.back() },
        )
        Spacer(Modifier.height(8.dp))
        QCard {
            SwitchRow("Allow unlocks", s.unlocksEnabled, if (s.unlocksEnabled) "Up to ${s.unlocksPerDay} a day." else "Blocked means blocked.") { on ->
                editor.edit { it.copy(unlocksEnabled = on) }
            }
        }
        if (s.unlocksEnabled) {
            SectionTitle("Each unlock gives you")
            QCard {
                ChoiceRow(
                    unlockLabel(UnlockKind.VIDEOS, s.unlockVideos, s.unlockMinutes),
                    "Swipe through ${plural(s.unlockVideos, "more video")}, then it locks again.",
                    s.unlockKind == UnlockKind.VIDEOS,
                ) { editor.edit { it.copy(unlockKind = UnlockKind.VIDEOS) } }
                ChoiceRow(
                    unlockLabel(UnlockKind.MINUTES, s.unlockVideos, s.unlockMinutes),
                    "The app's feeds open for ${plural(s.unlockMinutes, "minute")}.",
                    s.unlockKind == UnlockKind.MINUTES,
                ) { editor.edit { it.copy(unlockKind = UnlockKind.MINUTES) } }
                ChoiceRow(
                    "Let me pick each time",
                    "The block screen offers both.",
                    s.unlockKind == UnlockKind.CHOOSE,
                ) { editor.edit { it.copy(unlockKind = UnlockKind.CHOOSE) } }
                Spacer(Modifier.height(6.dp))
                if (UnlockKind.VIDEOS in s.unlockKind.offers()) {
                    StepperRow("Videos per unlock", s.unlockVideos, options = listOf(1, 2, 3, 5, 8, 10, 15, 20), format = { "$it" }) { v ->
                        editor.edit { it.copy(unlockVideos = v) }
                    }
                }
                StepperRow(
                    "Minutes per unlock",
                    s.unlockMinutes,
                    if (s.unlockKind == UnlockKind.VIDEOS) "Used when a whole app hits its daily limit, where videos can't be counted." else null,
                    options = listOf(1, 2, 3, 5, 10, 15, 20, 30),
                    format = { "${it}m" },
                ) { v -> editor.edit { it.copy(unlockMinutes = v) } }
            }

            SectionTitle("How often")
            QCard {
                StepperRow("Unlocks per day", s.unlocksPerDay, options = listOf(1, 2, 3, 4, 5, 8, 10, 20), format = { "$it" }) { v ->
                    editor.edit { it.copy(unlocksPerDay = v) }
                }
                StepperRow(
                    "Wait before unlocking",
                    s.unlockWaitSeconds,
                    "A moment to change your mind.",
                    options = listOf(0, 5, 10, 15, 20, 30, 45, 60, 90, 120),
                    format = { if (it < 60) "${it}s" else if (it % 60 == 0) "${it / 60}m" else "${it / 60}m ${it % 60}s" },
                ) { v ->
                    editor.edit { it.copy(unlockWaitSeconds = v) }
                }
                if (s.unlockWaitSeconds > 0) {
                    SwitchRow(
                        "Skip the wait with an ad",
                        s.adSkipsWait,
                        if (s.adSkipsWait) "While you wait, you can watch 1 short ad instead. It opens in Quiet, never over another app."
                        else "Unlocking always means waiting.",
                    ) { on -> editor.edit { it.copy(adSkipsWait = on) } }
                }
                Spacer(Modifier.height(6.dp))
                Note("Whether focus time can be unlocked is set under Focus schedule.")
            }
        }
    }
}

@Composable
private fun AllowancePage() {
    val s by QuietApp.store.settings.collectAsStateWithLifecycle()
    val rules by QuietApp.store.rules.collectAsStateWithLifecycle()
    val editor = LocalEditor.current
    val nav = LocalNav.current
    Page {
        ScreenHeader(
            "Daily allowance",
            "Feeds set to Limit share one daily allowance. Once it's used up they're blocked until midnight.",
            onBack = { nav.back() },
        )
        Spacer(Modifier.height(8.dp))
        QCard {
            StepperRow(
                "Minutes a day",
                s.budgetMinutes,
                options = listOf(0, 5, 10, 15, 20, 30, 45, 60, 90, 120),
                format = { "${it}m" },
            ) { v -> editor.edit { it.copy(budgetMinutes = v) } }
            Spacer(Modifier.height(6.dp))
            val limited = feedsIn(Mode.LIMIT, rules, s)
            Note(if (limited.isEmpty()) "No feed is set to Limit right now. Choose Limit for a feed under Apps." else "Set to Limit: $limited.")
        }
    }
}

@Composable
private fun PausePage() {
    val s by QuietApp.store.settings.collectAsStateWithLifecycle()
    val rules by QuietApp.store.rules.collectAsStateWithLifecycle()
    val editor = LocalEditor.current
    val nav = LocalNav.current
    Page {
        ScreenHeader(
            "Pause screen",
            "Feeds set to Pause show a short breathing screen first. Then you go in for a few minutes, or leave.",
            onBack = { nav.back() },
        )
        Spacer(Modifier.height(8.dp))
        QCard {
            StepperRow("Breathe for", s.pauseSeconds, options = listOf(3, 5, 10, 15, 20, 30), format = { "${it}s" }) { v ->
                editor.edit { it.copy(pauseSeconds = v) }
            }
            StepperRow("Then open for", s.pauseMinutes, options = listOf(1, 2, 3, 5, 10, 15, 20, 30), format = { "${it}m" }) { v ->
                editor.edit { it.copy(pauseMinutes = v) }
            }
            Spacer(Modifier.height(6.dp))
            val paused = feedsIn(Mode.PAUSE, rules, s)
            Note(if (paused.isEmpty()) "No feed is set to Pause right now. Choose Pause for a feed under Apps." else "Set to Pause: $paused.")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LockPage() {
    val s by QuietApp.store.settings.collectAsStateWithLifecycle()
    val editor = LocalEditor.current
    val nav = LocalNav.current
    val now = rememberNow()
    Page {
        ScreenHeader(
            "Settings lock",
            "Future-you has to wait before making Quiet less strict. Making it stricter is always instant.",
            onBack = { nav.back() },
        )
        Spacer(Modifier.height(8.dp))
        QCard { LockStatus(s, now, onSetUp = null) }

        SectionTitle("Wait before loosening")
        QCard {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (v in SettingsLock.ALL_CHOICES) {
                    Chip(SettingsLock.formatWait(v), v == s.lockSeconds) { editor.edit { it.copy(lockSeconds = v) } }
                }
            }
            Spacer(Modifier.height(10.dp))
            Note(
                "When the wait is over, settings stay open for ${SettingsLock.OPEN_MS / 60_000} minutes so you can make " +
                    "several changes, then lock again. Shortening the wait counts as loosening too.",
            )
        }

        SectionTitle("What needs the wait")
        QCard {
            Bullet("Pausing protection, or turning off an app")
            Bullet("Allowing, limiting or pausing a feed that was blocked")
            Bullet("More unlocks, bigger ones, a shorter wait to unlock, a new kind of unlock, or turning on the ad that skips the wait")
            Bullet("A bigger daily allowance, or a looser daily app limit")
            Bullet("Ending focus early, or deleting a focus time")
        }
    }
}

@Composable
private fun BlockScreenPage() {
    val s by QuietApp.store.settings.collectAsStateWithLifecycle()
    val editor = LocalEditor.current
    val nav = LocalNav.current
    val c = Q.colors
    Page {
        ScreenHeader("Block screen", "What you see when a feed is blocked.", onBack = { nav.back() })
        Spacer(Modifier.height(8.dp))
        QCard {
            Text("Cover", style = MaterialTheme.typography.titleSmall, color = c.text)
            Spacer(Modifier.height(8.dp))
            Segmented(CoverStyle.entries, s.coverStyle, { if (it == CoverStyle.ABOVE_TABS) "Keep tab bar" else "Full screen" }) { v ->
                editor.edit { it.copy(coverStyle = v) }
            }
            Spacer(Modifier.height(6.dp))
            Note(if (s.coverStyle == CoverStyle.ABOVE_TABS) "The app's own tabs stay tappable under the cover." else "Nothing of the app shows through.")
            Spacer(Modifier.height(8.dp))
            Divider()
            SwitchRow("Silence the video behind it", s.pauseVideo, "Pauses or mutes what's playing under the cover.", Icons.Rounded.VolumeOff) { on ->
                editor.edit { it.copy(pauseVideo = on) }
            }
            SwitchRow("Haptic tap", s.haptics, "A small buzz when a block appears.", Icons.Rounded.Vibration) { on ->
                editor.edit { it.copy(haptics = on) }
            }
            SwitchRow("Timer pill", s.showTimerPill, "Shows time or videos left while something is unlocked or limited.") { on ->
                editor.edit { it.copy(showTimerPill = on) }
            }
        }
    }
}

@Composable
private fun PrivacyPage() {
    val store = QuietApp.store
    val nav = LocalNav.current
    val context = LocalContext.current
    val c = Q.colors
    var confirmReset by remember { mutableStateOf(false) }
    Page {
        ScreenHeader("Privacy and your data", "Blocking and time tracking work entirely on your phone. There's no account and no server.", onBack = { nav.back() })

        SectionTitle("What Quiet reads")
        QCard {
            Bullet("Which app is open, to count your time in social apps.")
            Bullet("The layout of Instagram, TikTok and YouTube screens (tab names, button labels, view IDs), to tell a feed from a chat.")
            Bullet("That's read as you use the apps and thrown away. Messages, captions and anything you type are never saved.")
        }

        SectionTitle("What Quiet keeps")
        QCard {
            Bullet("Your settings, daily time per app, and a short activity log, on this phone only.")
            Bullet("None of it is sent anywhere or shared with anyone. Uninstalling Quiet deletes it all.")
        }

        SectionTitle("The one ad")
        QCard {
            Bullet("When you unlock, you can watch one short ad instead of waiting. It shows on Quiet's own screen, only when you ask for it.")
            Bullet("Google AdMob shows it. Google collects things like your IP address and advertising ID to do that. Nothing from your settings, stats or screens goes with it.")
            val activity = context as? Activity
            if (activity != null && AdConsent.privacyOptionsRequired(activity)) {
                NavRow("Ad privacy choices", "Change what you agreed to for ads.", Icons.Rounded.Policy) { AdConsent.showPrivacyOptions(activity) }
            }
        }

        SectionTitle("Your data")
        QCard {
            NavRow("Privacy policy", "The full policy, on the web.", Icons.Rounded.Policy) { openUrl(context, PRIVACY_POLICY_URL) }
            NavRow("Export my data", "Settings and stats as a JSON file.", Icons.Rounded.Download) {
                val dir = File(context.cacheDir, "exports").apply { mkdirs() }
                val file = File(dir, "quiet-export.json").apply { writeText(store.exportJson()) }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                val send = Intent(Intent.ACTION_SEND).setType("application/json").putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(Intent.createChooser(send, "Export Quiet data").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            NavRow("Reset stats", "Clears every number and the activity log.", Icons.Rounded.RestartAlt) { confirmReset = true }
        }
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = c.surface,
            title = { Text("Reset all stats?", color = c.text) },
            text = { Text("Times, counts, streaks and the activity log are deleted. Settings stay, and today's limits keep counting.", color = c.textDim) },
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

@Composable
private fun AdvancedPage() {
    val store = QuietApp.store
    val s by store.settings.collectAsStateWithLifecycle()
    val rules by store.rules.collectAsStateWithLifecycle()
    val nav = LocalNav.current
    val c = Q.colors
    Page {
        ScreenHeader("Advanced", "For checking how Quiet reads apps, and fixing it when an app update moves things around.", onBack = { nav.back() })
        Spacer(Modifier.height(8.dp))
        QCard {
            NavRow("Inspector", if (BuildConfig.DEBUG) "See what Quiet sees right now, and save screen dumps." else "See what Quiet sees right now.", Icons.Rounded.BugReport) { nav.go(Route.Inspector) }
            NavRow("Reload detection rules", rules.sources.entries.joinToString { "${it.key}: ${it.value}" }, Icons.Rounded.Refresh) {
                store.reloadRules()
                QuietService.instance?.refresh()
            }
            if (rules.errors.isNotEmpty()) {
                Text(rules.errors.values.joinToString("\n"), style = MaterialTheme.typography.bodySmall, color = c.danger)
            }
            SwitchRow("Verbose logging", s.verboseLogging, "Writes screen changes to logcat (tag Quiet).") { on ->
                store.updateSettings { it.copy(verboseLogging = on) }
            }
        }
    }
}
