package quiet.core.rules

enum class SurfaceKind {
    protected,
    feed,
    shared_media,
    other,
}

enum class Policy {
    ALLOW,
    GATE,
    BLOCK,
    OPEN_ONLY,
}

data class MatchRules(
    val selectedTab: List<String> = emptyList(),
    val hasLabel: List<String> = emptyList(),
    val hasIdContains: List<String> = emptyList(),
)

data class SurfaceRule(
    val id: String,
    val fullId: String,
    val label: String,
    val kind: SurfaceKind,
    val defaultPolicy: Policy,
    val enforce: Boolean,
    val match: MatchRules,
    val notes: String? = null,
)

data class AppRules(
    val schemaVersion: Int,
    val app: String,
    val displayName: String,
    val packages: List<String>,
    val messagesTabLabels: List<String>,
    val verified: Boolean,
    val surfaces: List<SurfaceRule>,
)

data class ParsedRules(
    val apps: List<AppRules>,
    val errors: Map<String, String>,
    val warnings: List<String>,
)

data class ClassificationResult(
    val surfaceId: String?,
    val kind: SurfaceKind?,
    val enforce: Boolean,
    val matchedKey: MatchKey?,
    val truncatedRejected: Boolean = false,
) {
    val isUnknown: Boolean = surfaceId == null

    companion object {
        val UNKNOWN = ClassificationResult(null, null, false, null)
    }
}

enum class MatchKey {
    selectedTab,
    hasLabel,
    hasIdContains,
}
