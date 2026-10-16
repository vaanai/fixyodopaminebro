package app.quiet.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.View.MeasureSpec
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.core.app.ApplicationProvider
import app.quiet.QuietApplication
import app.quiet.data.prefs.QuietPreferences
import app.quiet.ui.library.LibraryEditSheetContent
import app.quiet.ui.theme.QuietThemeAccessor
import app.quiet.ui.onboarding.OnboardingScreen
import app.quiet.ui.screens.AppDetailScreen
import app.quiet.ui.screens.SettingsScreen
import app.quiet.ui.screens.TodayScreen
import app.quiet.ui.sheets.GateSheetContent
import app.quiet.ui.sheets.InterventionSheetContent
import app.quiet.ui.theme.AppearanceMode
import app.quiet.ui.theme.QuietTheme
import app.quiet.ui.viewmodel.OnboardingViewModel
import app.quiet.ui.viewmodel.SettingsViewModel
import app.quiet.ui.viewmodel.TodayViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    application = QuietApplication::class,
    qualifiers = "w1080dp-h2400dp-normal-notnight-xxhdpi",
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class QuietUiScreenshotCaptureTest {
    private val widthPx = 1080
    private val heightPx = 2400

    private val outDir = File(
        System.getenv("QUIET_UI_SCREENSHOT_DIR")
            ?: "/cursor/stores/self/media/quiet-ui",
    )

    @Test
    fun captureAllScreens() {
        outDir.mkdirs()
        seedPrefsForScreens()
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        capture(activity, "today-home.png") {
            QuietTheme(appearance = AppearanceMode.Dark) {
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    val vm: TodayViewModel = viewModel()
                    TodayScreen(vm = vm, onSettings = {})
                }
            }
        }
        capture(activity, "onboarding-preset.png") {
            QuietTheme(appearance = AppearanceMode.Dark) {
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    val vm: OnboardingViewModel = viewModel()
                    OnboardingScreen(vm = vm, onDone = {})
                }
            }
        }
        capture(activity, "gate-sheet.png") {
            QuietTheme(appearance = AppearanceMode.Dark) {
                Box(Modifier.fillMaxSize().background(Color(0xB8000000))) {
                    GateSheetContent(
                        surfaceLabel = "Reels",
                        minutes = 10,
                        holdMs = 3_000L,
                        messagesFound = true,
                        onConfirm = {},
                        onGoMessages = {},
                        onNotNow = {},
                        onGoHome = {},
                    )
                }
            }
        }
        capture(activity, "intervention-sheet.png") {
            QuietTheme(appearance = AppearanceMode.Dark) {
                Box(Modifier.fillMaxSize().background(Color(0xB8000000))) {
                    InterventionSheetContent(
                        surfaceLabel = "For You",
                        messagesFound = true,
                        breakEnabled = true,
                        onGoMessages = {},
                        onBreak = {},
                        onHome = {},
                    )
                }
            }
        }
        capture(activity, "settings-gate.png") {
            QuietTheme(appearance = AppearanceMode.Dark) {
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    val vm: SettingsViewModel = viewModel()
                    SettingsScreen(vm = vm, onBack = {})
                }
            }
        }
        capture(activity, "app-detail.png") {
            QuietTheme(appearance = AppearanceMode.Dark) {
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    AppDetailScreen(appId = "instagram", onBack = {})
                }
            }
        }
        capture(activity, "library-edit.png") {
            QuietTheme(appearance = AppearanceMode.Dark) {
                LibraryEditSheetCaptureFrame()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun LibraryEditSheetCaptureFrame() {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xB8000000)),
        ) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(
                    topStart = QuietThemeAccessor.radius.radiusSheet,
                    topEnd = QuietThemeAccessor.radius.radiusSheet,
                ),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column {
                    BottomSheetDefaults.DragHandle()
                    LibraryEditSheetContent(
                        initial = null,
                        onSave = {},
                        onDelete = null,
                    )
                }
            }
        }
    }

    private fun seedPrefsForScreens() {
        val app = ApplicationProvider.getApplicationContext<QuietApplication>()
        runBlocking {
            app.preferences.clearAll()
            app.policyRepository.seedBalancedIfNeeded()
            app.preferences.edit { it[QuietPreferences.Keys.ONBOARDING] = false }
        }
    }

    private fun capture(activity: ComponentActivity, filename: String, content: @Composable () -> Unit) {
        val composeView = ComposeView(activity)
        composeView.setViewTreeLifecycleOwner(activity)
        activity.setContentView(
            composeView,
            android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        activity.runOnUiThread {
            composeView.setContent(content)
        }
        ShadowLooper.idleMainLooper()
        composeView.measure(
            MeasureSpec.makeMeasureSpec(widthPx, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(heightPx, MeasureSpec.EXACTLY),
        )
        composeView.layout(0, 0, widthPx, heightPx)
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        composeView.draw(canvas)
        FileOutputStream(File(outDir, filename)).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()
    }
}
