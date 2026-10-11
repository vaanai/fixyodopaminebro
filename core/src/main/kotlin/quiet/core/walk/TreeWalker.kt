package quiet.core.walk

import quiet.core.QuietTiming
import quiet.core.model.NodeInfo

data class WalkResult(
    val nodes: List<NodeInfo>,
    val truncated: Boolean,
)

object TreeWalker {
    fun walk(root: RawNode?, clock: () -> Long = System::nanoTime): WalkResult {
        if (root == null) {
            return WalkResult(emptyList(), truncated = false)
        }
        val start = clock()
        val recorded = ArrayList<NodeInfo>(128)
        var nonSelectedCount = 0
        var truncated = false

        fun deadlineExceeded(): Boolean {
            val elapsedMs = (clock() - start) / 1_000_000
            return elapsedMs >= QuietTiming.WALK_TIME_CAP_MS
        }

        fun shouldRecord(node: RawNode, selected: Boolean): Boolean {
            if (selected) return true
            val hasSignal = !node.text.isNullOrBlank() ||
                !node.contentDescription.isNullOrBlank() ||
                !node.viewIdResourceName.isNullOrBlank()
            return hasSignal
        }

        fun visit(node: RawNode) {
            if (deadlineExceeded()) {
                truncated = true
                return
            }
            val selected = node.isSelected
            if (shouldRecord(node, selected)) {
                if (!selected) {
                    if (nonSelectedCount >= QuietTiming.WALK_NODE_CAP) {
                        truncated = true
                    } else {
                        nonSelectedCount++
                        recorded.add(node.toNodeInfo())
                    }
                } else {
                    recorded.add(node.toNodeInfo())
                }
            }
            if (deadlineExceeded()) {
                truncated = true
                return
            }
            val childCount = node.childCount
            for (i in 0 until childCount) {
                if (i % 25 == 0 && deadlineExceeded()) {
                    truncated = true
                    return
                }
                val child = node.childAt(i) ?: continue
                visit(child)
            }
        }

        visit(root)
        return WalkResult(recorded, truncated)
    }

    private fun RawNode.toNodeInfo(): NodeInfo = NodeInfo(
        text = text?.toString()?.takeIf { it.isNotBlank() },
        contentDescription = contentDescription?.toString()?.takeIf { it.isNotBlank() },
        viewId = viewIdResourceName,
        isSelected = isSelected,
        isClickable = isClickable,
        className = className?.toString(),
        boundsLeft = boundsLeft,
        boundsTop = boundsTop,
        boundsRight = boundsRight,
        boundsBottom = boundsBottom,
    )
}
