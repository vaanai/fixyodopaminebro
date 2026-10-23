package app.quiet.service

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import app.quiet.ui.theme.QuietTheme
import quiet.core.engine.Cover
import quiet.core.settings.Accent

/** Compose needs a lifecycle and saved-state owner; a service has neither, so the overlays get their own. */
private class OverlayOwner : SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val controller = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry

    init {
        controller.performAttach()
        controller.performRestore(null)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }
}

interface CoverActions {
    fun onNav(cover: Cover, target: String)
    fun onBack(cover: Cover)
    fun onLeave(cover: Cover)
    /** [videos] true for "watch N more", false for "N minutes". */
    fun onUnlock(cover: Cover, videos: Boolean)
    fun onContinue(cover: Cover)
}

data class CoverModel(val cover: Cover, val accent: Accent, val todayLine: String?)

data class PillModel(val text: String, val urgent: Boolean, val accent: Accent)

/**
 * The windows Quiet draws over other apps. Accessibility overlays need no extra permission and sit
 * above the app being covered.
 */
class Overlays(private val service: AccessibilityService, private val actions: CoverActions) {
    private val wm = service.getSystemService(WindowManager::class.java)
    private val owner = OverlayOwner()
    private val main = Handler(Looper.getMainLooper())

    private var coverView: ComposeView? = null
    private var coverHeight: Int? = null
    private val coverState = mutableStateOf<CoverModel?>(null)

    private var pillView: ComposeView? = null
    private val pillState = mutableStateOf<PillModel?>(null)

    private var toastView: ComposeView? = null
    private val toastState = mutableStateOf<PillModel?>(null)
    private val hideToast = Runnable { removeToast() }

    val coverShowing: Boolean get() = coverView != null
    val currentCoverKey: String? get() = coverState.value?.cover?.key

    fun showCover(model: CoverModel) {
        val height = model.cover.bottomPx
        coverState.value = model
        val existing = coverView
        if (existing != null) {
            if (height != coverHeight) {
                coverHeight = height
                runCatching { wm.updateViewLayout(existing, coverParams(height)) }
            }
            return
        }
        val view = compose {
            val m = coverState.value
            if (m != null) {
                QuietTheme(m.accent) {
                    CoverContent(m, actions)
                }
            }
        }
        if (add(view, coverParams(height))) {
            coverView = view
            coverHeight = height
        }
    }

    fun hideCover() {
        coverView?.let { remove(it) }
        coverView = null
        coverHeight = null
        coverState.value = null
    }

    fun showPill(model: PillModel?) {
        if (model == null) {
            pillView?.let { remove(it) }
            pillView = null
            pillState.value = null
            return
        }
        pillState.value = model
        if (pillView != null) return
        val view = compose {
            val m = pillState.value
            if (m != null) QuietTheme(m.accent) { PillContent(m.text, m.urgent) }
        }
        val lp = baseParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT).apply {
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = (service.resources.displayMetrics.density * 44).toInt()
        }
        if (add(view, lp)) pillView = view
    }

    fun toast(model: PillModel, durationMs: Long = 4_000) {
        toastState.value = model
        main.removeCallbacks(hideToast)
        if (toastView == null) {
            val view = compose {
                val m = toastState.value
                if (m != null) QuietTheme(m.accent) { ToastContent(m.text) }
            }
            val lp = baseParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT).apply {
                flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                gravity = Gravity.BOTTOM
                y = (service.resources.displayMetrics.density * 120).toInt()
            }
            if (add(view, lp)) toastView = view
        }
        main.postDelayed(hideToast, durationMs)
    }

    private fun removeToast() {
        toastView?.let { remove(it) }
        toastView = null
        toastState.value = null
    }

    fun hideAll() {
        hideCover()
        showPill(null)
        main.removeCallbacks(hideToast)
        removeToast()
    }

    private fun compose(content: @androidx.compose.runtime.Composable () -> Unit): ComposeView =
        ComposeView(service).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent(content)
        }

    private fun coverParams(height: Int?): WindowManager.LayoutParams =
        baseParams(WindowManager.LayoutParams.MATCH_PARENT, height ?: WindowManager.LayoutParams.MATCH_PARENT).apply {
            gravity = Gravity.TOP or Gravity.START
            flags = flags or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        }

    private fun baseParams(w: Int, h: Int) = WindowManager.LayoutParams(
        w,
        h,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT,
    ).apply {
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    private fun add(view: View, lp: WindowManager.LayoutParams): Boolean = try {
        wm.addView(view, lp)
        true
    } catch (e: Exception) {
        Log.w("Quiet", "overlay add failed", e)
        false
    }

    private fun remove(view: View) {
        try {
            wm.removeViewImmediate(view)
        } catch (_: Exception) {
        }
    }
}
