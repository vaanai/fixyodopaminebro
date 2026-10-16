package app.quiet.service

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import quiet.core.walk.RawNode

class AccessibilityNodeAdapter(
    private val node: AccessibilityNodeInfo,
    private val recycleOnRelease: Boolean = false,
) : RawNode {
    private val rect = Rect()

    override val text: CharSequence? = node.text
    override val contentDescription: CharSequence? = node.contentDescription
    override val viewIdResourceName: String? = node.viewIdResourceName
    override val isSelected: Boolean = node.isSelected
    override val isClickable: Boolean = node.isClickable
    override val className: CharSequence? = node.className

    init {
        node.getBoundsInScreen(rect)
    }

    override val boundsLeft: Int = rect.left
    override val boundsTop: Int = rect.top
    override val boundsRight: Int = rect.right
    override val boundsBottom: Int = rect.bottom
    override val childCount: Int = node.childCount

    override fun childAt(index: Int): RawNode? {
        val child = node.getChild(index) ?: return null
        return AccessibilityNodeAdapter(child, recycleOnRelease = true)
    }

    override fun release() {
        if (!recycleOnRelease) return
        try {
            node.recycle()
        } catch (_: Exception) {
        }
    }
}

fun AccessibilityNodeInfo.toAdapter(): RawNode = AccessibilityNodeAdapter(this, recycleOnRelease = false)
