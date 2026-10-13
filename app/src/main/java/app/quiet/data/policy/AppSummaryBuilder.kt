package app.quiet.data.policy

import app.quiet.data.prefs.SettingsSnapshot
import quiet.core.rules.AppRules
import quiet.core.rules.Policy
import quiet.core.rules.SurfaceKind

object AppSummaryBuilder {
    fun build(rules: AppRules, snapshot: SettingsSnapshot): String {
        val policies = parsePolicies(snapshot.surfacePoliciesJson)
        val friends = parsePolicies(snapshot.friendsReelsJson)
        val feedParts = rules.surfaces
            .filter { it.kind == SurfaceKind.feed && it.enforce }
            .map { surface ->
                val policy = friends[surface.fullId] ?: policies[surface.fullId] ?: surface.defaultPolicy
                when (policy) {
                    Policy.BLOCK -> "${surface.label} blocked"
                    Policy.GATE -> "${surface.label} gated"
                    Policy.ALLOW, Policy.OPEN_ONLY -> "${surface.label} open"
                }
            }
        val hasUndetected = rules.surfaces.any {
            (it.kind == SurfaceKind.feed || it.kind == SurfaceKind.shared_media) && !it.enforce
        }
        val friendsSurface = rules.surfaces.firstOrNull { it.kind == SurfaceKind.shared_media }
        val friendsPart = friendsSurface?.let { surface ->
            val policy = friends[surface.fullId] ?: surface.defaultPolicy
            when (policy) {
                Policy.BLOCK -> "Friends' reels off"
                Policy.ALLOW -> "Friends' reels allowed"
                Policy.OPEN_ONLY -> "Friends' reels allowed"
                else -> null
            }
        }
        val tail = if (hasUndetected) "Other surfaces not detected yet" else null
        return (feedParts + listOfNotNull(friendsPart, tail))
            .joinToString(" · ")
            .ifBlank { "Other surfaces not detected yet" }
    }

    private fun parsePolicies(json: String): Map<String, Policy> = StoredPolicyJson.parseMap(json)
}
