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
        val tail = if (hasUndetected) "Other surfaces not detected yet" else null
        return (feedParts + listOfNotNull(tail)).joinToString(" · ").ifBlank { "Other surfaces not detected yet" }
    }

    private fun parsePolicies(json: String): Map<String, Policy> {
        if (json.isBlank() || json == "{}") return emptyMap()
        val obj = org.json.JSONObject(json)
        return obj.keys().asSequence().associateWith { Policy.valueOf(obj.getString(it)) }
    }
}
