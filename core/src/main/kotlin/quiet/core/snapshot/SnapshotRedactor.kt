package quiet.core.snapshot

data class SnapshotNode(
    val className: String?,
    val bounds: String,
    val depth: Int,
    val clickable: Boolean,
    val enabled: Boolean,
    val selected: Boolean,
    val viewId: String?,
    val text: String?,
    val contentDescription: String?,
    val children: List<SnapshotNode> = emptyList(),
)

object SnapshotRedactor {
    private val vocabulary = setOf(
        "Home", "Reels", "Shorts", "For You", "Following", "Discover", "Explore",
        "Search", "Messages", "Direct", "Inbox", "Friends", "Profile", "Create",
        "Activity", "Stories", "Library", "Subscriptions",
    )

    fun redactText(raw: String?): String? {
        if (raw.isNullOrBlank()) return raw
        return if (vocabulary.any { it.equals(raw, ignoreCase = true) }) {
            raw
        } else {
            "<redacted len=${raw.length}>"
        }
    }

    fun redactNode(node: SnapshotNode): SnapshotNode = SnapshotNode(
        className = node.className,
        bounds = node.bounds,
        depth = node.depth,
        clickable = node.clickable,
        enabled = node.enabled,
        selected = node.selected,
        viewId = node.viewId,
        text = redactText(node.text),
        contentDescription = redactText(node.contentDescription),
        children = node.children.map { redactNode(it) },
    )

    fun collectForeignStrings(node: SnapshotNode): List<String> {
        val out = mutableListOf<String>()
        fun walk(n: SnapshotNode) {
            n.text?.let {
                if (!it.startsWith("<redacted") && vocabulary.none { v -> v.equals(it, true) }) out.add(it)
            }
            n.contentDescription?.let {
                if (!it.startsWith("<redacted") && vocabulary.none { v -> v.equals(it, true) }) out.add(it)
            }
            n.children.forEach { walk(it) }
        }
        walk(node)
        return out
    }
}
