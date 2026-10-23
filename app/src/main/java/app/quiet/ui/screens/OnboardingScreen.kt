package app.quiet.ui.screens

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
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Insights
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
import app.quiet.QuietApp
import app.quiet.ui.components.PrimaryButton
import app.quiet.ui.components.SecondaryButton
import app.quiet.ui.theme.Q
import kotlinx.coroutines.delay
import quiet.core.settings.Defaults
import quiet.core.settings.Preset

@Composable
fun OnboardingScreen() {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var preset by rememberSaveable { mutableStateOf(Preset.BALANCED) }
    val c = Q.colors
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp, vertical = 16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(4) { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (i <= step) c.accent else c.surfaceHigh),
                )
            }
        }
        Spacer(Modifier.height(28.dp))
        AnimatedContent(step, Modifier.weight(1f), label = "onboarding") { s ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                when (s) {
                    0 -> Welcome()
                    1 -> ChoosePreset(preset) { preset = it }
                    2 -> TurnOn()
                    else -> KeepRunning()
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        val context = LocalContext.current
        when (step) {
            0 -> PrimaryButton("Get started") { step = 1 }
            1 -> PrimaryButton("Use ${preset.title}") {
                QuietApp.store.updateSettings { Defaults.preset(preset, it) }
                step = 2
            }
            2 -> {
                var enabled by remember { mutableStateOf(app.quiet.service.QuietService.isEnabled(context)) }
                LaunchedEffect(Unit) {
                    while (true) {
                        enabled = app.quiet.service.QuietService.isEnabled(context)
                        delay(1000)
                    }
                }
                if (enabled) {
                    PrimaryButton("Quiet is on. Next") { step = 3 }
                } else {
                    PrimaryButton("Open Accessibility settings") { openAccessibilitySettings(context) }
                    Spacer(Modifier.height(8.dp))
                    SecondaryButton("Do it later") { step = 3 }
                }
            }
            else -> PrimaryButton("Done") { QuietApp.store.updateSettings { it.copy(onboarded = true) } }
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
        "Quiet blocks the endless parts of Instagram, TikTok and YouTube: Reels, For You, Shorts. Your chats, friends and the videos people send you keep working.",
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
private fun ChoosePreset(selected: Preset, onSelect: (Preset) -> Unit) {
    val c = Q.colors
    Text("How strict?", style = MaterialTheme.typography.headlineLarge, color = c.text)
    Spacer(Modifier.height(8.dp))
    Text("You can change every detail later.", style = MaterialTheme.typography.bodyLarge, color = c.textDim)
    Spacer(Modifier.height(20.dp))
    for (p in Preset.entries) {
        val on = p == selected
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(if (on) c.accentDim else c.surface)
                .border(1.5.dp, if (on) c.accent else c.outline, RoundedCornerShape(20.dp))
                .clickableNoRipple { onSelect(p) }
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

@Composable
private fun TurnOn() {
    val c = Q.colors
    val context = LocalContext.current
    Text("Turn Quiet on", style = MaterialTheme.typography.headlineLarge, color = c.text)
    Spacer(Modifier.height(12.dp))
    Text(
        "Quiet works as an accessibility service: that's how it can tell a Reels feed from a chat. Everything stays on your phone.",
        style = MaterialTheme.typography.bodyLarge,
        color = c.textDim,
    )
    Spacer(Modifier.height(20.dp))
    Steps(
        "Tap Open Accessibility settings below.",
        "Find Quiet (it may be under Downloaded apps).",
        "Switch it on and allow.",
    )
    Spacer(Modifier.height(20.dp))
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.surface).padding(16.dp),
    ) {
        Text("Switch greyed out?", style = MaterialTheme.typography.titleSmall, color = c.text)
        Spacer(Modifier.height(6.dp))
        Text(
            "Android blocks sideloaded apps from accessibility until you allow it. Open App info, tap the ⋮ menu at the top right, choose Allow restricted settings, then try again.",
            style = MaterialTheme.typography.bodyMedium,
            color = c.textDim,
        )
        Spacer(Modifier.height(12.dp))
        SecondaryButton("Open App info") { openAppInfo(context) }
    }
}

@Composable
private fun KeepRunning() {
    val c = Q.colors
    val context = LocalContext.current
    Text("Keep it running", style = MaterialTheme.typography.headlineLarge, color = c.text)
    Spacer(Modifier.height(12.dp))
    Text(
        "Android sometimes puts background apps to sleep. Exempting Quiet from battery optimisation keeps it awake. It uses almost no battery: it only looks when the screen changes.",
        style = MaterialTheme.typography.bodyLarge,
        color = c.textDim,
    )
    Spacer(Modifier.height(20.dp))
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
            Text("Quiet is exempt from battery optimisation.", style = MaterialTheme.typography.titleSmall, color = c.text)
        }
    } else {
        SecondaryButton("Exempt Quiet") { requestBatteryExemption(context) }
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

@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.then(
        Modifier.clickable(
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
    )

