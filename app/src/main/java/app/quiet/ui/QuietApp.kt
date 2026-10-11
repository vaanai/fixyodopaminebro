package app.quiet.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.quiet.ui.navigation.QuietBottomBar
import app.quiet.ui.onboarding.OnboardingScreen
import app.quiet.ui.screens.AppDetailScreen
import app.quiet.ui.screens.AppsScreen
import app.quiet.ui.screens.LibraryScreen
import app.quiet.ui.screens.SettingsScreen
import app.quiet.ui.screens.TodayScreen
import app.quiet.ui.viewmodel.AppsViewModel
import app.quiet.ui.viewmodel.LibraryViewModel
import app.quiet.ui.viewmodel.OnboardingViewModel
import app.quiet.ui.viewmodel.SettingsViewModel
import app.quiet.ui.viewmodel.TodayViewModel

@Composable
fun QuietApp() {
    val onboardingVm: OnboardingViewModel = viewModel()
    val onboarding by onboardingVm.state.collectAsState()
    if (!onboarding.complete) {
        OnboardingScreen(
            vm = onboardingVm,
            onDone = { onboardingVm.finish() },
        )
        return
    }
    val nav = rememberNavController()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        bottomBar = {
            QuietBottomBar(
                selected = tab,
                onSelect = { index ->
                    tab = index
                    val route = when (index) {
                        0 -> "today"
                        1 -> "apps"
                        else -> "library"
                    }
                    nav.navigate(route) { launchSingleTop = true; popUpTo("today") }
                },
            )
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "today",
            modifier = Modifier.padding(padding),
        ) {
            composable("today") {
                val vm: TodayViewModel = viewModel()
                TodayScreen(vm = vm, onSettings = { nav.navigate("settings") })
            }
            composable("apps") {
                val vm: AppsViewModel = viewModel()
                AppsScreen(vm = vm, onOpenApp = { nav.navigate("app/$it") })
            }
            composable("app/{appId}") { entry ->
                val appId = entry.arguments?.getString("appId") ?: return@composable
                AppDetailScreen(appId = appId, onBack = { nav.popBackStack() })
            }
            composable("library") {
                val vm: LibraryViewModel = viewModel()
                LibraryScreen(vm = vm)
            }
            composable("settings") {
                val vm: SettingsViewModel = viewModel()
                SettingsScreen(vm = vm, onBack = { nav.popBackStack() })
            }
        }
    }
}
