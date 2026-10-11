package app.quiet.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.quiet.QuietApplication
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import quiet.core.rules.AppRules
import quiet.core.rules.Policy
import quiet.core.rules.RuleParser
import quiet.core.rules.SurfaceKind
import quiet.core.rules.SurfaceRule

data class SurfaceRowState(
    val rule: SurfaceRule,
    val policy: Policy,
    val sublabel: String?,
)

data class AppDetailUiState(
    val appName: String = "",
    val feedSurfaces: List<SurfaceRowState> = emptyList(),
    val friendsSurface: SurfaceRowState? = null,
)

class AppDetailViewModel(app: Application, private val appId: String) : AndroidViewModel(app) {
    private val quietApp = app as QuietApplication
    private val rules: AppRules = loadRules(appId)
    private val _state = MutableStateFlow(AppDetailUiState())
    val state: StateFlow<AppDetailUiState> = _state.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            val snap = quietApp.preferences.readSnapshot()
            val repo = quietApp.policyRepository
            val feed = rules.surfaces.filter { it.kind == SurfaceKind.feed }.map { surface ->
                val sub = if (!surface.enforce) "Not detected yet" else null
                SurfaceRowState(
                    rule = surface,
                    policy = repo.policyForSurface(snap, surface.fullId, surface.defaultPolicy),
                    sublabel = sub,
                )
            }
            val friends = rules.surfaces.firstOrNull { it.kind == SurfaceKind.shared_media }
            _state.value = AppDetailUiState(
                appName = rules.displayName,
                feedSurfaces = feed,
                friendsSurface = friends?.let {
                    val policy = repo.policyForSurface(snap, it.fullId, it.defaultPolicy)
                    val mapped = if (policy == Policy.OPEN_ONLY) Policy.ALLOW else policy
                    SurfaceRowState(it, mapped, "Open-only plays the reel you tap.")
                },
            )
        }
    }

    fun setFeedPolicy(surfaceId: String, policy: Policy) {
        viewModelScope.launch {
            quietApp.policyRepository.setSurfacePolicy(surfaceId, policy)
            reload()
        }
    }

    fun setFriendsPolicy(policy: Policy) {
        val friends = rules.surfaces.firstOrNull { it.kind == SurfaceKind.shared_media } ?: return
        viewModelScope.launch {
            val stored = if (policy == Policy.ALLOW) Policy.OPEN_ONLY else Policy.BLOCK
            quietApp.policyRepository.setFriendsReels(friends.fullId, stored)
            reload()
        }
    }

    private fun loadRules(appId: String): AppRules {
        val path = "rules/$appId.json"
        val json = getApplication<Application>().classLoader.getResource(path)!!.readText()
        return RuleParser.parseResource(json, path).first!!
    }

    companion object {
        fun factory(appId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AppDetailViewModel(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application, appId)
            }
        }
    }
}
