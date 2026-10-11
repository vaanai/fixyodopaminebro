package quiet.core.rules

object RuleValidator {
    fun validate(rules: AppRules): String? {
        var seenNonProtected = false
        for (surface in rules.surfaces) {
            if (surface.kind == SurfaceKind.protected) {
                if (seenNonProtected) {
                    return "Protected surfaces must come before other kinds"
                }
                if (surface.defaultPolicy != Policy.ALLOW) {
                    return "Protected surface ${surface.fullId} must have ALLOW policy"
                }
            } else {
                seenNonProtected = true
                if (surface.kind == SurfaceKind.feed && surface.defaultPolicy == Policy.OPEN_ONLY) {
                    return "OPEN_ONLY is not valid on feed surface ${surface.fullId}"
                }
            }
        }
        return null
    }
}
