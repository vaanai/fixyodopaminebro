package app.quiet.ui.viewmodel

import android.app.Application
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.quiet.QuietApplication
import app.quiet.data.prefs.PresetIds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import quiet.core.QuietTiming

data class OnboardingUiState(
    val step: Int = 1,
    val complete: Boolean = false,
    val showRestrictedHelp: Boolean = Build.VERSION.SDK_INT >= 33,
    val showHealthBanner: Boolean = true,
    val preset: String = PresetIds.BALANCED,
)

class OnboardingViewModel(app: Application) : AndroidViewModel(app) {
    private val quietApp = app as QuietApplication
    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val snap = quietApp.preferences.readSnapshot()
            if (snap.onboardingComplete) {
                _state.value = _state.value.copy(complete = true)
            }
            refreshBanner()
        }
    }

    fun selectPreset(preset: String) {
        viewModelScope.launch {
            quietApp.policyRepository.applyPreset(preset)
            _state.value = _state.value.copy(preset = preset)
        }
    }

    fun next() {
        val step = _state.value.step
        if (step >= 4) {
            finish()
        } else {
            _state.value = _state.value.copy(step = step + 1)
            refreshBanner()
        }
    }

    fun finish() {
        viewModelScope.launch {
            val snap = quietApp.preferences.readSnapshot()
            quietApp.preferences.applySnapshot(snap.copy(onboardingComplete = true))
            _state.value = _state.value.copy(complete = true, step = 4)
        }
    }

    fun refreshBanner() {
        viewModelScope.launch {
            val snap = quietApp.preferences.readSnapshot()
            val serviceOn = isAccessibilityEnabled()
            val stale = snap.heartbeatEpochMs?.let {
                System.currentTimeMillis() - it > QuietTiming.HEARTBEAT_STALE_MS
            } ?: true
            _state.value = _state.value.copy(
                showHealthBanner = !serviceOn || stale || snap.settingsLoadFailed,
            )
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
