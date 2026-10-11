package app.quiet.service

import android.view.accessibility.AccessibilityNodeInfo
import quiet.core.model.NodeInfo

/** Holds the accessibility root from the walk that confirmed enforcement until click completes. */
class ConfirmingWalkHolder {
    var heldRoot: AccessibilityNodeInfo? = null
    var heldPackage: String? = null
    var freshRootLookups: Int = 0

    fun release(recycle: (AccessibilityNodeInfo?) -> Unit) {
        recycle(heldRoot)
        heldRoot = null
        heldPackage = null
    }
}
