package app.quiet.service

import android.graphics.Rect
import android.os.Build
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import quiet.core.model.UiNode
import quiet.core.model.UiTree

/**
 * Copies a window's accessibility tree into a [UiTree], with caps so a huge or slow tree
 * can't stall the service. The platform nodes are kept alongside so a match can be clicked.
 */
class TreeReader(private val maxNodes: Int = 1500, private val maxMillis: Long = 300) {
    class Result(val tree: UiTree, val platform: List<AccessibilityNodeInfo>, val millis: Long)

    fun read(root: AccessibilityNodeInfo, packageName: String, screenWidth: Int, screenHeight: Int): Result {
        val start = SystemClock.uptimeMillis()
        val nodes = ArrayList<UiNode>(256)
        val platform = ArrayList<AccessibilityNodeInfo>(256)
        var truncated = false
        val rect = Rect()

        // Iterative DFS keeps deep trees from blowing the stack.
        val stack = ArrayDeque<Triple<AccessibilityNodeInfo, Int, Int>>()
        stack.addLast(Triple(root, -1, 0))
        while (stack.isNotEmpty()) {
            if (nodes.size >= maxNodes || SystemClock.uptimeMillis() - start > maxMillis) {
                truncated = true
                break
            }
            val (info, parent, depth) = stack.removeLast()
            info.getBoundsInScreen(rect)
            val index = nodes.size
            nodes.add(
                UiNode(
                    index = index,
                    parent = parent,
                    depth = depth,
                    id = info.viewIdResourceName,
                    text = info.text?.toString()?.takeIf { it.isNotBlank() },
                    desc = info.contentDescription?.toString()?.takeIf { it.isNotBlank() },
                    hint = info.hintText?.toString()?.takeIf { it.isNotBlank() },
                    cls = info.className?.toString(),
                    selected = info.isSelected,
                    clickable = info.isClickable,
                    scrollable = info.isScrollable,
                    editable = info.isEditable,
                    visible = info.isVisibleToUser,
                    left = rect.left,
                    top = rect.top,
                    right = rect.right,
                    bottom = rect.bottom,
                ),
            )
            platform.add(info)
            // Push children in reverse so they come off the stack in order.
            for (i in info.childCount - 1 downTo 0) {
                val child = try {
                    // Prefetching pulls the subtree into the local cache in a few IPCs instead of one per node.
                    if (Build.VERSION.SDK_INT >= 33) info.getChild(i, PREFETCH) else info.getChild(i)
                } catch (_: Exception) {
                    null
                } ?: continue
                stack.addLast(Triple(child, index, depth + 1))
            }
        }
        return Result(
            UiTree(packageName, nodes, screenWidth, screenHeight, truncated),
            platform,
            SystemClock.uptimeMillis() - start,
        )
    }

    companion object {
        const val PREFETCH = AccessibilityNodeInfo.FLAG_PREFETCH_DESCENDANTS_HYBRID
    }
}
