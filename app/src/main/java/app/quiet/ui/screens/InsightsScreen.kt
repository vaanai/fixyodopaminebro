package app.quiet.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.quiet.QuietApp
import app.quiet.ui.components.AppIcon
import app.quiet.ui.components.Bar
import app.quiet.ui.components.QCard
import app.quiet.ui.components.ScreenHeader
import app.quiet.ui.components.SectionTitle
import app.quiet.ui.components.Segmented
import app.quiet.ui.components.Stat
import app.quiet.ui.components.formatMinutes
import app.quiet.ui.theme.Q
import quiet.core.stats.StatsMath
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun InsightsScreen() {
    val store = QuietApp.store
    val settings by store.settings.collectAsStateWithLifecycle()
    val history by store.history.collectAsStateWithLifecycle()
    val c = Q.colors
    var range by remember { mutableIntStateOf(7) }
    val today = LocalDate.now()
    val days = StatsMath.lastDays(history, today, range)
    // Averages only cover days Quiet was actually counting, not the days before it was installed.
    val counted = days.filter { history.containsKey(it.date) }
    val countedDays = counted.size.coerceAtLeast(1)

    Page {
        ScreenHeader("Insights", "Where your scrolling time goes.")
        Spacer(Modifier.height(8.dp))
        Segmented(listOf(7, 14, 30), range, { "$it days" }) { range = it }

        SectionTitle("Endless feeds per day")
        QCard {
            val budget = settings.budgetMinutes * 60
            val max = (days.maxOfOrNull { it.shortFormSeconds } ?: 0).coerceAtLeast(budget).coerceAtLeast(60)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Legend(c.accent, "Feeds")
                Spacer(Modifier.width(14.dp))
                Legend(c.accent.copy(alpha = 0.4f), "Single videos")
                Spacer(Modifier.width(14.dp))
                Legend(c.warn, "Allowance")
            }
            Spacer(Modifier.height(14.dp))
            val accent = c.accent
            val warn = c.warn
            val track = c.surfaceHigh
            Canvas(Modifier.fillMaxWidth().height(160.dp)) {
                val n = days.size
                val gap = if (n > 14) 3.dp.toPx() else 8.dp.toPx()
                val barW = (size.width - gap * (n - 1)) / n
                days.forEachIndexed { i, d ->
                    val x = i * (barW + gap)
                    drawRoundRect(track, Offset(x, 0f), Size(barW, size.height), CornerRadius(6f))
                    val feedH = size.height * d.feedSeconds / max
                    val viewH = size.height * d.viewerSeconds / max
                    if (viewH > 0) drawRoundRect(accent.copy(alpha = 0.4f), Offset(x, size.height - feedH - viewH), Size(barW, viewH + feedH), CornerRadius(6f))
                    if (feedH > 0) drawRoundRect(accent, Offset(x, size.height - feedH), Size(barW, feedH), CornerRadius(6f))
                }
                val y = size.height - size.height * budget / max
                drawLine(warn, Offset(0f, y), Offset(size.width, y), 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
            }
            Spacer(Modifier.height(6.dp))
            if (range == 7) {
                Row(Modifier.fillMaxWidth()) {
                    for (d in days) {
                        Text(
                            LocalDate.parse(d.date).dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall,
                            color = c.textFaint,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            val total = days.sumOf { it.feedSeconds }
            val avg = total / countedDays
            Row {
                Stat(formatMinutes(avg), "a day on average", Modifier.weight(1f))
                Stat(StatsMath.streak(history, today, settings.budgetMinutes).toString(), "day streak", Modifier.weight(1f))
                Stat(counted.count { it.feedSeconds <= budget }.toString() + "/" + counted.size, "days within allowance", Modifier.weight(1f))
            }
        }

        SectionTitle("What Quiet did")
        QCard {
            Row {
                Stat(days.sumOf { it.blocks }.toString(), "feeds turned away", Modifier.weight(1f))
                Stat(days.sumOf { it.swipesStopped }.toString(), "swipes stopped", Modifier.weight(1f))
                Stat(days.sumOf { it.skips }.toString(), "auto-skips", Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            Row {
                Stat(days.sumOf { it.videos.values.sum() }.toString(), "videos watched", Modifier.weight(1f))
                Stat(days.sumOf { it.unlocks }.toString(), "unlocks used", Modifier.weight(1f))
                Stat(days.sumOf { it.pausesLeft }.toString(), "times you walked away at a pause", Modifier.weight(1f))
            }
        }

        SectionTitle("When you scroll")
        QCard {
            val hours = IntArray(24)
            for (d in days) d.hourShortForm.forEachIndexed { h, v -> hours[h] += v }
            val peak = hours.maxOrNull()?.coerceAtLeast(1) ?: 1
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                for (h in 0 until 24) {
                    val f = hours[h].toFloat() / peak
                    Box(
                        Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (hours[h] == 0) c.surfaceHigh else c.accent.copy(alpha = 0.15f + 0.85f * f)),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                for (label in listOf("12am", "6am", "12pm", "6pm", "11pm")) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = c.textFaint)
                }
            }
            val worst = hours.indices.maxByOrNull { hours[it] }
            if (worst != null && hours[worst] > 0) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Your heaviest hour is ${formatTime(worst * 60)}–${formatTime(((worst + 1) % 24) * 60)}. A focus window there would do the most.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textDim,
                )
            }
        }

        SectionTitle("Apps")
        QCard {
            val totals = settings.tracked.map { t -> t to days.sumOf { it.appSeconds[t.packageName] ?: 0 } }
                .filter { it.second > 0 }
                .sortedByDescending { it.second }
            if (totals.isEmpty()) {
                Text("Nothing yet. Time shows up here once Quiet is running.", style = MaterialTheme.typography.bodyMedium, color = c.textDim)
            }
            val max = totals.firstOrNull()?.second?.coerceAtLeast(1) ?: 1
            totals.forEachIndexed { i, (t, secs) ->
                if (i > 0) Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(t.packageName, t.label, 30.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row {
                            Text(t.label, style = MaterialTheme.typography.titleSmall, color = c.text, modifier = Modifier.weight(1f))
                            Text(
                                "${formatMinutes(secs)} · ${formatMinutes(secs / countedDays)}/day",
                                style = MaterialTheme.typography.labelMedium,
                                color = c.textDim,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Bar(secs.toFloat() / max)
                    }
                }
            }
        }

        SectionTitle("Inside the apps")
        QCard {
            val rules = store.rules.value
            val surfaceTotals = mutableMapOf<String, Int>()
            for (d in days) d.surfaceSeconds.forEach { (k, v) -> surfaceTotals[k] = (surfaceTotals[k] ?: 0) + v }
            val sorted = surfaceTotals.entries.sortedByDescending { it.value }.take(8)
            if (sorted.isEmpty()) Text("No data yet.", style = MaterialTheme.typography.bodyMedium, color = c.textDim)
            val max = sorted.firstOrNull()?.value?.coerceAtLeast(1) ?: 1
            sorted.forEachIndexed { i, (key, secs) ->
                if (i > 0) Spacer(Modifier.height(10.dp))
                val app = rules.apps[key.substringBefore('.')]
                val label = "${app?.displayName ?: key.substringBefore('.')} · ${app?.surface(key.substringAfter('.'))?.label ?: key.substringAfter('.')}"
                Row {
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = c.text, modifier = Modifier.weight(1f))
                    Text(formatMinutes(secs), style = MaterialTheme.typography.labelMedium, color = c.textDim)
                }
                Spacer(Modifier.height(4.dp))
                Bar(secs.toFloat() / max, height = 4.dp)
            }
        }
    }
}

@Composable
private fun Legend(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Q.colors.textDim)
    }
}
