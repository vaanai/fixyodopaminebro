package app.quiet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.quiet.ui.QuietApp
import app.quiet.ui.theme.AppearanceMode
import app.quiet.ui.theme.QuietTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuietThemedApp()
        }
    }
}

@Composable
private fun QuietThemedApp() {
    val app = LocalContext.current.applicationContext as QuietApplication
    var appearance by remember { mutableStateOf(AppearanceMode.System) }
    LaunchedEffect(app) {
        app.preferences.snapshotFlow.collect { snap ->
            appearance = when (snap.appearance) {
                "dark" -> AppearanceMode.Dark
                "light" -> AppearanceMode.Light
                else -> AppearanceMode.System
            }
        }
    }
    QuietTheme(appearance = appearance) {
        QuietApp()
    }
}
