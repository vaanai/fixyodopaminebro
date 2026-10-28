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

    /**
     * [tabWords]: when the walk is cut short, look these labels up (the app's tabs) and add what's found, so a
     * busy feed read in part still shows which tab is open. See [quiet.core.rules.AppRules.tabWords].
     */
    fun read(
        root: AccessibilityNodeInfo,
        packageName: String,
        screenWidth: Int,
        screenHeight: Int,
        tabWords: List<String> = emptyList(),
    ): Result {
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
        if (truncated && tabWords.isNotEmpty()) addTabs(root, tabWords, nodes, platform)
        return Result(
            UiTree(packageName, nodes, screenWidth, screenHeight, truncated),
            platform,
            SystemClock.uptimeMillis() - start,
        )
    }

    /**
     * Finds each label with one search inside the app (far quicker than walking a busy tree) and adds the
     * node with a couple of its ancestors and its siblings, which is where apps keep "selected".
     */
    private fun addTabs(root: AccessibilityNodeInfo, words: List<String>, nodes: MutableList<UiNode>, platform: MutableList<AccessibilityNodeInfo>) {
        val start = SystemClock.uptimeMillis()
        // Nodes already read, by identity, so a hit the walk did reach (or an ancestor two labels share) is reused.
        val known = HashMap<AccessibilityNodeInfo, Int>(platform.size * 2)
        platform.forEachIndexed { i, info -> known[info] = i }
        val rect = Rect()
        var added = 0

        fun add(info: AccessibilityNodeInfo, parent: Int): Int {
            known[info]?.let { return it }
            info.getBoundsInScreen(rect)
            val index = nodes.size
            nodes.add(
                UiNode(
                    index = index,
                    parent = parent,
                    depth = if (parent < 0) 0 else nodes[parent].depth + 1,
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
                    extra = true,
                ),
            )
            platform.add(info)
            known[info] = index
            added++
            return index
        }

        for (word in words) {
            if (added >= MAX_TAB_NODES || SystemClock.uptimeMillis() - start > TAB_SEARCH_MS) break
            val found = runCatching { root.findAccessibilityNodeInfosByText(word) }.getOrNull() ?: continue
            for (hit in found) {
                if (added >= MAX_TAB_NODES) break
                if (hit in known) continue
                // Ancestors first, so each node's parent is already in the list.
                val chain = ArrayList<AccessibilityNodeInfo>(TAB_ANCESTORS)
                var p = parentOf(hit)
                while (p != null && chain.size < TAB_ANCESTORS) {
                    chain.add(0, p)
                    p = parentOf(p)
                }
                var parent = -1
                for (a in chain) parent = add(a, parent)
                add(hit, parent)
                // Siblings: an icon next to the label is sometimes the one marked selected.
                val holder = chain.lastOrNull() ?: continue
                for (i in 0 until minOf(holder.childCount, TAB_SIBLINGS)) {
                    val sib = runCatching { holder.getChild(i) }.getOrNull() ?: continue
                    add(sib, parent)
                }
            }
        }
    }

    private fun parentOf(node: AccessibilityNodeInfo): AccessibilityNodeInfo? = try {
        node.parent
    } catch (_: Exception) {
        null
    }

    companion object {
        private const val TAB_SEARCH_MS = 120L
        private const val MAX_TAB_NODES = 120
        private const val TAB_ANCESTORS = 2
        private const val TAB_SIBLINGS = 6
        const val PREFETCH = AccessibilityNodeInfo.FLAG_PREFETCH_DESCENDANTS_HYBRID
    }
}
