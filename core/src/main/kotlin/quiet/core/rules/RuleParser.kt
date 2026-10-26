package quiet.core.rules

import org.json.JSONArray
import org.json.JSONObject

class RuleParseException(message: String) : Exception(message)

object RuleParser {
    const val SCHEMA_VERSION = 2

    fun parse(json: String): AppRules {
        val root = try {
            JSONObject(json)
        } catch (e: Exception) {
            throw RuleParseException("Not valid JSON: ${e.message}")
        }
        val schema = root.optInt("schemaVersion", -1)
        if (schema != SCHEMA_VERSION) throw RuleParseException("schemaVersion must be $SCHEMA_VERSION, was $schema")
        val app = root.reqString("app")

        val signals = mutableMapOf<String, Signal>()
        val signalsJson = root.optJSONObject("signals") ?: JSONObject()
        for (name in signalsJson.keys()) {
            val arr = signalsJson.optJSONArray(name) ?: throw RuleParseException("signal $name must be an array")
            val matchers = (0 until arr.length()).map { i -> parseMatcher(arr.getJSONObject(i), "$name[$i]") }
            signals[name] = Signal(name, matchers)
        }

        val surfaces = root.optJSONArray("surfaces").objects().map { s ->
            val id = s.reqString("id")
            val openTop = s.optJSONObject("openTop")?.let { k ->
                val signal = k.reqString("signal")
                if (signal !in signals) throw RuleParseException("surface $id's openTop uses unknown signal $signal")
                OpenTop(
                    signal = signal,
                    name = k.reqString("name"),
                    surface = k.optString("surface", "").ifBlank { null },
                    back = k.optString("back", "").ifBlank { null },
                    description = k.optString("description", ""),
                )
            }
            SurfaceInfo(
                id = id,
                label = s.reqString("label"),
                kind = parseKind(s.reqString("kind")),
                description = s.optString("description", ""),
                openTop = openTop,
            )
        }
        val surfaceIds = surfaces.map { it.id }.toSet()
        for (s in surfaces) {
            val to = s.openTop?.surface ?: continue
            if (to !in surfaceIds) throw RuleParseException("surface ${s.id}'s openTop leads to unknown surface $to")
        }

        val rules = root.optJSONArray("rules").objects().mapIndexed { i, r ->
            val surface = r.reqString("surface")
            if (surface !in surfaceIds) throw RuleParseException("rules[$i] points at unknown surface $surface")
            val info = surfaces.first { it.id == surface }
            val cond = Condition(
                all = r.optJSONArray("all").strings(),
                any = r.optJSONArray("any").strings(),
                none = r.optJSONArray("none").strings(),
            )
            if (cond.isEmpty) throw RuleParseException("rules[$i] ($surface) needs 'all' or 'any'")
            for (s in cond.all + cond.any + cond.none) {
                if (s !in signals) throw RuleParseException("rules[$i] ($surface) uses unknown signal $s")
            }
            SurfaceRule(surface, info.kind, cond)
        }

        val nav = root.optJSONArray("nav").objects().map { n ->
            val signal = n.reqString("signal")
            if (signal !in signals) throw RuleParseException("nav ${n.optString("id")} uses unknown signal $signal")
            NavTarget(n.reqString("id"), n.reqString("label"), signal)
        }

        for (s in surfaces) {
            val back = s.openTop?.back ?: continue
            if (nav.none { it.id == back }) throw RuleParseException("surface ${s.id}'s openTop goes back via unknown nav target $back")
        }

        fun optSignal(key: String): String? {
            val v = root.optString(key, "").ifBlank { null } ?: return null
            if (v !in signals) throw RuleParseException("$key uses unknown signal $v")
            return v
        }

        val autoSkip = root.optString("autoSkipTarget", "").ifBlank { null }
        if (autoSkip != null && nav.none { it.id == autoSkip }) {
            throw RuleParseException("autoSkipTarget $autoSkip is not a nav target")
        }

        val launch = root.optJSONObject("launch")?.let { l ->
            val surface = l.reqString("surface")
            val signal = l.reqString("signal")
            if (surface !in surfaceIds) throw RuleParseException("launch points at unknown surface $surface")
            if (signal !in signals) throw RuleParseException("launch uses unknown signal $signal")
            LaunchGuess(surface, signal)
        }

        return AppRules(
            app = app,
            displayName = root.reqString("displayName"),
            packages = root.optJSONArray("packages").strings().also {
                if (it.isEmpty()) throw RuleParseException("packages must not be empty")
            },
            version = root.optInt("version", 1),
            verified = root.optBoolean("verified", false),
            signals = signals,
            surfaces = surfaces,
            rules = rules,
            nav = nav,
            tabBarSignal = optSignal("tabBarSignal"),
            pagerSignal = optSignal("pagerSignal"),
            signatureIds = root.optJSONArray("signatureIds").strings(),
            autoSkipTarget = autoSkip,
            launch = launch,
        )
    }

    private val matcherKeys = setOf(
        "id", "idExact", "text", "textContains", "textStartsWith", "desc", "descContains", "descStartsWith",
        "hintContains", "labelRegex", "cls", "selected", "editable", "scrollable", "minHeightFraction",
        "minTopFraction", "maxBottomFraction", "onScreen", "note",
    )

    private fun parseMatcher(o: JSONObject, where: String): NodeMatcher {
        for (k in o.keys()) if (k !in matcherKeys) throw RuleParseException("$where: unknown key '$k'")
        fun s(key: String): String? = if (o.has(key)) o.getString(key) else null
        fun b(key: String): Boolean? = if (o.has(key)) o.getBoolean(key) else null
        fun d(key: String): Double? = if (o.has(key)) o.getDouble(key) else null
        val regex = s("labelRegex")?.let {
            try {
                Regex(it, RegexOption.IGNORE_CASE)
            } catch (e: Exception) {
                throw RuleParseException("$where: bad labelRegex: ${e.message}")
            }
        }
        val m = NodeMatcher(
            id = s("id"), idExact = s("idExact"), text = s("text"), textContains = s("textContains"),
            textStartsWith = s("textStartsWith"), desc = s("desc"), descContains = s("descContains"),
            descStartsWith = s("descStartsWith"), hintContains = s("hintContains"), labelRegex = regex,
            cls = s("cls"), selected = b("selected"), editable = b("editable"), scrollable = b("scrollable"),
            minHeightFraction = d("minHeightFraction"), minTopFraction = d("minTopFraction"),
            maxBottomFraction = d("maxBottomFraction"), onScreen = b("onScreen") ?: true,
        )
        val hasText = listOf(m.id, m.idExact, m.text, m.textContains, m.textStartsWith, m.desc, m.descContains,
            m.descStartsWith, m.hintContains, m.cls).any { it != null } || regex != null
        if (!hasText) throw RuleParseException("$where: needs at least one of id/text/desc/hint/cls/labelRegex")
        return m
    }

    private fun parseKind(s: String): SurfaceKind = try {
        SurfaceKind.valueOf(s)
    } catch (e: Exception) {
        throw RuleParseException("unknown kind $s")
    }

    private fun JSONObject.reqString(key: String): String =
        optString(key, "").ifBlank { throw RuleParseException("missing '$key'") }

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).map { getJSONObject(it) }

    private fun JSONArray?.strings(): List<String> =
        if (this == null) emptyList() else (0 until length()).map { getString(it) }
}
