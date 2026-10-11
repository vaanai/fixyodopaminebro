package quiet.core.rules

import org.json.JSONObject

object RuleParser {
    fun parseResource(json: String, sourceName: String): Pair<AppRules?, String?> {
        return try {
            val root = JSONObject(json)
            val warnings = mutableListOf<String>()
            val app = root.getString("app")
            val surfacesJson = root.getJSONArray("surfaces")
            val surfaces = mutableListOf<SurfaceRule>()
            for (i in 0 until surfacesJson.length()) {
                val s = surfacesJson.getJSONObject(i)
                val id = s.getString("id")
                val matchObj = s.getJSONObject("match")
                if (matchObj.has("lacksLabel")) {
                    return Pair(null, "$sourceName: lacksLabel is not supported in v1")
                }
                val match = MatchRules(
                    selectedTab = matchObj.optStringList("selectedTab"),
                    hasLabel = matchObj.optStringList("hasLabel"),
                    hasIdContains = matchObj.optStringList("hasIdContains"),
                )
                val kind = SurfaceKind.valueOf(s.getString("kind"))
                val policy = Policy.valueOf(s.getString("defaultPolicy"))
                val enforce = s.optBoolean("enforce", kind == SurfaceKind.protected)
                surfaces.add(
                    SurfaceRule(
                        id = id,
                        fullId = "$app.$id",
                        label = s.getString("label"),
                        kind = kind,
                        defaultPolicy = policy,
                        enforce = enforce,
                        match = match,
                        notes = s.optString("notes", null),
                    ),
                )
            }
            val verified = root.optBoolean("verified", false)
            if (!verified) {
                warnings.add("Unverified rules for ${root.getString("displayName")}.")
            }
            val rules = AppRules(
                schemaVersion = root.getInt("schemaVersion"),
                app = app,
                displayName = root.getString("displayName"),
                packages = root.getJSONArray("packages").toStringList(),
                messagesTabLabels = root.getJSONArray("messagesTabLabels").toStringList(),
                verified = verified,
                surfaces = surfaces,
            )
            val validationError = RuleValidator.validate(rules)
            if (validationError != null) {
                return Pair(null, "$sourceName: $validationError")
            }
            Pair(rules, null)
        } catch (e: Exception) {
            Pair(null, "$sourceName: ${e.message}")
        }
    }

    fun loadAll(vararg pairs: Pair<String, String>): ParsedRules {
        val apps = mutableListOf<AppRules>()
        val errors = mutableMapOf<String, String>()
        val warnings = mutableListOf<String>()
        for ((name, json) in pairs) {
            val (rules, error) = parseResource(json, name)
            if (rules != null) {
                apps.add(rules)
                if (!rules.verified) {
                    warnings.add("Unverified rules for ${rules.displayName}.")
                }
            } else if (error != null) {
                val appKey = name.substringBefore('.')
                errors[appKey] = error
            }
        }
        return ParsedRules(apps, errors, warnings)
    }

    private fun JSONObject.optStringList(key: String): List<String> {
        if (!has(key)) return emptyList()
        val arr = getJSONArray(key)
        return arr.toStringList()
    }

    private fun org.json.JSONArray.toStringList(): List<String> {
        val out = ArrayList<String>(length())
        for (i in 0 until length()) {
            out.add(getString(i))
        }
        return out
    }
}
