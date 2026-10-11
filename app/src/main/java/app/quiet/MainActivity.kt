package app.quiet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.quiet.ui.QuietApp
import app.quiet.ui.theme.QuietTheme
import app.quiet.ui.theme.AppearanceMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuietTheme(appearance = AppearanceMode.Dark) {
                QuietApp()
            }
        }
    }
}
