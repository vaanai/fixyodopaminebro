package quiet.core.detect

import quiet.core.model.UiNode
import quiet.core.model.UiTree
import quiet.core.rules.AppRules
import quiet.core.rules.SurfaceKind

/** What the detector made of one screen. */
data class Reading(
    val app: String,
    val surface: String?,
    val kind: SurfaceKind?,
    /** Signals that were present, for the Inspector and logs. */
    val signals: Set<String>,
    /** Fingerprint of the video on screen; changes when the user swipes to a new one. */
    val signature: String,
    /** Y coordinate where the app's bottom tab bar begins, or null when there is no tab bar on screen. */
    val tabBarTop: Int?,
    val truncated: Boolean,
    /** The surface's open top (like the Stories row above Home) is on screen: you haven't scrolled past it. */
    val atTop: Boolean = false,
    /**
     * Nothing on screen could be told apart: no window, an empty tree, or only unnamed containers (a
     * screen mid-transition). As opposed to a screen with content that no rule matched.
     */
    val blank: Boolean = false,
) {
    val isUnknown: Boolean get() = surface == null

    companion object {
        fun unknown(app: String, truncated: Boolean = false) =
            Reading(app, null, null, emptySet(), "", null, truncated, blank = true)
    }
}

object Detector {
    private val NUMBERS = Regex("""\d[\d.,:]*\s*[KMBkmb]?""")

    fun read(rules: AppRules, tree: UiTree): Reading {
        if (tree.nodes.isEmpty()) return Reading.unknown(rules.app, tree.truncated)
        val present = rules.signals.values.filter { it.firstMatch(tree) != null }.map { it.name }.toSet()

        val rule = rules.rules.firstOrNull { r ->
            val c = r.condition
            c.all.all { it in present } &&
                (c.any.isEmpty() || c.any.any { it in present }) &&
                c.none.none { it in present }
        }

        val tabBarTop = rules.tabBarSignal
            ?.let { rules.signals[it]?.firstMatch(tree) }
            ?.let { tree.clickableSelfOrAncestor(it) ?: it }
            ?.takeIf { it.top > tree.screenHeight / 2 }
            ?.top

        val top = rule?.let { rules.surface(it.surface)?.openTop }
        return Reading(
            app = rules.app,
            surface = rule?.surface,
            kind = rule?.kind,
            signals = present,
            signature = signature(tree, rules.signatureIds),
            tabBarTop = tabBarTop,
            truncated = tree.truncated,
            atTop = top != null && top.signal in present,
            blank = tree.nodes.none { n -> tree.isOnScreen(n) && (n.id != null || n.labels().isNotEmpty()) },
        )
    }

    /**
     * A fingerprint of what's on screen, built from visible text that doesn't change while a video
     * plays. Numbers are masked out (like counts, timestamps and view counts tick live), so
     * "1.2M likes" and "1.3M likes" read the same.
     * When the rules name signature ids (author name, caption), only those nodes are used.
     */
    fun signature(tree: UiTree, signatureIds: List<String>): String {
        val preferred = if (signatureIds.isEmpty()) emptyList() else tree.nodes.filter { n ->
            tree.isOnScreen(n) && signatureIds.any { sid -> n.id?.contains(sid, ignoreCase = true) == true }
        }
        val pool: List<UiNode> = preferred.ifEmpty { tree.nodes.filter { tree.isOnScreen(it) } }
        val parts = pool.asSequence()
            .flatMap { it.labels().asSequence() }
            .map { it.trim().take(80).replace(NUMBERS, "#") }
            .filter { label -> label.count { it.isLetter() } >= 2 }
            .distinct()
            .take(24)
            .toList()
        if (parts.isEmpty()) return ""
        return parts.joinToString("|").hashCode().toUInt().toString(16)
    }

    /** Finds the node a nav target points at and returns the node to click. */
    fun findNavNode(rules: AppRules, tree: UiTree, targetId: String): UiNode? {
        val target = rules.navTarget(targetId) ?: return null
        val signal = rules.signals[target.signal] ?: return null
        val node = signal.firstMatch(tree) ?: return null
        return tree.clickableSelfOrAncestor(node) ?: node
    }
}
