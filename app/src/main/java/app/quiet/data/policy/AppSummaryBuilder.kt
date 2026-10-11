package app.quiet.data.policy

import app.quiet.data.prefs.SettingsSnapshot
import quiet.core.rules.AppRules
import quiet.core.rules.Policy
import quiet.core.rules.SurfaceKind

object AppSummaryBuilder {
    fun build(rules: AppRules, snapshot: SettingsSnapshot): String {
        val policies = parsePolicies(snapshot.surfacePoliciesJson)
        val friends = parsePolicies(snapshot.friendsReelsJson)
        val parts = rules.surfaces
            .filter { it.kind == SurfaceKind.feed && it.enforce }
            .map { surface ->
                val policy = friends[surface.fullId] ?: policies[surface.fullId] ?: surface.defaultPolicy
                "${surface.label} ${policyVerb(policy)}"
            }
        val unenforced = rules.surfaces.any { !it.enforce && (it.kind == SurfaceKind.feed || it.kind == SurfaceKind.shared_media) }
        val tail = if (unenforced) "Some surfaces not detected yet" else null
        return (parts + listOfNotNull(tail)).joinToString(" · ").ifBlank { "Some surfaces not detected yet" }
    }

    fun friendsLine(surfaceId: String, snapshot: SettingsSnapshot): String {
        val policy = parsePolicies(snapshot.friendsReelsJson)[surfaceId] ?: Policy.BLOCK
        return when (policy) {
            Policy.OPEN_ONLY, Policy.ALLOW -> "Friends' reels open"
            else -> "Friends' reels blocked"
        }
    }

    private fun policyVerb(policy: Policy): String = when (policy) {
        Policy.BLOCK -> "blocked"
        Policy.GATE -> "gated"
        Policy.ALLOW, Policy.OPEN_ONLY -> "open"
    }

    private fun parsePolicies(json: String): Map<String, Policy> {
        if (json.isBlank() || json == "{}") return emptyMap()
        val obj = org.json.JSONObject(json)
        return obj.keys().asSequence().associateWith { Policy.valueOf(obj.getString(it)) }
    }
}
