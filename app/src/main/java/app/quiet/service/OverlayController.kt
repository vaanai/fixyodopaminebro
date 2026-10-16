package app.quiet.service

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import app.quiet.ui.sheets.GateSheetContent
import app.quiet.ui.sheets.InterventionSheetContent
import app.quiet.ui.theme.AppearanceMode
import app.quiet.ui.theme.QuietTheme

class OverlayController(private val service: AccessibilityService) {
    private var root: FrameLayout? = null
    private var params: WindowManager.LayoutParams? = null
    private val overlayLifecycleOwner = object : LifecycleOwner {
        private val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle = registry

        init {
            registry.currentState = Lifecycle.State.RESUMED
        }
    }

    fun dismiss() {
        val view = root ?: return
        try {
            service.getSystemService(WindowManager::class.java)?.removeView(view)
        } catch (_: Exception) {
        } finally {
            root = null
            params = null
        }
    }

    fun showIntervention(request: SheetRequest, onGoMessages: () -> Unit, onBreak: () -> Unit, onHome: () -> Unit) {
        show {
            InterventionSheetContent(
                surfaceLabel = request.label,
                messagesFound = request.messagesAvailable,
                breakEnabled = request.breakEnabled,
                onGoMessages = onGoMessages,
                onBreak = onBreak,
                onHome = onHome,
            )
        }
    }

    fun showGate(
        request: GateRequest,
        messagesFound: Boolean,
        onConfirm: () -> Unit,
        onMessages: () -> Unit,
        onNotNow: () -> Unit,
        onGoHome: () -> Unit,
    ) {
        show {
            GateSheetContent(
                surfaceLabel = request.label,
                minutes = request.minutes,
                holdMs = request.holdMs,
                messagesFound = messagesFound,
                onConfirm = onConfirm,
                onGoMessages = onMessages,
                onNotNow = onNotNow,
                onGoHome = onGoHome,
            )
        }
    }

    private fun show(content: @androidx.compose.runtime.Composable () -> Unit) {
        dismiss()
        val frame = FrameLayout(service)
        val composeView = ComposeView(service).apply {
            setViewTreeLifecycleOwner(overlayLifecycleOwner)
            setContent {
                QuietTheme(appearance = AppearanceMode.Dark) {
                    Surface(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                        content()
                    }
                }
            }
        }
        frame.addView(composeView)
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM
        }
        service.getSystemService(WindowManager::class.java)?.addView(frame, lp)
        root = frame
        params = lp
    }
}
