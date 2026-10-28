package quiet.core.model

/**
 * A flattened copy of one window's accessibility tree.
 *
 * The service copies the platform nodes into this shape once per evaluation so that detection,
 * matching and tests all work on plain data with no Android dependency.
 */
data class UiNode(
    val index: Int,
    val parent: Int,
    val depth: Int,
    val id: String? = null,
    val text: String? = null,
    val desc: String? = null,
    val hint: String? = null,
    val cls: String? = null,
    val selected: Boolean = false,
    val clickable: Boolean = false,
    val scrollable: Boolean = false,
    val editable: Boolean = false,
    val visible: Boolean = true,
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0,
    /**
     * Added after a read was cut short, by looking the app's tabs up by label rather than walking to them.
     * Such nodes say which tab is open; they aren't part of what the video fingerprint is built from.
     */
    val extra: Boolean = false,
) {
    val width: Int get() = (right - left).coerceAtLeast(0)
    val height: Int get() = (bottom - top).coerceAtLeast(0)
    val centerY: Int get() = (top + bottom) / 2

    /** Short id without the package prefix, e.g. "clips_tab" for "com.instagram.android:id/clips_tab". */
    val shortId: String? get() = id?.substringAfter(":id/")

    fun labels(): List<String> = listOfNotNull(text, desc).filter { it.isNotBlank() }
}

class UiTree(
    val packageName: String,
    val nodes: List<UiNode>,
    val screenWidth: Int,
    val screenHeight: Int,
    val truncated: Boolean = false,
) {
    private val children: Array<MutableList<Int>> by lazy {
        val out = Array(nodes.size) { mutableListOf<Int>() }
        for (n in nodes) if (n.parent >= 0 && n.parent < nodes.size) out[n.parent].add(n.index)
        out
    }

    /**
     * Apps disagree on where "selected" lives: sometimes on the tab container, sometimes on the icon
     * inside it, sometimes on the label next to it. A node counts as selected when it, an ancestor
     * up to [UP] levels, or a descendant up to [DOWN] levels is selected.
     */
    private val selectedNear: BooleanArray by lazy {
        val out = BooleanArray(nodes.size)
        for (n in nodes) {
            if (!n.selected) continue
            out[n.index] = true
            markDown(n.index, DOWN_FROM_SELECTED, out)
            var p = n.parent
            var hops = 0
            while (p >= 0 && hops < UP_FROM_SELECTED) {
                out[p] = true
                p = nodes[p].parent
                hops++
            }
        }
        out
    }

    private fun markDown(index: Int, levels: Int, out: BooleanArray) {
        if (levels <= 0) return
        for (c in children[index]) {
            out[c] = true
            markDown(c, levels - 1, out)
        }
    }

    fun isSelectedNear(node: UiNode): Boolean = selectedNear[node.index]

    fun childrenOf(node: UiNode): List<UiNode> = children[node.index].map { nodes[it] }

    fun parentOf(node: UiNode): UiNode? = nodes.getOrNull(node.parent)

    /** Nearest clickable node at or above [node]; what to tap when [node] itself isn't clickable. */
    fun clickableSelfOrAncestor(node: UiNode): UiNode? {
        var cur: UiNode? = node
        var hops = 0
        while (cur != null && hops < 6) {
            if (cur.clickable) return cur
            cur = parentOf(cur)
            hops++
        }
        return null
    }

    fun isOnScreen(node: UiNode): Boolean =
        node.visible && node.width > 0 && node.height > 0 &&
            node.right > 0 && node.bottom > 0 &&
            (screenWidth <= 0 || node.left < screenWidth) &&
            (screenHeight <= 0 || node.top < screenHeight)

    companion object {
        const val DOWN_FROM_SELECTED = 3
        const val UP_FROM_SELECTED = 2
    }
}

/** Builds trees in tests and in the service adapter without juggling indices by hand. */
class UiTreeBuilder(private val packageName: String, private val width: Int = 1080, private val height: Int = 2400) {
    private val nodes = mutableListOf<UiNode>()
    private var truncated = false

    fun add(parent: Int, node: (index: Int, depth: Int) -> UiNode): Int {
        val index = nodes.size
        val depth = if (parent < 0) 0 else nodes[parent].depth + 1
        nodes.add(node(index, depth))
        return index
    }

    fun markTruncated() {
        truncated = true
    }

    fun size(): Int = nodes.size

    fun build(): UiTree = UiTree(packageName, nodes.toList(), width, height, truncated)
}
