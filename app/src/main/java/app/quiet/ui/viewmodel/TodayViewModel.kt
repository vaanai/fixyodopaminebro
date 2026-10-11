package app.quiet.ui.viewmodel

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.quiet.QuietApplication
import app.quiet.data.policy.EnforcementStateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import quiet.core.QuietTiming
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

data class TodayUiState(
    val blockedToday: Int = 0,
    val presetLabel: String = "Balanced",
    val quietHoursOn: Boolean = false,
    val gatedMinutes: String = "0 min",
    val breaksLeft: String = "0 / 0",
    val breaksRemaining: Int = 0,
    val showHealthBanner: Boolean = true,
    val recent: List<String> = emptyList(),
    val breakActive: Boolean = false,
    val breakUntilLabel: String? = null,
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
        val gatedTotal = quietApp.eventRepository.gatedMinutesToday(startOfDay, prefs.gateMinutes)
        val recent = quietApp.eventRepository.recent(5).map {
            "${it.appId} · ${it.surfaceId} · ${it.action}"
        }
        val now = System.currentTimeMillis()
        val breakActive = prefs.breakUntilEpochMs?.let { it > now } == true
        val breakUntilLabel = prefs.breakUntilEpochMs?.takeIf { it > now }?.let { until ->
            DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                .format(Instant.ofEpochMilli(until).atZone(ZoneId.systemDefault()))
        }
        _state.value = TodayUiState(
            blockedToday = blocked,
            presetLabel = prefs.presetId.replaceFirstChar { it.uppercase() },
            quietHoursOn = prefs.quietHoursEnabled,
            gatedMinutes = "$gatedTotal min",
            breaksLeft = "${prefs.breaksRemainingToday} / ${prefs.breaksPerDay}",
            breaksRemaining = prefs.breaksRemainingToday,
            showHealthBanner = !serviceOn || stale || prefs.settingsLoadFailed,
            recent = recent,
            breakActive = breakActive,
            breakUntilLabel = breakUntilLabel,
        )
    }

    fun startBreak() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val memory = EnforcementStateRepository.memoryFromSnapshot(quietApp.preferences.readSnapshot())
            quietApp.enforcementStateRepository.startBreak(memory, now)
            refresh()
        }
    }

    fun endBreak() {
        viewModelScope.launch {
            val memory = EnforcementStateRepository.memoryFromSnapshot(quietApp.preferences.readSnapshot())
            quietApp.enforcementStateRepository.endBreak(memory)
            refresh()
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            getApplication<Application>().contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return enabled.contains("app.quiet")
    }
}
