package app.quiet.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.quiet.QuietApplication
import app.quiet.data.prefs.PresetIds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
data class OnboardingUiState(
    val step: Int = 1,
    val complete: Boolean = false,
    val showRestrictedHelp: Boolean = Build.VERSION.SDK_INT >= 33,
    val accessibilityOn: Boolean = false,
    val instagramInstalled: Boolean = false,
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
            refreshDeviceState()
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
            refreshDeviceState()
        }
    }

    fun finish() {
        viewModelScope.launch {
            val snap = quietApp.preferences.readSnapshot()
            quietApp.preferences.applySnapshot(snap.copy(onboardingComplete = true))
            _state.value = _state.value.copy(complete = true, step = 4)
        }
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

    fun openInstagram() {
        val pm = getApplication<Application>().packageManager
        val launch = pm.getLaunchIntentForPackage("com.instagram.android") ?: return
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(launch)
    }

    fun refreshDeviceState() {
        viewModelScope.launch {
            val snap = quietApp.preferences.readSnapshot()
            _state.value = _state.value.copy(
                accessibilityOn = isAccessibilityEnabled(),
                instagramInstalled = isPackageInstalled("com.instagram.android"),
                preset = snap.presetId,
            )
        }
    }

    private fun isPackageInstalled(packageName: String): Boolean = try {
        getApplication<Application>().packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: Exception) {
        false
    }

    private fun isAccessibilityEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            getApplication<Application>().contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return enabled.contains("app.quiet")
    }
}
