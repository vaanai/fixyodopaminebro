package app.quiet.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.quiet.LocalEditor
import app.quiet.LocalNav
import app.quiet.QuietApp
import app.quiet.ui.components.AppIcon
import app.quiet.ui.components.Divider
import app.quiet.ui.components.QCard
import app.quiet.ui.components.ScreenHeader
import app.quiet.ui.components.SecondaryButton
import app.quiet.ui.components.SectionTitle
import app.quiet.ui.components.StepperRow
import app.quiet.ui.components.isInstalled
import app.quiet.ui.theme.Q
import quiet.core.settings.TrackedApp

@Composable
fun TrackedAppsScreen() {
    val s by QuietApp.store.settings.collectAsStateWithLifecycle()
    val rules by QuietApp.store.rules.collectAsStateWithLifecycle()
    val editor = LocalEditor.current
    val nav = LocalNav.current
    val context = LocalContext.current
    val c = Q.colors
    var picking by remember { mutableStateOf(false) }
    val pm = context.packageManager

    Page {
        ScreenHeader("Tracked apps", "Time in these apps is counted. Give any of them a daily limit.", onBack = { nav.back() })
        val installed = s.tracked.filter { isInstalled(pm, it.packageName) }
        val missing = s.tracked.size - installed.size
        Spacer(Modifier.height(8.dp))
        QCard {
            if (installed.isEmpty()) Text("None of your tracked apps are installed.", color = c.textDim)
            installed.forEachIndexed { i, t ->
                if (i > 0) Divider()
                Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(t.packageName, t.label, 32.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        val supported = rules.forPackage(t.packageName) != null
                        if (supported) {
                            Text(t.label, style = MaterialTheme.typography.titleSmall, color = c.text)
                            Text("Limits for this app live on its own page under Apps.", style = MaterialTheme.typography.bodySmall, color = c.textFaint)
                        } else {
                            StepperRow(
                                t.label,
                                t.dailyLimitMinutes,
                                options = listOf(0, 5, 10, 15, 20, 30, 45, 60, 90, 120, 180),
                                format = { if (it == 0) "No limit" else "${it}m" },
                            ) { v ->
                                editor.edit { st -> st.copy(tracked = st.tracked.map { if (it.packageName == t.packageName) it.copy(dailyLimitMinutes = v) else it }) }
                            }
                        }
                    }
                    IconButton(onClick = { editor.edit { st -> st.copy(tracked = st.tracked.filterNot { it.packageName == t.packageName }) } }) {
                        Icon(Icons.Rounded.Close, "Stop tracking", tint = c.textFaint)
                    }
                }
            }
        }
        if (missing > 0) {
            Spacer(Modifier.height(8.dp))
            Text("$missing more on the list aren't installed and are hidden.", style = MaterialTheme.typography.bodySmall, color = c.textFaint, modifier = Modifier.padding(start = 6.dp))
        }
        Spacer(Modifier.height(16.dp))
        SecondaryButton(if (picking) "Hide app list" else "Track another app") { picking = !picking }
        if (picking) {
            val launchable = remember {
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
                    .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
                    .distinctBy { it.first }
                    .filter { it.first != context.packageName }
                    .sortedBy { it.second.lowercase() }
            }
            SectionTitle("Installed apps")
            QCard {
                launchable.filter { (pkg, _) -> s.tracked.none { it.packageName == pkg } }.forEach { (pkg, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { editor.edit { st -> st.copy(tracked = st.tracked + TrackedApp(pkg, label)) } }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIcon(pkg, label, 28.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(label, style = MaterialTheme.typography.bodyMedium, color = c.text)
                    }
                }
            }
        }
    }
}
