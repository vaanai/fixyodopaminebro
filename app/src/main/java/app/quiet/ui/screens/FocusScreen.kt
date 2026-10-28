package app.quiet.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.quiet.LocalEditor
import app.quiet.LocalNav
import app.quiet.QuietApp
import app.quiet.ui.components.Chip
import app.quiet.ui.components.Note
import app.quiet.ui.components.PrimaryButton
import app.quiet.ui.components.QCard
import app.quiet.ui.components.ScreenHeader
import app.quiet.ui.components.SectionTitle
import app.quiet.ui.components.StepperRow
import app.quiet.ui.components.SwitchRow
import app.quiet.ui.theme.Q
import quiet.core.settings.FocusWindow
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID

private val quarterHours = (0 until 24 * 4).map { it * 15 }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusScreen() {
    val s by QuietApp.store.settings.collectAsStateWithLifecycle()
    val editor = LocalEditor.current
    val nav = LocalNav.current
    val c = Q.colors

    fun update(id: String, f: (FocusWindow) -> FocusWindow) =
        editor.edit { st -> st.copy(focusWindows = st.focusWindows.map { if (it.id == id) f(it) else it }) }

    Page {
        ScreenHeader("Focus schedule", "During these times every video feed is blocked, even ones you allow, and so is anything set to Pause or Limit. Chats, profiles and anything else you allow keep working.", onBack = { nav.back() })
        for (w in s.focusWindows) {
            Spacer(Modifier.height(12.dp))
            QCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(w.label, style = MaterialTheme.typography.titleMedium, color = c.text, modifier = Modifier.weight(1f))
                    IconButton(onClick = { editor.edit { st -> st.copy(focusWindows = st.focusWindows.filterNot { it.id == w.id }) } }) {
                        Icon(Icons.Rounded.Delete, "Delete", tint = c.textDim)
                    }
                }
                SwitchRow("On", w.enabled) { on -> update(w.id) { it.copy(enabled = on) } }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (d in DayOfWeek.entries) {
                        Chip(d.getDisplayName(TextStyle.SHORT, Locale.getDefault()), d in w.days) {
                            update(w.id) { it.copy(days = if (d in it.days) it.days - d else it.days + d) }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                StepperRow("Starts", w.startMinute, options = quarterHours, format = ::formatTime) { v -> update(w.id) { it.copy(startMinute = v) } }
                StepperRow("Ends", w.endMinute, options = quarterHours, format = ::formatTime) { v -> update(w.id) { it.copy(endMinute = v) } }
                if (w.endMinute < w.startMinute) {
                    Text("Runs past midnight into the next day.", style = MaterialTheme.typography.bodySmall, color = c.textFaint)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        val weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
        PrimaryButton("Add a focus window") {
            editor.edit { st ->
                val label = when (st.focusWindows.size) {
                    0 -> "Wind down"
                    1 -> "Mornings"
                    else -> "Focus ${st.focusWindows.size + 1}"
                }
                val (start, end) = if (st.focusWindows.isEmpty()) 23 * 60 to 7 * 60 else 8 * 60 to 12 * 60
                st.copy(focusWindows = st.focusWindows + FocusWindow(UUID.randomUUID().toString(), label, DayOfWeek.entries.toSet().let { if (st.focusWindows.isEmpty()) it else weekdays }, start, end))
            }
        }

        SectionTitle("Unlocks")
        QCard {
            if (s.unlocksEnabled) {
                SwitchRow("Unlocks during focus", s.focusAllowsUnlocks, if (s.focusAllowsUnlocks) "Focus time can be unlocked like any other block." else "Focus time can't be unlocked.") { on ->
                    editor.edit { it.copy(focusAllowsUnlocks = on) }
                }
            } else {
                Note("Unlocks are off, so focus time can't be unlocked either.")
            }
        }
    }
}
