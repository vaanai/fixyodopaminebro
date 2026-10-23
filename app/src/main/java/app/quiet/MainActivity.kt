package app.quiet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.quiet.service.QuietService
import app.quiet.ui.screens.AppDetailScreen
import app.quiet.ui.screens.AppsScreen
import app.quiet.ui.screens.FocusScreen
import app.quiet.ui.screens.InsightsScreen
import app.quiet.ui.screens.InspectorScreen
import app.quiet.ui.screens.OnboardingScreen
import app.quiet.ui.screens.SettingsScreen
import app.quiet.ui.screens.StrictWaitDialog
import app.quiet.ui.screens.TodayScreen
import app.quiet.ui.screens.TrackedAppsScreen
import app.quiet.ui.screens.BottomBar
import app.quiet.ui.theme.QuietTheme
import app.quiet.ui.theme.Q
import quiet.core.settings.Settings
import quiet.core.settings.Strictness

enum class Tab { TODAY, APPS, INSIGHTS, SETTINGS }

sealed interface Route {
    data class Main(val tab: Tab) : Route
    data class AppDetail(val app: String) : Route
    data object Focus : Route
    data object Tracked : Route
    data object Inspector : Route
}

/** Every settings change in the UI goes through here so strict mode can make loosening wait. */
fun interface SettingsEditor {
    fun edit(transform: (Settings) -> Settings)
}

class Navigator(private val stack: MutableList<Route>) {
    val current: Route get() = stack.last()
    fun go(r: Route) {
        stack.add(r)
    }
    fun tab(t: Tab) {
        stack.clear()
        stack.add(Route.Main(t))
    }
    fun back(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        return true
    }
}

val LocalEditor = staticCompositionLocalOf<SettingsEditor> { SettingsEditor { QuietApp.store.updateSettings(it) } }
val LocalNav = staticCompositionLocalOf<Navigator> { error("no navigator") }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent { Root() }
    }

    override fun onResume() {
        super.onResume()
        QuietService.instance?.refresh()
    }
}

@Composable
private fun Root() {
    val store = QuietApp.store
    val settings by store.settings.collectAsStateWithLifecycle()
    var pending by remember { mutableStateOf<((Settings) -> Settings)?>(null) }
    val editor = remember {
        SettingsEditor { transform ->
            val current = store.settings.value
            val next = transform(current)
            if (current.strictMode && Strictness.isLoosening(current, next)) {
                pending = transform
            } else {
                store.updateSettings(transform)
            }
        }
    }
    val stack = remember { mutableStateListOf<Route>(Route.Main(Tab.TODAY)) }
    val nav = remember { Navigator(stack) }

    QuietTheme(settings.accent) {
        CompositionLocalProvider(LocalEditor provides editor, LocalNav provides nav) {
            Box(Modifier.fillMaxSize().background(Q.colors.bg)) {
                if (!settings.onboarded) {
                    OnboardingScreen()
                } else {
                    BackHandler(enabled = stack.size > 1) { nav.back() }
                    val route = stack.last()
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f)) {
                            AnimatedContent(
                                targetState = route,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "route",
                            ) { r ->
                                when (r) {
                                    is Route.Main -> when (r.tab) {
                                        Tab.TODAY -> TodayScreen()
                                        Tab.APPS -> AppsScreen()
                                        Tab.INSIGHTS -> InsightsScreen()
                                        Tab.SETTINGS -> SettingsScreen()
                                    }
                                    is Route.AppDetail -> AppDetailScreen(r.app)
                                    Route.Focus -> FocusScreen()
                                    Route.Tracked -> TrackedAppsScreen()
                                    Route.Inspector -> InspectorScreen()
                                }
                            }
                        }
                        if (route is Route.Main) BottomBar(route.tab) { nav.tab(it) }
                    }
                }
                pending?.let { transform ->
                    StrictWaitDialog(
                        seconds = settings.strictDelaySeconds,
                        onConfirm = {
                            store.updateSettings(transform)
                            pending = null
                        },
                        onCancel = { pending = null },
                    )
                }
            }
        }
    }
}
