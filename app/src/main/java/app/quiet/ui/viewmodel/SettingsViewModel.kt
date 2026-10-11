package app.quiet.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.quiet.QuietApplication
import app.quiet.data.repo.DataWiper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val unknownCount: Int = 0,
    val diagnostics: String = "Unverified rules for Instagram.\nUnverified rules for TikTok.",
    val exportLog: String = "",
)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val quietApp = app as QuietApplication
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val snap = quietApp.preferences.readSnapshot()
            _state.value = SettingsUiState(unknownCount = snap.unknownSurfaceCount)
        }
    }

    fun exportLog() {
        viewModelScope.launch {
            val json = quietApp.eventRepository.exportJson()
            _state.value = _state.value.copy(exportLog = json)
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            DataWiper(quietApp.preferences, quietApp.eventRepository, quietApp.libraryRepository, quietApp.policyRepository)
                .deleteAllData()
        }
    }
}
