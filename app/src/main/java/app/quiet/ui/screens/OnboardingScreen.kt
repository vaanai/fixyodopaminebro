package app.quiet.ui.screens

import android.content.Context
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.quiet.QuietApp
import app.quiet.service.QuietService
import app.quiet.ui.components.ChoiceRow
import app.quiet.ui.components.Note
import app.quiet.ui.components.PrimaryButton
import app.quiet.ui.components.QCard
import app.quiet.ui.components.SecondaryButton
import app.quiet.ui.components.SectionTitle
import app.quiet.ui.components.Segmented
import app.quiet.ui.components.StepperRow
import app.quiet.ui.components.SwitchRow
import app.quiet.ui.components.plural
import app.quiet.ui.components.unlockLabel
import app.quiet.ui.theme.Q
import kotlinx.coroutines.delay
import quiet.core.settings.Defaults
import quiet.core.settings.Mode
import quiet.core.settings.Preset
import quiet.core.settings.Settings
import quiet.core.settings.SettingsLock
import quiet.core.settings.UnlockKind

/**
 * Setup, in the order someone thinks about it: what Quiet is, how strict, the few choices people
 * change most, turning the service on (after the disclosure), and finally locking it all in.
 */
private enum class Step { WELCOME, PRESET, TUNE, DISCLOSURE, TURN_ON, KEEP_RUNNING, LOCK }

@Composable
fun OnboardingScreen() {
    var step by rememberSaveable { mutableStateOf(Step.WELCOME) }
    var preset by rememberSaveable { mutableStateOf(Preset.BALANCED) }
    var declined by rememberSaveable { mutableStateOf(false) }
    var lockSeconds by rememberSaveable { mutableIntStateOf(120) }
    val store = QuietApp.store
    val c = Q.colors
    val context = LocalContext.current

    fun back() {
        if (declined) declined = false else step = Step.entries[(step.ordinal - 1).coerceAtLeast(0)]
    }
    BackHandler(enabled = step != Step.WELCOME) { back() }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp, vertical = 16.dp),
    ) {
        Row(Modifier.height(40.dp), verticalAlignment = Alignment.CenterVertically) {
            if (step != Step.WELCOME) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).clickable { back() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = c.text)
                }
                Spacer(Modifier.width(8.dp))
            }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (s in Step.entries) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (s.ordinal <= step.ordinal) c.accent else c.surfaceHigh),
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        AnimatedContent(step to declined, Modifier.weight(1f), label = "onboarding") { (s, saidNo) ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                when (s) {
                    Step.WELCOME -> Welcome()
                    Step.PRESET -> ChoosePreset(preset) { preset = it }
                    Step.TUNE -> Tune()
                    Step.DISCLOSURE -> if (saidNo) Declined() else Disclosure()
                    Step.TURN_ON -> TurnOn()
                    Step.KEEP_RUNNING -> KeepRunning()
                    Step.LOCK -> LockItIn(lockSeconds) { lockSeconds = it }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        when (step) {
            Step.WELCOME -> PrimaryButton("Get started") { step = Step.PRESET }
            Step.PRESET -> PrimaryButton("Use ${preset.title}") {
                store.updateSettings { Defaults.preset(preset, it) }
                step = Step.TUNE
            }
            Step.TUNE -> PrimaryButton("Next") { step = Step.DISCLOSURE }
            Step.DISCLOSURE -> if (declined) {
                PrimaryButton("Go back") { declined = false }
                Spacer(Modifier.height(8.dp))
                SecondaryButton("Continue without it") { step = Step.LOCK }
            } else {
                PrimaryButton("Agree and continue") {
                    store.updateSettings { it.copy(disclosureAccepted = true) }
                    step = Step.TURN_ON
                }
                Spacer(Modifier.height(8.dp))
                SecondaryButton("No thanks") { declined = true }
            }
            Step.TURN_ON -> {
                var enabled by remember { mutableStateOf(QuietService.isEnabled(context)) }
                LaunchedEffect(Unit) {
                    while (true) {
                        enabled = QuietService.isEnabled(context)
                        delay(1000)
                    }
                }
                if (enabled) {
                    PrimaryButton("Quiet is on. Next") { step = Step.KEEP_RUNNING }
                } else {
                    PrimaryButton("Open Accessibility settings") { openAccessibilitySettings(context) }
                    Spacer(Modifier.height(8.dp))
                    SecondaryButton("Do it later") { step = Step.KEEP_RUNNING }
                }
            }
            Step.KEEP_RUNNING -> PrimaryButton("Next") { step = Step.LOCK }
            Step.LOCK -> PrimaryButton("Lock it in") {
                store.updateSettings {
                    it.copy(lockSeconds = lockSeconds, lockOpensAt = 0L, lockPromptSeen = true, onboarded = true)
                }
            }
        }
    }
}

@Composable
private fun Welcome() {
    val c = Q.colors
    Box(Modifier.size(64.dp).clip(CircleShape).background(c.accentDim), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.Spa, null, tint = c.accent, modifier = Modifier.size(32.dp))
    }
    Spacer(Modifier.height(24.dp))
    Text("Keep the apps.\nLose the scroll.", style = MaterialTheme.typography.displayMedium, color = c.text)
    Spacer(Modifier.height(16.dp))
    Text(
        "Quiet blocks the endless parts of Instagram, TikTok and YouTube: Reels, For You, Shorts. Your chats, your friends' posts and the videos people send you keep working.",
        style = MaterialTheme.typography.bodyLarge,
        color = c.textDim,
    )
    Spacer(Modifier.height(28.dp))
    Point(Icons.Rounded.ChatBubble, "Friends still get through", "A reel someone sends you plays. Swiping past it into the algorithm is what gets stopped.")
    Point(Icons.Rounded.Shield, "Blocked means a way out", "Tap Reels and you land in your messages. TikTok skips its For You feed and opens on your inbox.")
    Point(Icons.Rounded.Insights, "See where the time goes", "Daily time in every social app, when you scroll most, and your streak.")
}

@Composable
private fun Point(icon: ImageVector, title: String, body: String) {
    val c = Q.colors
    Row(Modifier.padding(vertical = 10.dp)) {
        Icon(icon, null, tint = c.accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, color = c.text)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = c.textDim)
        }
    }
}

@Composable
private fun Heading(title: String, body: String) {
    val c = Q.colors
    Text(title, style = MaterialTheme.typography.headlineLarge, color = c.text)
    Spacer(Modifier.height(8.dp))
    Text(body, style = MaterialTheme.typography.bodyLarge, color = c.textDim)
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun ChoosePreset(selected: Preset, onSelect: (Preset) -> Unit) {
    val c = Q.colors
    Heading("How strict?", "A starting point. You'll fine-tune it next.")
    for (p in Preset.entries) {
        val on = p == selected
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(if (on) c.accentDim else c.surface)
                .border(1.5.dp, if (on) c.accent else c.outline, RoundedCornerShape(20.dp))
                .clickable { onSelect(p) }
                .padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.title, style = MaterialTheme.typography.titleMedium, color = c.text, modifier = Modifier.weight(1f))
                if (on) Icon(Icons.Rounded.CheckCircle, null, tint = c.accent)
            }
            Spacer(Modifier.height(4.dp))
            Text(p.blurb, style = MaterialTheme.typography.bodyMedium, color = c.textDim)
        }
    }
}

/** The handful of choices people change most, on one page. */
@Composable
private fun Tune() {
    val store = QuietApp.store
    val s by store.settings.collectAsStateWithLifecycle()
    val rules by store.rules.collectAsStateWithLifecycle()
    fun edit(f: (Settings) -> Settings) = store.updateSettings(f)

    Heading("Make it yours", "The things people change most. Everything else is in Settings.")

    SectionTitle("Apps")
    QCard {
        for (app in rules.apps.values.sortedBy { listOf("instagram", "tiktok", "youtube").indexOf(it.app) }) {
            SwitchRow(app.displayName, s.app(app.app).enabled, summary(app, s)) { on ->
                edit { st -> st.withApp(app.app) { it.copy(enabled = on) } }
            }
        }
    }

    if (rules.apps["instagram"] != null && s.app("instagram").enabled) {
        val home = s.modeFor("instagram", "home")
        val keep = s.keepsVisible("instagram", "home")
        fun setHome(mode: Mode, keepStories: Boolean) = edit { st ->
            st.withApp("instagram") {
                it.copy(
                    modes = it.modes + ("home" to mode),
                    keepVisible = if (keepStories) it.keepVisible + "home" else it.keepVisible - "home",
                )
            }
        }
        SectionTitle("Instagram home feed")
        QCard {
            ChoiceRow("Leave it open", "Posts from people you follow, plus suggestions.", home == Mode.ALLOW) { setHome(Mode.ALLOW, keep) }
            ChoiceRow("Block the feed, keep Stories", "The Stories row stays tappable. The posts below are covered.", home == Mode.BLOCK && keep) {
                setHome(Mode.BLOCK, true)
            }
            ChoiceRow("Block all of it", "Home is covered. Messages and profiles still work.", home == Mode.BLOCK && !keep) {
                setHome(Mode.BLOCK, false)
            }
            if (home == Mode.LIMIT || home == Mode.PAUSE) {
                Note("Home is set to ${home.label}. You can change that under Apps later.")
            }
        }
    }

    SectionTitle("Unlocks")
    QCard {
        Note("When something's blocked, an unlock opens it for a bit. Each one gives you:")
        Spacer(Modifier.height(6.dp))
        for (kind in UnlockKind.entries) {
            val title = if (kind == UnlockKind.CHOOSE) "Let me pick each time" else unlockLabel(kind, s.unlockVideos, s.unlockMinutes)
            val sub = when (kind) {
                UnlockKind.VIDEOS -> "Swipe through ${plural(s.unlockVideos, "more video")}, then it locks again."
                UnlockKind.MINUTES -> "Feeds open for ${plural(s.unlockMinutes, "minute")}."
                UnlockKind.CHOOSE -> "The block screen offers both."
            }
            ChoiceRow(title, sub, s.unlocksEnabled && s.unlockKind == kind) {
                edit { it.copy(unlocksEnabled = true, unlockKind = kind) }
            }
        }
        ChoiceRow("No unlocks", "Blocked means blocked.", !s.unlocksEnabled) { edit { it.copy(unlocksEnabled = false) } }
        if (s.unlocksEnabled) {
            Spacer(Modifier.height(4.dp))
            StepperRow("Unlocks per day", s.unlocksPerDay, options = listOf(1, 2, 3, 4, 5, 8, 10, 20), format = { "$it" }) { v ->
                edit { it.copy(unlocksPerDay = v) }
            }
        }
    }
}

@Composable
private fun Disclosure() {
    Heading("Before you turn it on", "Quiet needs one permission. Here's exactly what it's for.")
    DisclosureBody()
}

@Composable
private fun Declined() {
    Heading(
        "Quiet can't work without it",
        "Without the Accessibility service Quiet can't see feeds, so it can't block anything or count your time. " +
            "You can turn it on later from the Today screen.",
    )
}

@Composable
private fun TurnOn() {
    val c = Q.colors
    val context = LocalContext.current
    Heading("Turn Quiet on", "Switch on Quiet's Accessibility service. You'll come straight back here.")
    Steps(
        "Tap Open Accessibility settings below.",
        "Find Quiet (it may be under Downloaded apps).",
        "Switch it on and allow.",
    )
    if (!isFromPlayStore(context)) {
        Spacer(Modifier.height(20.dp))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.surface).padding(16.dp),
        ) {
            Text("Switch greyed out?", style = MaterialTheme.typography.titleSmall, color = c.text)
            Spacer(Modifier.height(6.dp))
            Text(
                "Android blocks apps installed outside the Play Store from accessibility until you allow it. Open App info, tap the ⋮ menu at the top right, choose Allow restricted settings, then try again.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textDim,
            )
            Spacer(Modifier.height(12.dp))
            SecondaryButton("Open App info") { openAppInfo(context) }
        }
    }
}

@Composable
private fun KeepRunning() {
    val c = Q.colors
    val context = LocalContext.current
    Heading(
        "Keep it running",
        "Some phones stop background apps to save battery. Quiet barely uses any: it only looks when the screen changes.",
    )
    var exempt by remember { mutableStateOf(isBatteryExempt(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            exempt = isBatteryExempt(context)
            delay(1000)
        }
    }
    if (exempt) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CheckCircle, null, tint = c.good)
            Spacer(Modifier.width(10.dp))
            Text("Quiet can run in the background.", style = MaterialTheme.typography.titleSmall, color = c.text)
        }
    } else {
        Steps(
            "Tap Open App info below.",
            "Open App battery usage.",
            "Allow background usage, or choose Unrestricted.",
        )
        Spacer(Modifier.height(12.dp))
        SecondaryButton("Open App info") { openAppInfo(context) }
    }
    Spacer(Modifier.height(20.dp))
    Row {
        Icon(Icons.Rounded.BatteryChargingFull, null, tint = c.textFaint)
        Spacer(Modifier.width(10.dp))
        Text(
            "Tip: add the Focus tile to Quick Settings for one-tap focus hours.",
            style = MaterialTheme.typography.bodyMedium,
            color = c.textDim,
        )
    }
}

@Composable
private fun LockItIn(seconds: Int, onChange: (Int) -> Unit) {
    val c = Q.colors
    val store = QuietApp.store
    val s by store.settings.collectAsStateWithLifecycle()
    val rules by store.rules.collectAsStateWithLifecycle()
    Box(Modifier.size(56.dp).clip(CircleShape).background(c.accentDim), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.Lock, null, tint = c.accent, modifier = Modifier.size(28.dp))
    }
    Spacer(Modifier.height(18.dp))
    Heading(
        "Lock it in",
        "Pick how long future-you has to wait before making Quiet less strict. Making it stricter is always instant.",
    )
    Segmented(SettingsLock.SETUP_CHOICES, seconds, { SettingsLock.formatWait(it) }) { onChange(it) }
    Spacer(Modifier.height(16.dp))
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(c.warn.copy(alpha = 0.12f))
            .border(1.dp, c.warn.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        Text("Choose wisely", style = MaterialTheme.typography.titleSmall, color = c.warn)
        Spacer(Modifier.height(4.dp))
        Text(
            "Once it's locked, turning things off, allowing a feed or getting more unlocks means waiting " +
                "${SettingsLock.formatWait(seconds)} first. Shortening the wait later takes the wait too.",
            style = MaterialTheme.typography.bodyMedium,
            color = c.text,
        )
    }

    SectionTitle("Your setup")
    QCard {
        val apps = rules.apps.values.sortedBy { listOf("instagram", "tiktok", "youtube").indexOf(it.app) }
        for (app in apps) {
            Row(Modifier.padding(vertical = 4.dp)) {
                Text(app.displayName, style = MaterialTheme.typography.titleSmall, color = c.text, modifier = Modifier.width(96.dp))
                Text(summary(app, s), style = MaterialTheme.typography.bodyMedium, color = c.textDim)
            }
        }
        Row(Modifier.padding(vertical = 4.dp)) {
            Text("Unlocks", style = MaterialTheme.typography.titleSmall, color = c.text, modifier = Modifier.width(96.dp))
            Text(unlockSummary(s), style = MaterialTheme.typography.bodyMedium, color = c.textDim)
        }
    }
}

@Composable
private fun Steps(vararg lines: String) {
    val c = Q.colors
    lines.forEachIndexed { i, s ->
        Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(26.dp).clip(CircleShape).background(c.surfaceHigh), contentAlignment = Alignment.Center) {
                Text("${i + 1}", style = MaterialTheme.typography.labelMedium, color = c.accent)
            }
            Spacer(Modifier.width(12.dp))
            Text(s, style = MaterialTheme.typography.bodyLarge, color = c.text)
        }
    }
}

/** Play installs aren't affected by Android's restricted settings for accessibility, so the tip is only for sideloads. */
private fun isFromPlayStore(context: Context): Boolean = runCatching {
    val installer = if (Build.VERSION.SDK_INT >= 30) {
        context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.getInstallerPackageName(context.packageName)
    }
    installer == "com.android.vending"
}.getOrDefault(false)
