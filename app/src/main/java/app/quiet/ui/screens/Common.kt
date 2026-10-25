package app.quiet.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.quiet.QuietApp
import app.quiet.Tab
import app.quiet.ui.components.SmallButton
import app.quiet.ui.theme.Q
import kotlinx.coroutines.delay
import quiet.core.engine.Engine
import quiet.core.settings.SettingsLock

/** Standard scrolling page with the screen's gutters and insets. */
@Composable
fun Page(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(bottom = 32.dp),
        content = content,
    )
}

@Composable
fun BottomBar(current: Tab, onSelect: (Tab) -> Unit) {
    val c = Q.colors
    val items = listOf(
        Triple(Tab.TODAY, "Today", Icons.Rounded.Today),
        Triple(Tab.APPS, "Apps", Icons.Rounded.Apps),
        Triple(Tab.INSIGHTS, "Insights", Icons.Rounded.Insights),
        Triple(Tab.SETTINGS, "Settings", Icons.Rounded.Tune),
    )
    Row(
        Modifier
            .fillMaxWidth()
            .background(c.surface)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        for ((tab, label, icon) in items) {
            val on = tab == current
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelect(tab) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (on) c.accentDim else androidx.compose.ui.graphics.Color.Transparent)
                        .padding(horizontal = 18.dp, vertical = 5.dp),
                ) {
                    Icon(icon, label, tint = if (on) c.accent else c.textFaint, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.height(3.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = if (on) c.text else c.textFaint)
            }
        }
    }
}

/**
 * Shown when a change would make Quiet less strict while the settings lock is on. Starting the wait
 * is saved, so it keeps running if you leave Quiet; once it ends, settings stay open for a few minutes.
 */
@Composable
fun LockDialog(onApply: () -> Unit, onCancel: () -> Unit) {
    val c = Q.colors
    val store = QuietApp.store
    val s by store.settings.collectAsStateWithLifecycle()
    val now = rememberNow()
    val state = SettingsLock.state(s, now)
    val openMinutes = SettingsLock.OPEN_MS / 60_000
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = c.surface,
        icon = { Icon(if (state is SettingsLock.State.Locked) Icons.Rounded.Lock else Icons.Rounded.LockOpen, null, tint = c.accent) },
        title = {
            Text(
                when (state) {
                    is SettingsLock.State.Locked -> "Settings are locked"
                    is SettingsLock.State.Waiting -> "Unlocking settings"
                    else -> "Settings are open"
                },
                color = c.text,
            )
        },
        text = {
            Text(
                when (state) {
                    is SettingsLock.State.Locked ->
                        "This makes Quiet less strict, so it waits ${SettingsLock.formatWait(s.lockSeconds)} first: the lock you chose. " +
                            "Making Quiet stricter is always instant."
                    is SettingsLock.State.Waiting ->
                        "${Engine.formatClock(state.opensAt - now)} to go. You can leave Quiet, the wait keeps running. " +
                            "Afterwards settings stay open for $openMinutes minutes."
                    is SettingsLock.State.Open -> "Go ahead. Settings lock again in ${Engine.formatClock(state.closesAt - now)}."
                    SettingsLock.State.Off -> "Settings aren't locked."
                },
                color = c.textDim,
            )
        },
        confirmButton = {
            when (state) {
                is SettingsLock.State.Locked -> TextButton(onClick = {
                    store.updateSettings { SettingsLock.startWait(it, System.currentTimeMillis()) }
                }) { Text("Start the wait", color = c.accent) }
                is SettingsLock.State.Waiting -> TextButton(onClick = {}, enabled = false) {
                    Text("Wait ${Engine.formatClock(state.opensAt - now)}", color = c.textFaint)
                }
                else -> TextButton(onClick = onApply) { Text("Make the change", color = c.accent) }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(if (state is SettingsLock.State.Locked) "Keep it locked" else "Never mind", color = c.text)
            }
        },
    )
}

/** The settings lock at a glance, with the one action that makes sense right now. */
@Composable
fun LockStatus(s: quiet.core.settings.Settings, now: Long, onSetUp: (() -> Unit)?) {
    val c = Q.colors
    val store = QuietApp.store
    val state = SettingsLock.state(s, now)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (state is SettingsLock.State.Locked || state is SettingsLock.State.Waiting) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
            null,
            tint = if (state == SettingsLock.State.Off) c.textDim else c.accent,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                when (state) {
                    SettingsLock.State.Off -> "Settings lock is off"
                    is SettingsLock.State.Locked -> "Settings are locked"
                    is SettingsLock.State.Waiting -> "Unlocking in ${Engine.formatClock(state.opensAt - now)}"
                    is SettingsLock.State.Open -> "Open for ${Engine.formatClock(state.closesAt - now)}"
                },
                style = MaterialTheme.typography.titleSmall,
                color = c.text,
            )
            Text(
                when (state) {
                    SettingsLock.State.Off -> "Changes apply straight away."
                    else -> "Loosening anything waits ${SettingsLock.formatWait(s.lockSeconds)}."
                },
                style = MaterialTheme.typography.bodySmall,
                color = c.textDim,
            )
        }
        Spacer(Modifier.width(8.dp))
        when (state) {
            SettingsLock.State.Off -> onSetUp?.let { SmallButton("Set one up", accent = true, onClick = it) }
            is SettingsLock.State.Locked -> SmallButton("Unlock") { store.updateSettings { SettingsLock.startWait(it, System.currentTimeMillis()) } }
            is SettingsLock.State.Waiting -> SmallButton("Cancel") { store.updateSettings { SettingsLock.relock(it) } }
            is SettingsLock.State.Open -> SmallButton("Lock now") { store.updateSettings { SettingsLock.relock(it) } }
        }
    }
}

/** Wall clock that ticks every [periodMs] while on screen. */
@Composable
fun rememberNow(periodMs: Long = 1000): Long {
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(periodMs)
            value = System.currentTimeMillis()
        }
    }
    return now
}

fun openAccessibilitySettings(context: Context) {
    val intent = Intent(AndroidSettings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

fun openAppInfo(context: Context) {
    val intent = Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

const val PRIVACY_POLICY_URL = "https://github.com/vaanai/fixyodopaminebro/blob/main/docs/privacy-policy.md"

fun isBatteryExempt(context: Context): Boolean =
    context.getSystemService(android.os.PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) == true

fun formatTime(minuteOfDay: Int): String {
    val h = minuteOfDay / 60
    val m = minuteOfDay % 60
    return "%02d:%02d".format(h, m)
}

fun clockOf(epochMs: Long): String =
    java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(epochMs))
