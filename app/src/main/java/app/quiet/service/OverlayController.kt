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
import app.quiet.ui.sheets.GateSheetContent
import app.quiet.ui.sheets.InterventionSheetContent
import app.quiet.ui.theme.QuietTheme

class OverlayController(private val service: AccessibilityService) {
    private var root: FrameLayout? = null
    private var params: WindowManager.LayoutParams? = null

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

    fun showGate(request: GateRequest, onConfirm: () -> Unit, onMessages: () -> Unit, onNotNow: () -> Unit) {
        show {
            GateSheetContent(
                surfaceLabel = request.label,
                minutes = request.minutes,
                onConfirm = onConfirm,
                onGoMessages = onMessages,
                onNotNow = onNotNow,
            )
        }
    }

    private fun show(content: @androidx.compose.runtime.Composable () -> Unit) {
        dismiss()
        val frame = FrameLayout(service)
        val composeView = ComposeView(service).apply {
            setContent {
                QuietTheme {
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
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM
        }
        service.getSystemService(WindowManager::class.java)?.addView(frame, lp)
        root = frame
        params = lp
    }
}
