package app.quiet.ui.viewmodel

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.quiet.QuietApplication
import app.quiet.data.prefs.SettingsSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import quiet.core.QuietTiming
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class TodayUiState(
    val blockedToday: Int = 0,
    val presetLabel: String = "Balanced",
    val quietHoursOn: Boolean = false,
    val breaksLeft: String = "0 / 0",
    val showHealthBanner: Boolean = true,
    val recent: List<String> = emptyList(),
    val breakActive: Boolean = false,
)

class TodayViewModel(app: Application) : AndroidViewModel(app) {
    private val quietApp = app as QuietApplication
    private val _state = MutableStateFlow(TodayUiState())
    val state: StateFlow<TodayUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { refresh() }
    }

    suspend fun refresh() {
        val prefs = quietApp.preferences.readSnapshot()
        val serviceOn = isAccessibilityEnabled()
        val stale = prefs.heartbeatEpochMs?.let {
            System.currentTimeMillis() - it > QuietTiming.HEARTBEAT_STALE_MS
        } ?: true
        val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val blocked = quietApp.eventRepository.countBlockedToday(startOfDay)
        val recent = quietApp.eventRepository.recent(5).map {
            "${it.appId} · ${it.surfaceId} · ${it.action}"
        }
        _state.value = TodayUiState(
            blockedToday = blocked,
            presetLabel = prefs.presetId.replaceFirstChar { it.uppercase() },
            quietHoursOn = prefs.quietHoursEnabled,
            breaksLeft = "${prefs.breaksPerDay - prefs.breaksRemainingToday} / ${prefs.breaksPerDay}",
            showHealthBanner = !serviceOn || stale || prefs.settingsLoadFailed,
            recent = recent,
            breakActive = prefs.breakUntilEpochMs?.let { it > System.currentTimeMillis() } == true,
        )
    }

    private fun isAccessibilityEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            getApplication<Application>().contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return enabled.contains("app.quiet")
    }
}
