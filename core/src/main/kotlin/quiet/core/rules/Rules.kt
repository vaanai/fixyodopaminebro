package quiet.core.rules

import quiet.core.model.UiNode
import quiet.core.model.UiTree

/** What sort of place inside an app a screen is. Policies are written against these. */
enum class SurfaceKind {
    /** An endless vertical video feed: Instagram Reels tab, TikTok For You, YouTube Shorts tab. */
    VIDEO_FEED,

    /** A single short video opened from somewhere else (a DM, a profile, the home feed). */
    VIDEO_VIEWER,

    /** A scrolling feed of posts (Instagram Home). */
    FEED,

    /** Search / Explore / Discover grids. */
    EXPLORE,

    /** Stories from people you follow. */
    STORIES,

    /** Inbox and chat threads. Never blocked. */
    MESSAGES,

    PROFILE,
    LIVE,
    OTHER,
}

/**
 * One condition on a single node. Every field that is set must hold. String comparisons ignore case.
 * A rule with no fields set never matches, so a typo can't turn into "match everything".
 */
data class NodeMatcher(
    val id: String? = null,
    val idExact: String? = null,
    val text: String? = null,
    val textContains: String? = null,
    val textStartsWith: String? = null,
    val desc: String? = null,
    val descContains: String? = null,
    val descStartsWith: String? = null,
    val hintContains: String? = null,
    val labelRegex: Regex? = null,
    val cls: String? = null,
    val selected: Boolean? = null,
    val editable: Boolean? = null,
    val scrollable: Boolean? = null,
    /** Node must take at least this share of the screen height (0..1). Useful for "is this the full-screen pager". */
    val minHeightFraction: Double? = null,
    /** Node must sit in the bottom part of the screen (top edge below this fraction). Used for tab bars. */
    val minTopFraction: Double? = null,
    /** Node must sit in the top part of the screen (bottom edge above this fraction). Used for top tabs. */
    val maxBottomFraction: Double? = null,
    /** Off-screen nodes (pages a pager keeps ready) normally don't count. */
    val onScreen: Boolean = true,
) {
    private val hasCondition: Boolean =
        listOf(id, idExact, text, textContains, textStartsWith, desc, descContains, descStartsWith, hintContains, cls)
            .any { it != null } || labelRegex != null

    fun matches(node: UiNode, tree: UiTree): Boolean {
        if (!hasCondition) return false
        if (onScreen && !tree.isOnScreen(node)) return false
        if (id != null && node.id?.contains(id, ignoreCase = true) != true) return false
        if (idExact != null && node.shortId?.equals(idExact, ignoreCase = true) != true) return false
        if (text != null && node.text?.trim()?.equals(text, ignoreCase = true) != true) return false
        if (textContains != null && node.text?.contains(textContains, ignoreCase = true) != true) return false
        if (textStartsWith != null && node.text?.trim()?.startsWith(textStartsWith, ignoreCase = true) != true) return false
        if (desc != null && node.desc?.trim()?.equals(desc, ignoreCase = true) != true) return false
        if (descContains != null && node.desc?.contains(descContains, ignoreCase = true) != true) return false
        if (descStartsWith != null && node.desc?.trim()?.startsWith(descStartsWith, ignoreCase = true) != true) return false
        if (hintContains != null && node.hint?.contains(hintContains, ignoreCase = true) != true) return false
        if (labelRegex != null && node.labels().none { labelRegex.containsMatchIn(it) }) return false
        if (cls != null && node.cls?.contains(cls, ignoreCase = true) != true) return false
        if (selected != null && tree.isSelectedNear(node) != selected) return false
        if (editable != null && node.editable != editable) return false
        if (scrollable != null && node.scrollable != scrollable) return false
        if (minHeightFraction != null && tree.screenHeight > 0 &&
            node.height < tree.screenHeight * minHeightFraction
        ) return false
        if (minTopFraction != null && tree.screenHeight > 0 && node.top < tree.screenHeight * minTopFraction) return false
        if (maxBottomFraction != null && tree.screenHeight > 0 && node.bottom > tree.screenHeight * maxBottomFraction) return false
        return true
    }
}

/** A named signal is true when any of its matchers matches any node. */
data class Signal(val name: String, val anyOf: List<NodeMatcher>) {
    fun firstMatch(tree: UiTree): UiNode? = tree.nodes.firstOrNull { node -> anyOf.any { it.matches(node, tree) } }
}

/** `all` signals must be present, at least one of `any` (if given), and none of `none`. */
data class Condition(
    val all: List<String> = emptyList(),
    val any: List<String> = emptyList(),
    val none: List<String> = emptyList(),
) {
    val isEmpty: Boolean get() = all.isEmpty() && any.isEmpty()
}

data class SurfaceRule(
    val surface: String,
    val kind: SurfaceKind,
    val condition: Condition,
)

/** A surface the user can set a policy for. Several rules can lead to the same surface. */
data class SurfaceInfo(
    val id: String,
    val label: String,
    val kind: SurfaceKind,
    val description: String = "",
)

/** Where a button on the block screen should take you inside the app. */
data class NavTarget(val id: String, val label: String, val signal: String)

data class AppRules(
    val app: String,
    val displayName: String,
    val packages: List<String>,
    val version: Int,
    val verified: Boolean,
    val signals: Map<String, Signal>,
    val surfaces: List<SurfaceInfo>,
    val rules: List<SurfaceRule>,
    val nav: List<NavTarget>,
    /** Signal whose top edge marks where the app's bottom tab bar starts; the block cover stops above it. */
    val tabBarSignal: String?,
    /** Signal that finds the vertical video pager, so scroll events from it count as swipes. */
    val pagerSignal: String?,
    /** View ids (substring) whose text identifies the current video, e.g. the author name. */
    val signatureIds: List<String>,
    /** Where to go when the user lands on a blocked feed and auto-skip is on. */
    val autoSkipTarget: String?,
) {
    fun surface(id: String): SurfaceInfo? = surfaces.firstOrNull { it.id == id }
    fun navTarget(id: String): NavTarget? = nav.firstOrNull { it.id == id }
}
