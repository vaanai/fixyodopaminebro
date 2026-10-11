package app.quiet.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.quiet.QuietApplication
import app.quiet.data.policy.AppSummaryBuilder
import app.quiet.data.prefs.PresetIds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import quiet.core.rules.AppRules
import quiet.core.rules.RuleParser

data class AppCardState(val appId: String, val name: String, val summary: String)

data class AppsUiState(
    val preset: String = PresetIds.BALANCED,
    val apps: List<AppCardState> = emptyList(),
)

class AppsViewModel(app: Application) : AndroidViewModel(app) {
    private val quietApp = app as QuietApplication
    private val _state = MutableStateFlow(AppsUiState())
    val state: StateFlow<AppsUiState> = _state.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            val snap = quietApp.preferences.readSnapshot()
            val apps = listOf("instagram", "tiktok").mapNotNull { id ->
                loadRules(id)?.let { rules ->
                    AppCardState(
                        appId = id,
                        name = rules.displayName,
                        summary = AppSummaryBuilder.build(rules, snap),
                    )
                }
            }
            _state.value = AppsUiState(preset = snap.presetId, apps = apps)
        }
    }

    fun applyPreset(preset: String) {
        viewModelScope.launch {
            quietApp.policyRepository.applyPreset(preset)
            reload()
        }
    }

    private fun loadRules(appId: String): AppRules? {
        val path = "rules/$appId.json"
        val json = getApplication<Application>().classLoader.getResource(path)?.readText() ?: return null
        return RuleParser.parseResource(json, path).first
    }
}
