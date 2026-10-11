package app.quiet.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.quiet.QuietApplication
import app.quiet.data.prefs.PresetIds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppsUiState(val preset: String = PresetIds.BALANCED)

class AppsViewModel(app: Application) : AndroidViewModel(app) {
    private val quietApp = app as QuietApplication
    private val _state = MutableStateFlow(AppsUiState())
    val state: StateFlow<AppsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val snap = quietApp.preferences.readSnapshot()
            _state.value = AppsUiState(preset = snap.presetId)
        }
    }

    fun applyPreset(preset: String) {
        viewModelScope.launch {
            quietApp.policyRepository.applyPreset(preset)
            _state.value = AppsUiState(preset = preset)
        }
    }
}
