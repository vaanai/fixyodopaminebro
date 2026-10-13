package app.quiet.data.policy

import org.json.JSONObject
import quiet.core.rules.Policy

object StoredPolicyJson {
    fun parseValue(raw: String, default: Policy): Policy {
        if (raw.isBlank()) return default
        return try {
            Policy.valueOf(raw)
        } catch (_: IllegalArgumentException) {
            default
        }
    }

    fun parseMap(json: String, entryDefault: Policy = Policy.ALLOW): Map<String, Policy> {
        if (json.isBlank() || json == "{}") return emptyMap()
        return try {
            val obj = JSONObject(json)
            obj.keys().asSequence().associateWith { key ->
                parseValue(obj.getString(key), entryDefault)
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
