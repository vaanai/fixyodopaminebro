package app.quiet.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.quiet.LocalEditor
import app.quiet.QuietApp
import app.quiet.service.LiveState
import app.quiet.service.QuietService
import app.quiet.ui.components.AppIcon
import app.quiet.ui.components.Bar
import app.quiet.ui.components.QCard
import app.quiet.ui.components.Ring
import app.quiet.ui.components.SectionTitle
import app.quiet.ui.components.SmallButton
import app.quiet.ui.components.Stat
import app.quiet.ui.components.formatMinutes
import app.quiet.ui.theme.Q
import quiet.core.engine.Engine
import quiet.core.stats.DayStats
import quiet.core.stats.StatsMath
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TodayScreen() {
    val store = QuietApp.store
    val settings by store.settings.collectAsStateWithLifecycle()
    val history by store.history.collectAsStateWithLifecycle()
    val live by LiveState.state.collectAsStateWithLifecycle()
    val log by store.log.collectAsStateWithLifecycle()
    val rules by store.rules.collectAsStateWithLifecycle()
    val editor = LocalEditor.current
    val context = LocalContext.current
    val c = Q.colors
    val now = rememberNow()
    val todayKey = LocalDate.now().toString()
    val today = history[todayKey] ?: DayStats(todayKey)
    val enabled = QuietService.isEnabled(context)
    val running = live.connected && now - live.lastHeartbeat < 10_000

    Page {
        Spacer(Modifier.height(16.dp))
        Text(
            LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())),
            style = MaterialTheme.typography.labelMedium,
            color = c.textFaint,
        )
        Text(greeting(), style = MaterialTheme.typography.headlineLarge, color = c.text)
        Spacer(Modifier.height(16.dp))

        if (!enabled || !running) {
            QCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Warning, null, tint = c.warn)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (!enabled) "Quiet is switched off" else "Quiet is starting up",
                        style = MaterialTheme.typography.titleMedium,
                        color = c.text,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    if (!enabled) "Turn on Quiet in Accessibility settings so it can see feeds and count your time."
                    else "If this doesn't clear in a few seconds, switch Quiet off and on again in Accessibility settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textDim,
                )
                Spacer(Modifier.height(12.dp))
                SmallButton("Open Accessibility settings", accent = true) { openAccessibilitySettings(context) }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (!settings.protectionOn) {
            QCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.PauseCircle, null, tint = c.warn)
                    Spacer(Modifier.width(10.dp))
                    Text("Protection is paused", style = MaterialTheme.typography.titleMedium, color = c.text, modifier = Modifier.weight(1f))
                    SmallButton("Resume", accent = true) { editor.edit { it.copy(protectionOn = true) } }
                }
                Text("Quiet is still counting time, but nothing is blocked.", style = MaterialTheme.typography.bodyMedium, color = c.textDim)
            }
            Spacer(Modifier.height(12.dp))
        }

        // Hero: endless-feed time against the allowance.
        QCard(padding = 22.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val budget = settings.budgetMinutes * 60
                Ring(if (budget > 0) today.feedSeconds.toFloat() / budget else 0f, color = if (today.feedSeconds > budget) c.warn else c.accent) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(formatMinutes(today.feedSeconds).ifEmpty { "0m" }, style = MaterialTheme.typography.displayMedium, color = c.text)
                        Text("of ${settings.budgetMinutes}m", style = MaterialTheme.typography.bodySmall, color = c.textDim)
                    }
                }
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Stat(formatMinutes(today.feedSeconds), "in endless feeds")
                    Stat(formatMinutes(today.viewerSeconds), "on videos friends sent")
                    Stat(formatMinutes(today.totalAppSeconds), "in social apps")
                }
            }
            val streak = StatsMath.streak(history, LocalDate.now(), settings.budgetMinutes)
            if (streak >= 2) {
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.LocalFireDepartment, null, tint = c.warn, modifier = Modifier.padding(end = 6.dp))
                    Text("$streak days in a row within your allowance", style = MaterialTheme.typography.labelLarge, color = c.text)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        QCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat(today.blocks.toString(), "feeds turned\naway", Modifier.weight(1f))
                Stat(today.swipesStopped.toString(), "swipes\nstopped", Modifier.weight(1f))
                Stat(today.skips.toString(), "auto-skips", Modifier.weight(1f))
                val left = (settings.unlocksPerDay - today.unlocks).coerceAtLeast(0)
                Stat(if (settings.strictMode || !settings.unlocksEnabled) "–" else "$left", "unlocks\nleft", Modifier.weight(1f))
            }
        }

        // Live unlocks.
        val passes = live.engine?.passes.orEmpty().filter { it.value > now && !it.key.contains('.') }
        for ((app, until) in passes) {
            Spacer(Modifier.height(12.dp))
            QCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.LockOpen, null, tint = c.warn)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${rules.apps[app]?.displayName ?: app} is unlocked", style = MaterialTheme.typography.titleSmall, color = c.text)
                        Text("${Engine.formatClock(until - now)} left", style = MaterialTheme.typography.bodySmall, color = c.textDim)
                    }
                    SmallButton("Lock now") { QuietService.instance?.relock(app) }
                }
            }
        }

        // Deliberate unlocks from here, so they don't depend on catching the block screen.
        val focusNow = Engine.isFocusActive(settings, now, ZoneId.systemDefault())
        val unlocksLeft = settings.unlocksPerDay - today.unlocks
        if (settings.protectionOn && settings.canUnlock(focusNow) && unlocksLeft > 0 && passes.isEmpty()) {
            Spacer(Modifier.height(12.dp))
            QCard {
                Text("Want to watch a bit?", style = MaterialTheme.typography.titleSmall, color = c.text)
                Text(
                    "Uses one of your $unlocksLeft unlocks left today. Then open the app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textDim,
                )
                Spacer(Modifier.height(10.dp))
                for (app in rules.apps.values.filter { settings.app(it.app).enabled }) {
                    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(app.displayName, style = MaterialTheme.typography.bodyMedium, color = c.text, modifier = Modifier.weight(1f))
                        if (settings.unlockVideos > 0) {
                            SmallButton("${settings.unlockVideos} more") { QuietService.instance?.unlockFromApp(app.app, videos = true) }
                            Spacer(Modifier.width(8.dp))
                        }
                        SmallButton("${settings.unlockMinutes} min") { QuietService.instance?.unlockFromApp(app.app, videos = false) }
                    }
                }
            }
        }

        FocusCard(settings.focusUntil, now, settings.strictMode, Engine.isFocusActive(settings, now, ZoneId.systemDefault())) { until ->
            editor.edit { it.copy(focusUntil = until) }
        }

        // Per-app time.
        val apps = settings.tracked
            .map { it to (today.appSeconds[it.packageName] ?: 0) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
        if (apps.isNotEmpty()) {
            SectionTitle("Time in apps today")
            QCard {
                val max = apps.first().second.coerceAtLeast(1)
                apps.forEachIndexed { i, (t, secs) ->
                    if (i > 0) Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(t.packageName, t.label, 34.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row {
                                Text(t.label, style = MaterialTheme.typography.titleSmall, color = c.text, modifier = Modifier.weight(1f))
                                Text(formatMinutes(secs), style = MaterialTheme.typography.titleSmall, color = c.text)
                            }
                            Spacer(Modifier.height(6.dp))
                            val limit = t.dailyLimitMinutes.takeIf { it > 0 }
                                ?: rules.forPackage(t.packageName)?.let { settings.app(it.app).dailyLimitMinutes }?.takeIf { it > 0 }
                            Bar(
                                if (limit != null) secs / (limit * 60f) else secs.toFloat() / max,
                                color = if (limit != null && secs >= limit * 60) c.warn else c.accent,
                            )
                            val opens = today.opens[t.packageName] ?: 0
                            Spacer(Modifier.height(4.dp))
                            Text(
                                buildString {
                                    append("$opens ${if (opens == 1) "open" else "opens"}")
                                    if (limit != null) append(" · limit ${limit}m")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = c.textFaint,
                            )
                        }
                    }
                }
            }
        }

        if (log.isNotEmpty()) {
            SectionTitle("Recently")
            QCard {
                log.take(6).forEachIndexed { i, e ->
                    if (i > 0) Spacer(Modifier.height(10.dp))
                    Row {
                        Text(clockOf(e.at), style = MaterialTheme.typography.labelMedium, color = c.textFaint, modifier = Modifier.width(64.dp))
                        Text("${e.app} · ${e.text}", style = MaterialTheme.typography.bodyMedium, color = c.textDim)
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusCard(focusUntil: Long, now: Long, strict: Boolean, active: Boolean, set: (Long) -> Unit) {
    val c = Q.colors
    Spacer(Modifier.height(12.dp))
    QCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Bedtime, null, tint = if (active) c.accent else c.textDim)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(if (active) "Focus is on" else "Focus", style = MaterialTheme.typography.titleSmall, color = c.text)
                Text(
                    when {
                        focusUntil > now -> "Every feed is off until ${clockOf(focusUntil)}"
                        active -> "A focus schedule is running"
                        else -> "Turn every feed off for a while. Chats keep working."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textDim,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (focusUntil > now) {
                SmallButton("+30 min") { set(focusUntil + 30 * 60_000L) }
                if (!strict) SmallButton("End now") { set(0L) }
            } else {
                SmallButton("30 min") { set(now + 30 * 60_000L) }
                SmallButton("1 hour") { set(now + 60 * 60_000L) }
                SmallButton("2 hours") { set(now + 120 * 60_000L) }
                SmallButton("Rest of day") {
                    val end = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    set(end)
                }
            }
        }
    }
}

private fun greeting(): String {
    val h = java.time.LocalTime.now().hour
    return when (h) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        in 18..22 -> "Good evening"
        else -> "Late one, huh"
    }
}
