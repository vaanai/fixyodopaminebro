package app.quiet.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.quiet.LocalNav
import app.quiet.QuietApp
import app.quiet.service.LiveState
import app.quiet.service.QuietService
import app.quiet.ui.components.PrimaryButton
import app.quiet.ui.components.QCard
import app.quiet.ui.components.ScreenHeader
import app.quiet.ui.components.SecondaryButton
import app.quiet.ui.components.SectionTitle
import app.quiet.ui.components.Tag
import app.quiet.ui.theme.Q
import kotlinx.coroutines.delay
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InspectorScreen() {
    val live by LiveState.state.collectAsStateWithLifecycle()
    val rules by QuietApp.store.rules.collectAsStateWithLifecycle()
    val nav = LocalNav.current
    val context = LocalContext.current
    val c = Q.colors
    var countdown by remember { mutableIntStateOf(0) }
    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown--
            if (countdown == 0) QuietService.instance?.requestDump()
        }
    }

    Page {
        ScreenHeader("Inspector", "What Quiet sees in the app in front. Open this, switch to Instagram or TikTok, and come back.", onBack = { nav.back() })

        SectionTitle("Service")
        QCard {
            Line("Running", if (live.connected) "yes" else "no")
            Line("Foreground", live.foreground ?: "–")
            Line("Tree", "${live.nodeCount} nodes in ${live.walkMs} ms${if (live.reading?.truncated == true) " (truncated)" else ""}")
            Line("Last scroll", if (live.lastScrollAt == 0L) "–" else "${(System.currentTimeMillis() - live.lastScrollAt) / 1000}s ago")
        }

        val r = live.reading
        SectionTitle("Screen")
        QCard {
            Line("App", r?.app ?: "–")
            Line("Surface", r?.surface ?: "unknown")
            Line("Kind", r?.kind?.name ?: "–")
            Line("Tab bar at", r?.tabBarTop?.toString() ?: "–")
            Line("Signature", r?.signature?.ifEmpty { "–" } ?: "–")
            if (r != null && r.signals.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (sig in r.signals.sorted()) Tag(sig)
                }
            }
        }

        val e = live.engine
        val d = live.decision
        SectionTitle("Decision")
        QCard {
            Line("Cover", d?.cover?.reason?.name ?: "none")
            Line("Pill", d?.pill?.text ?: "–")
            Line("Origin", e?.origin?.name ?: "–")
            Line("Swipes", (e?.swipes ?: 0).toString())
            Line("Passes", e?.passes?.entries?.joinToString { "${it.key} ${((it.value - System.currentTimeMillis()) / 1000).coerceAtLeast(0)}s" }?.ifEmpty { "–" } ?: "–")
        }

        SectionTitle("Screen dump")
        QCard {
            Text(
                "Saves the full layout of the app in front as JSON, so its detection rules can be checked and fixed. Text from the app is included; it stays on your phone unless you share it.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textDim,
            )
            Spacer(Modifier.height(12.dp))
            PrimaryButton(if (countdown > 0) "Switch apps now… $countdown" else "Dump in 5 seconds", enabled = countdown == 0) { countdown = 5 }
            live.lastDump?.let { path ->
                Spacer(Modifier.height(10.dp))
                Text(path, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = c.textFaint)
                Spacer(Modifier.height(8.dp))
                SecondaryButton("Share last dump") {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", File(path))
                    val send = Intent(Intent.ACTION_SEND).setType("application/json").putExtra(Intent.EXTRA_STREAM, uri)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(Intent.createChooser(send, "Share dump").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
        }

        SectionTitle("Rules")
        QCard {
            for ((app, src) in rules.sources) Line(app, src)
            for ((app, err) in rules.errors) Text("$app: $err", style = MaterialTheme.typography.bodySmall, color = c.danger)
            Spacer(Modifier.height(8.dp))
            Text(
                "Override a rule file by putting <app>.json in ${QuietApp.store.rulesOverrideDir()?.absolutePath}, then Reload.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textFaint,
            )
            Spacer(Modifier.height(8.dp))
            SecondaryButton("Reload rules") {
                QuietApp.store.reloadRules()
                QuietService.instance?.refresh()
            }
        }
    }
}

@Composable
private fun Line(label: String, value: String) {
    val c = Q.colors
    Row(Modifier.height(26.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = c.textFaint, modifier = Modifier.width(96.dp))
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = c.text, maxLines = 1)
    }
}
