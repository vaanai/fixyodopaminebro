package app.quiet.service

import android.view.accessibility.AccessibilityNodeInfo
import quiet.core.model.NodeInfo

/** Clicks Messages using the accessibility root from the confirming walk only (no fresh tree). */
internal fun clickMessagesOnHeldRoot(
    holder: ConfirmingWalkHolder,
    label: NodeInfo,
    perform: (AccessibilityNodeInfo, NodeInfo) -> Boolean,
): Boolean {
    val root = holder.heldRoot ?: return false
    return perform(root, label)
}
