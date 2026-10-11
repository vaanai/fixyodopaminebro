package app.quiet.service

import android.view.accessibility.AccessibilityNodeInfo
import quiet.core.model.NodeInfo

object MessageClickResolver {
    fun canClickAncestor(labelArea: Int, ancestorArea: Int): Boolean =
        ancestorArea > 0 && labelArea > 0 && ancestorArea <= labelArea * 4

    fun findAndroidClickTarget(label: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val labelArea = area(label)
        var current: AccessibilityNodeInfo? = label
        var depth = 0
        while (current != null && depth < 32) {
            if (current.isClickable) {
                val ok = canClickAncestor(labelArea, area(current))
                return if (ok) AccessibilityNodeInfo.obtain(current) else null
            }
            val parent = current.parent
            if (current !== label) {
                current.recycle()
            }
            current = parent
            depth++
        }
        return null
    }

    fun findLabelNode(root: AccessibilityNodeInfo, labels: List<String>): AccessibilityNodeInfo? {
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.add(AccessibilityNodeInfo.obtain(root))
        while (stack.isNotEmpty()) {
            val node = stack.removeFirst()
            val texts = listOfNotNull(node.text?.toString(), node.contentDescription?.toString())
            if (texts.any { t -> labels.any { l -> l.equals(t, true) } }) {
                stack.forEach { if (it !== node) it.recycle() }
                return AccessibilityNodeInfo.obtain(node)
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { stack.add(it) }
            }
            if (node !== root) node.recycle()
        }
        return null
    }

    fun performClickFromConfirmingLabel(root: AccessibilityNodeInfo, labelFromWalk: NodeInfo): Boolean {
        val label = findLabelNodeMatching(root, labelFromWalk) ?: return false
        val target = findAndroidClickTarget(label) ?: run {
            label.recycle()
            return false
        }
        val clicked = target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        target.recycle()
        label.recycle()
        return clicked
    }

    fun findLabelNodeMatching(root: AccessibilityNodeInfo, fromWalk: NodeInfo): AccessibilityNodeInfo? {
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.add(AccessibilityNodeInfo.obtain(root))
        while (stack.isNotEmpty()) {
            val node = stack.removeFirst()
            val matchesBounds = node.boundsMatch(fromWalk)
            val texts = listOfNotNull(node.text?.toString(), node.contentDescription?.toString())
            val textMatch = fromWalk.text?.let { t -> texts.any { it.equals(t, true) } } == true ||
                (fromWalk.text == null && fromWalk.contentDescription?.let { d ->
                    texts.any { it.equals(d, true) }
                } == true)
            if (matchesBounds && textMatch) {
                stack.forEach { if (it !== node) it.recycle() }
                return AccessibilityNodeInfo.obtain(node)
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { stack.add(it) }
            }
            if (node !== root) node.recycle()
        }
        return null
    }

    private fun AccessibilityNodeInfo.boundsMatch(fromWalk: NodeInfo): Boolean {
        val r = android.graphics.Rect()
        getBoundsInScreen(r)
        return r.left == fromWalk.boundsLeft &&
            r.top == fromWalk.boundsTop &&
            r.right == fromWalk.boundsRight &&
            r.bottom == fromWalk.boundsBottom
    }

    fun performClick(root: AccessibilityNodeInfo, labels: List<String>): Boolean {
        val label = findLabelNode(root, labels) ?: return false
        val target = findAndroidClickTarget(label) ?: run {
            label.recycle()
            return false
        }
        val clicked = target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        target.recycle()
        label.recycle()
        return clicked
    }

    /** JVM/test tree: smallest clickable ancestor within 4× label area. */
    fun planClickForTest(label: NodeInfo, nodes: List<NodeInfo>): Boolean {
        val candidates = nodes.filter { it.isClickable && contains(it, label) }
        val ancestor = candidates.minByOrNull { it.boundsArea } ?: return false
        return canClickAncestor(label.boundsArea, ancestor.boundsArea)
    }

    private fun contains(outer: NodeInfo, inner: NodeInfo): Boolean =
        inner.boundsLeft >= outer.boundsLeft &&
            inner.boundsTop >= outer.boundsTop &&
            inner.boundsRight <= outer.boundsRight &&
            inner.boundsBottom <= outer.boundsBottom

    private fun area(node: AccessibilityNodeInfo): Int {
        val r = android.graphics.Rect()
        node.getBoundsInScreen(r)
        return r.width().coerceAtLeast(0) * r.height().coerceAtLeast(0)
    }
}
