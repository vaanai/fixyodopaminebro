package app.quiet.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.quiet.LocalEditor
import app.quiet.LocalNav
import app.quiet.QuietApp
import app.quiet.Route
import app.quiet.ui.components.AppIcon
import app.quiet.ui.components.Chip
import app.quiet.ui.components.Divider
import app.quiet.ui.components.NavRow
import app.quiet.ui.components.QCard
import app.quiet.ui.components.ScreenHeader
import app.quiet.ui.components.SectionTitle
import app.quiet.ui.components.Segmented
import app.quiet.ui.components.StepperRow
import app.quiet.ui.components.SwitchRow
import app.quiet.ui.components.Tag
import app.quiet.ui.components.isInstalled
import app.quiet.ui.theme.Q
import quiet.core.rules.AppRules
import quiet.core.rules.SurfaceKind
import quiet.core.settings.Mode
import quiet.core.settings.Origin
import quiet.core.settings.Settings
import quiet.core.settings.UNLIMITED

val Mode.label: String
    get() = when (this) {
        Mode.ALLOW -> "Allow"
        Mode.PAUSE -> "Pause"
        Mode.LIMIT -> "Limit"
        Mode.BLOCK -> "Block"
    }

private fun Mode.explain(budget: Int, pauseSeconds: Int, pauseMinutes: Int): String = when (this) {
    Mode.ALLOW -> "Open as normal. Time is still counted."
    Mode.PAUSE -> "A ${pauseSeconds}s breather first, then $pauseMinutes minutes at a time."
    Mode.LIMIT -> "Counts against your shared $budget-minute daily allowance, then blocks."
    Mode.BLOCK -> "Covered. You can still jump to chats and other tabs."
}

fun swipesLabel(n: Int): String = when (n) {
    UNLIMITED -> "No cap"
    0 -> "Just that one"
    1 -> "+1 more"
    else -> "+$n more"
}

private val Mode.done: String
    get() = when (this) {
        Mode.ALLOW -> "allowed"
        Mode.PAUSE -> "paused"
        Mode.LIMIT -> "limited"
        Mode.BLOCK -> "blocked"
    }

/** Kinds that are never blocked, so they get no Allow/Block choice: chats, profiles, and single videos (set by swipes). */
private val NOT_FEEDS = setOf(SurfaceKind.MESSAGES, SurfaceKind.PROFILE, SurfaceKind.VIDEO_VIEWER)

/** The surfaces that get an Allow, Pause, Limit or Block choice. */
fun feeds(rules: AppRules) = rules.surfaces.filter { it.kind !in NOT_FEEDS }

/** One-line summary of an app's setup: "Reels tab blocked, Home feed blocked past Stories". */
fun summary(rules: AppRules, s: Settings): String {
    val a = s.app(rules.app)
    if (!a.enabled) return "Not managed. Time is still counted."
    val blocked = feeds(rules)
        .filter { s.modeFor(rules.app, it.id) != Mode.ALLOW }
        .joinToString { surface ->
            val top = surface.openTop?.takeIf { t -> t.surface.let { to -> to == null || s.modeFor(rules.app, to) != Mode.BLOCK } }
            val kept = top?.let { " past ${it.name}" }.orEmpty()
            "${surface.label} ${s.modeFor(rules.app, surface.id).done}$kept"
        }
    return blocked.ifEmpty { "Everything allowed" }
}

/** "A", "A and B", "A, B and C". */
fun joinWithAnd(items: List<String>): String = when (items.size) {
    0 -> ""
    1 -> items[0]
    else -> items.dropLast(1).joinToString() + " and " + items.last()
}

/** Where single videos can be opened from in this app, so the swipe settings only list real places. */
fun origins(rules: AppRules): List<Origin> {
    val kinds = rules.surfaces.map { it.kind }.toSet()
    return Origin.entries.filter { o ->
        when (o) {
            Origin.MESSAGES -> SurfaceKind.MESSAGES in kinds
            Origin.PROFILE -> SurfaceKind.PROFILE in kinds
            Origin.FEED -> SurfaceKind.FEED in kinds
            Origin.EXPLORE -> SurfaceKind.EXPLORE in kinds
            Origin.STORIES -> SurfaceKind.STORIES in kinds
            Origin.OTHER -> true
        }
    }
}

@Composable
fun AppsScreen() {
    val store = QuietApp.store
    val settings by store.settings.collectAsStateWithLifecycle()
    val rules by store.rules.collectAsStateWithLifecycle()
    val nav = LocalNav.current
    val editor = LocalEditor.current
    val context = LocalContext.current
    val c = Q.colors
    Page {
        ScreenHeader("Apps", "Choose what's open, what's limited and what's off.")
        SectionTitle("Short-form video")
        for (app in rules.apps.values.sortedBy { listOf("instagram", "tiktok", "youtube").indexOf(it.app) }) {
            val a = settings.app(app.app)
            val installed = app.packages.any { isInstalled(context.packageManager, it) }
            QCard(onClick = { nav.go(Route.AppDetail(app.app)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(app.packages.first { isInstalled(context.packageManager, it) || it == app.packages.last() }, app.displayName, 44.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(app.displayName, style = MaterialTheme.typography.titleMedium, color = c.text)
                            if (!installed) {
                                Spacer(Modifier.width(8.dp))
                                Tag("Not installed", c.textFaint)
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(summary(app, settings), style = MaterialTheme.typography.bodySmall, color = c.textDim)
                    }
                    Switch(
                        checked = a.enabled,
                        onCheckedChange = { on -> editor.edit { s -> s.withApp(app.app) { it.copy(enabled = on) } } },
                        colors = SwitchDefaults.colors(checkedTrackColor = c.accent, checkedThumbColor = c.onAccent, uncheckedTrackColor = c.surfaceHigh),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        SectionTitle("Everything else")
        QCard {
            NavRow(
                "Tracked apps",
                "Count time in any app and give it a daily limit.",
                Icons.Rounded.Apps,
                value = settings.tracked.count { it.dailyLimitMinutes > 0 }.let { if (it > 0) "$it limited" else null },
            ) { nav.go(Route.Tracked) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppDetailScreen(appId: String) {
    val store = QuietApp.store
    val settings by store.settings.collectAsStateWithLifecycle()
    val rulesState by store.rules.collectAsStateWithLifecycle()
    val nav = LocalNav.current
    val editor = LocalEditor.current
    val c = Q.colors
    val rules = rulesState.apps[appId] ?: return
    val a = settings.app(appId)

    fun update(f: (quiet.core.settings.AppSettings) -> quiet.core.settings.AppSettings) =
        editor.edit { s -> s.withApp(appId, f) }

    Page {
        ScreenHeader(rules.displayName, onBack = { nav.back() })
        QCard {
            SwitchRow("Manage ${rules.displayName}", a.enabled, "Off means Quiet only counts time here.") { on -> update { it.copy(enabled = on) } }
        }

        SectionTitle("Feeds")
        QCard {
            feeds(rules).forEachIndexed { i, surface ->
                if (i > 0) {
                    Spacer(Modifier.height(10.dp))
                    Divider()
                    Spacer(Modifier.height(10.dp))
                }
                val mode = settings.modeFor(appId, surface.id)
                Text(surface.label, style = MaterialTheme.typography.titleSmall, color = c.text)
                if (surface.description.isNotBlank()) {
                    Text(surface.description, style = MaterialTheme.typography.bodySmall, color = c.textFaint)
                }
                Spacer(Modifier.height(10.dp))
                Segmented(Mode.entries, mode, { it.label }) { m -> update { it.copy(modes = it.modes + (surface.id to m)) } }
                Spacer(Modifier.height(6.dp))
                Text(mode.explain(settings.budgetMinutes, settings.pauseSeconds, settings.pauseMinutes), style = MaterialTheme.typography.bodySmall, color = c.textDim)
                val top = surface.openTop
                if (top != null && mode != Mode.ALLOW) {
                    Spacer(Modifier.height(4.dp))
                    val topBlocked = top.surface?.let { settings.modeFor(appId, it) == Mode.BLOCK } == true
                    Text(
                        if (topBlocked) "${top.name} are blocked too, so all of ${surface.label} is covered."
                        else "The top, with ${top.name}, stays open. The block starts when you scroll down past ${top.name}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textDim,
                    )
                }
            }
        }

        SectionTitle("Single videos")
        QCard {
            Text(
                "Opening one video from a chat, a profile or search always works. These set how far you can swipe on from it before Quiet steps in.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textDim,
            )
            Spacer(Modifier.height(6.dp))
            val options = listOf(0, 1, 2, 3, 5, 10, 20, UNLIMITED)
            for (origin in origins(rules)) {
                StepperRow(
                    origin.label,
                    settings.viewerSwipesFor(appId, origin),
                    options = options,
                    format = ::swipesLabel,
                ) { n -> update { it.copy(viewerSwipes = it.viewerSwipes + (origin to n)) } }
            }
        }

        val videoFeeds = rules.surfaces.filter { it.kind == SurfaceKind.VIDEO_FEED }
        if (rules.nav.isNotEmpty() && videoFeeds.isNotEmpty()) {
            SectionTitle("When a blocked video feed opens")
            QCard {
                Text(
                    "Cover it, or jump straight to another tab. Handy for apps that open on the feed. Applies to ${joinWithAnd(videoFeeds.map { it.label })}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textDim,
                )
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("Just cover it", a.autoSkipTarget == null) { update { it.copy(autoSkipTarget = null) } }
                    for (t in rules.nav) {
                        Chip("Jump to ${t.label}", a.autoSkipTarget == t.id) { update { it.copy(autoSkipTarget = t.id) } }
                    }
                }
            }
        }

        SectionTitle("Daily limit")
        QCard {
            StepperRow(
                "Whole app",
                a.dailyLimitMinutes,
                subtitle = "Once you've spent this long in ${rules.displayName} today, all of it is covered until midnight.",
                options = listOf(0, 10, 15, 20, 30, 45, 60, 90, 120, 180),
                format = { if (it == 0) "Off" else "${it}m" },
            ) { n -> update { it.copy(dailyLimitMinutes = n) } }
            if (a.dailyLimitMinutes > 0) {
                SwitchRow(
                    "Chats stay open",
                    settings.keepMessagesOpen,
                    if (settings.keepMessagesOpen) "Your messages still work after the limit. This goes for every app." else "The limit covers chats too. This goes for every app.",
                ) { on -> editor.edit { it.copy(keepMessagesOpen = on) } }
            }
        }

        SectionTitle("Detection")
        QCard {
            Text("Rules: ${rulesState.sources[appId] ?: "?"}${if (!rules.verified) " · not yet verified on a device" else ""}", style = MaterialTheme.typography.bodySmall, color = c.textDim)
            rulesState.errors[appId]?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = c.danger) }
            Spacer(Modifier.height(6.dp))
            Text("Packages: ${rules.packages.joinToString()}", style = MaterialTheme.typography.bodySmall, color = c.textFaint)
        }
    }
}
