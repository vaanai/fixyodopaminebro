package app.quiet.ui.viewmodel

import android.app.Application
import android.content.Intent
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

data class SettingsUiState(
    val unknownCount: Int = 0,
    val lastActive: String = "Not running",
    val unverifiedWarning: String = "Unverified rules for Instagram.\nUnverified rules for TikTok.",
    val captureCountdown: Int? = null,
    val captureMessage: String? = null,
    val exportLogReady: String? = null,
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
            _state.value = _state.value.copy(
                unknownCount = snap.unknownSurfaceCount,
                lastActive = last,
            )
        }
    }

    suspend fun exportLogJson(): String = quietApp.eventRepository.exportJson()

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
}
