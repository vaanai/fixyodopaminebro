package app.quiet.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.quiet.QuietApplication
import app.quiet.data.repo.DataWiper
import app.quiet.service.QuietAccessibilityService
import app.quiet.snapshot.SnapshotCapture
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import kotlin.math.min

data class SettingsUiState(
    val quietHoursEnabled: Boolean = false,
    val quietHoursStartLabel: String = "22:00",
    val quietHoursEndLabel: String = "07:00",
    val breaksPerDay: Int = 3,
    val gateMinutes: Int = 10,
    val holdSeconds: Int = 3,
    val appearance: String = "system",
    val accessibilityOn: Boolean = false,
    val batteryStatus: String = "Optimized",
    val unknownCount: Int = 0,
    val lastActive: String = "Not running",
    val unverifiedWarning: String = "Unverified rules for Instagram.\nUnverified rules for TikTok.",
    val versionName: String = "1.0.0",
    val captureCountdown: Int? = null,
    val captureMessage: String? = null,
)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val quietApp = app as QuietApplication
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            val snap = quietApp.preferences.readSnapshot()
            val last = snap.heartbeatEpochMs?.let {
                DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(it))
            } ?: "Not running"
            val version = try {
                getApplication<Application>().packageManager
                    .getPackageInfo(getApplication<Application>().packageName, 0).versionName ?: "1.0.0"
            } catch (_: Exception) {
                "1.0.0"
            }
            _state.value = _state.value.copy(
                quietHoursEnabled = snap.quietHoursEnabled,
                quietHoursStartLabel = formatMinutes(snap.quietHoursStartMinutes),
                quietHoursEndLabel = formatMinutes(snap.quietHoursEndMinutes),
                breaksPerDay = snap.breaksPerDay,
                gateMinutes = snap.gateMinutes,
                holdSeconds = snap.holdSeconds,
                appearance = snap.appearance,
                accessibilityOn = isAccessibilityEnabled(),
                batteryStatus = batteryLabel(),
                unknownCount = snap.unknownSurfaceCount,
                lastActive = last,
                versionName = version,
            )
        }
    }

    fun setQuietHoursEnabled(enabled: Boolean) {
        viewModelScope.launch {
            quietApp.policyRepository.patchSettings { it.copy(quietHoursEnabled = enabled) }
            reload()
        }
    }

    fun setQuietHoursStart(minutes: Int) {
        viewModelScope.launch {
            quietApp.policyRepository.patchSettings { it.copy(quietHoursStartMinutes = minutes) }
            reload()
        }
    }

    fun setQuietHoursEnd(minutes: Int) {
        viewModelScope.launch {
            quietApp.policyRepository.patchSettings { it.copy(quietHoursEndMinutes = minutes) }
            reload()
        }
    }

    fun setBreaksPerDay(count: Int) {
        viewModelScope.launch {
            quietApp.policyRepository.patchSettings { snap ->
                val clamped = count.coerceIn(0, 5)
                snap.copy(
                    breaksPerDay = clamped,
                    breaksRemainingToday = min(snap.breaksRemainingToday, clamped),
                )
            }
            reload()
        }
    }

    fun setGateMinutes(minutes: Int) {
        viewModelScope.launch {
            quietApp.policyRepository.patchSettings { it.copy(gateMinutes = minutes) }
            reload()
        }
    }

    fun setHoldSeconds(seconds: Int) {
        viewModelScope.launch {
            val clamped = seconds.coerceIn(2, 5)
            quietApp.policyRepository.patchSettings { it.copy(holdSeconds = clamped) }
            reload()
        }
    }

    fun setAppearance(mode: String) {
        viewModelScope.launch {
            quietApp.policyRepository.patchSettings { it.copy(appearance = mode) }
            reload()
        }
    }

    fun onResume() {
        reload()
    }

    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(intent)
    }

    fun openBatterySettings() {
        val pkg = getApplication<Application>().packageName
        val pm = getApplication<Application>().getSystemService(PowerManager::class.java)
        val intent = if (pm?.isIgnoringBatteryOptimizations(pkg) == true) {
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        } else {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$pkg")
            }
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(intent)
    }

    suspend fun exportLogJson(): String = quietApp.eventRepository.exportJson()

    suspend fun exportLibraryJson(): String = quietApp.libraryRepository.exportJson()

    fun startCapture() {
        viewModelScope.launch {
            var n = 3
            while (n > 0) {
                _state.value = _state.value.copy(captureCountdown = n, captureMessage = "Switch to the app.")
                delay(1_000)
                n--
            }
            _state.value = _state.value.copy(captureCountdown = null)
            val service = QuietAccessibilityService.instance
            if (service == null) {
                _state.value = _state.value.copy(captureMessage = "Turn on the Quiet accessibility service first.")
                return@launch
            }
            when (val result = service.captureSnapshotForDiagnostics()) {
                is SnapshotCapture.Result.Success -> {
                    _state.value = _state.value.copy(captureMessage = "Saved ${result.file.name}")
                    val send = shareSnapshot(result.file)
                    send.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    getApplication<Application>().startActivity(Intent.createChooser(send, "Share snapshot"))
                }
                is SnapshotCapture.Result.WrongApp -> {
                    _state.value = _state.value.copy(captureMessage = result.message)
                }
                is SnapshotCapture.Result.Failed -> {
                    _state.value = _state.value.copy(captureMessage = result.message)
                }
            }
        }
    }

    fun shareSnapshot(file: java.io.File): Intent {
        val uri = FileProvider.getUriForFile(
            getApplication(),
            "${getApplication<Application>().packageName}.fileprovider",
            file,
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            DataWiper(quietApp.preferences, quietApp.eventRepository, quietApp.libraryRepository, quietApp.policyRepository)
                .deleteAllData()
            reload()
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            getApplication<Application>().contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return enabled.contains("app.quiet")
    }

    private fun batteryLabel(): String {
        val pm = getApplication<Application>().getSystemService(PowerManager::class.java)
        val pkg = getApplication<Application>().packageName
        return if (pm?.isIgnoringBatteryOptimizations(pkg) == true) "Unrestricted" else "Optimized"
    }

    companion object {
        fun formatMinutes(minutes: Int): String {
            val h = (minutes / 60) % 24
            val m = minutes % 60
            return "%02d:%02d".format(h, m)
        }

        fun parseTimeToMinutes(hour: Int, minute: Int): Int = hour * 60 + minute
    }
}
