package quiet.core.classify

import quiet.core.model.NodeInfo
import quiet.core.rules.AppRules
import quiet.core.rules.ClassificationResult
import quiet.core.rules.MatchKey
import quiet.core.rules.MatchRules
import quiet.core.rules.SurfaceRule

object SurfaceClassifier {
    fun classify(nodes: List<NodeInfo>, rules: AppRules, truncated: Boolean): ClassificationResult {
        if (nodes.isEmpty()) return ClassificationResult.UNKNOWN
        for (surface in rules.surfaces) {
            val key = matchesSurface(nodes, surface.match, rules.messagesTabLabels)
            if (key != null) {
                if (truncated && key != MatchKey.selectedTab) {
                    return ClassificationResult.UNKNOWN.copy(truncatedRejected = true)
                }
                return ClassificationResult(
                    surfaceId = surface.fullId,
                    kind = surface.kind,
                    enforce = surface.enforce,
                    matchedKey = key,
                )
            }
        }
        return ClassificationResult.UNKNOWN
    }

    private fun matchesSurface(
        nodes: List<NodeInfo>,
        match: MatchRules,
        messagesTabLabels: List<String>,
    ): MatchKey? {
        val keysPresent = mutableListOf<MatchKey>()
        if (match.selectedTab.isNotEmpty()) keysPresent.add(MatchKey.selectedTab)
        if (match.hasLabel.isNotEmpty()) keysPresent.add(MatchKey.hasLabel)
        if (match.hasIdContains.isNotEmpty()) keysPresent.add(MatchKey.hasIdContains)
        if (keysPresent.isEmpty()) return null

        var matched: MatchKey? = null
        if (match.selectedTab.isNotEmpty()) {
            if (nodes.any { it.isSelected && it.labelTexts().any { t -> match.selectedTab.any { v -> v.equals(t, true) } } }) {
                matched = MatchKey.selectedTab
            } else {
                return null
            }
        }
        if (match.hasLabel.isNotEmpty()) {
            val ok = nodes.any { node ->
                node.labelTexts().any { t -> match.hasLabel.any { v -> v.equals(t, true) } }
            }
            if (!ok) return null
            matched = MatchKey.hasLabel
        }
        if (match.hasIdContains.isNotEmpty()) {
            val ok = nodes.any { node ->
                val id = node.viewId ?: return@any false
                match.hasIdContains.any { id.contains(it) }
            }
            if (!ok) return null
            matched = MatchKey.hasIdContains
        }
        return matched
    }

    fun isMessagesTabSelected(nodes: List<NodeInfo>, labels: List<String>): Boolean {
        return nodes.any { node ->
            node.isSelected && node.labelTexts().any { t -> labels.any { l -> l.equals(t, true) } }
        }
    }

    fun findMessagesNode(nodes: List<NodeInfo>, labels: List<String>): NodeInfo? {
        return nodes.firstOrNull { node ->
            node.labelTexts().any { t -> labels.any { l -> l.equals(t, true) } }
        }
    }
}
