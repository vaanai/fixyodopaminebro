package quiet.core.settings

import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek

/** Settings to and from JSON. Unknown or missing fields fall back to defaults so old files keep loading. */
object SettingsCodec {
    fun encode(s: Settings): String = JSONObject().apply {
        put("version", 1)
        put("protectionOn", s.protectionOn)
        put("apps", JSONObject().apply { s.apps.forEach { (k, v) -> put(k, encodeApp(v)) } })
        put("budgetMinutes", s.budgetMinutes)
        put("unlocksEnabled", s.unlocksEnabled)
        put("unlockMinutes", s.unlockMinutes)
        put("unlockVideos", s.unlockVideos)
        put("unlockWaitSeconds", s.unlockWaitSeconds)
        put("unlocksPerDay", s.unlocksPerDay)
        put("pauseSeconds", s.pauseSeconds)
        put("pauseMinutes", s.pauseMinutes)
        put("focusWindows", JSONArray().apply { s.focusWindows.forEach { put(encodeWindow(it)) } })
        put("focusAllowsUnlocks", s.focusAllowsUnlocks)
        put("focusUntil", s.focusUntil)
        put("pauseVideo", s.pauseVideo)
        put("haptics", s.haptics)
        put("showTimerPill", s.showTimerPill)
        put("nudgeEveryMinutes", s.nudgeEveryMinutes)
        put("coverStyle", s.coverStyle.name)
        put("keepMessagesOpen", s.keepMessagesOpen)
        put("strictMode", s.strictMode)
        put("strictDelaySeconds", s.strictDelaySeconds)
        put("tracked", JSONArray().apply {
            s.tracked.forEach {
                put(JSONObject().put("package", it.packageName).put("label", it.label).put("limit", it.dailyLimitMinutes))
            }
        })
        put("accent", s.accent.name)
        put("verboseLogging", s.verboseLogging)
        put("onboarded", s.onboarded)
    }.toString(2)

    fun decode(json: String): Settings {
        val o = JSONObject(json)
        val d = Settings()
        val apps = mutableMapOf<String, AppSettings>()
        o.optJSONObject("apps")?.let { a -> for (k in a.keys()) apps[k] = decodeApp(k, a.getJSONObject(k)) }
        return Settings(
            protectionOn = o.optBoolean("protectionOn", d.protectionOn),
            apps = apps,
            budgetMinutes = o.optInt("budgetMinutes", d.budgetMinutes),
            unlocksEnabled = o.optBoolean("unlocksEnabled", d.unlocksEnabled),
            unlockMinutes = o.optInt("unlockMinutes", d.unlockMinutes),
            unlockVideos = o.optInt("unlockVideos", d.unlockVideos),
            unlockWaitSeconds = o.optInt("unlockWaitSeconds", d.unlockWaitSeconds),
            unlocksPerDay = o.optInt("unlocksPerDay", d.unlocksPerDay),
            pauseSeconds = o.optInt("pauseSeconds", d.pauseSeconds),
            pauseMinutes = o.optInt("pauseMinutes", d.pauseMinutes),
            focusWindows = o.optJSONArray("focusWindows")?.let { arr ->
                (0 until arr.length()).mapNotNull { runCatching { decodeWindow(arr.getJSONObject(it)) }.getOrNull() }
            } ?: d.focusWindows,
            focusAllowsUnlocks = o.optBoolean("focusAllowsUnlocks", d.focusAllowsUnlocks),
            focusUntil = o.optLong("focusUntil", d.focusUntil),
            pauseVideo = o.optBoolean("pauseVideo", d.pauseVideo),
            haptics = o.optBoolean("haptics", d.haptics),
            showTimerPill = o.optBoolean("showTimerPill", d.showTimerPill),
            nudgeEveryMinutes = o.optInt("nudgeEveryMinutes", d.nudgeEveryMinutes),
            coverStyle = enumOr(o.optString("coverStyle"), d.coverStyle),
            keepMessagesOpen = o.optBoolean("keepMessagesOpen", d.keepMessagesOpen),
            strictMode = o.optBoolean("strictMode", d.strictMode),
            strictDelaySeconds = o.optInt("strictDelaySeconds", d.strictDelaySeconds),
            tracked = o.optJSONArray("tracked")?.let { arr ->
                (0 until arr.length()).map { i ->
                    val t = arr.getJSONObject(i)
                    TrackedApp(t.getString("package"), t.optString("label", t.getString("package")), t.optInt("limit", 0))
                }
            } ?: d.tracked,
            accent = enumOr(o.optString("accent"), d.accent),
            verboseLogging = o.optBoolean("verboseLogging", d.verboseLogging),
            onboarded = o.optBoolean("onboarded", d.onboarded),
        )
    }

    fun decodeOrDefault(json: String?): Settings =
        if (json.isNullOrBlank()) Settings() else runCatching { decode(json) }.getOrElse { Settings() }

    private fun encodeApp(a: AppSettings) = JSONObject().apply {
        put("enabled", a.enabled)
        put("modes", JSONObject().apply { a.modes.forEach { (k, v) -> put(k, v.name) } })
        put("viewerSwipes", JSONObject().apply { a.viewerSwipes.forEach { (k, v) -> put(k.name, v) } })
        put("dailyLimitMinutes", a.dailyLimitMinutes)
        put("autoSkipTarget", a.autoSkipTarget ?: "")
    }

    private fun decodeApp(id: String, o: JSONObject): AppSettings {
        val d = Defaults.appSettings(id)
        val modes = mutableMapOf<String, Mode>()
        o.optJSONObject("modes")?.let { m -> for (k in m.keys()) enumOrNull<Mode>(m.getString(k))?.let { modes[k] = it } }
        val swipes = mutableMapOf<Origin, Int>()
        o.optJSONObject("viewerSwipes")?.let { m -> for (k in m.keys()) enumOrNull<Origin>(k)?.let { swipes[it] = m.getInt(k) } }
        return AppSettings(
            enabled = o.optBoolean("enabled", d.enabled),
            modes = d.modes + modes,
            viewerSwipes = d.viewerSwipes + swipes,
            dailyLimitMinutes = o.optInt("dailyLimitMinutes", d.dailyLimitMinutes),
            autoSkipTarget = if (o.has("autoSkipTarget")) o.optString("autoSkipTarget").ifBlank { null } else d.autoSkipTarget,
        )
    }

    private fun encodeWindow(w: FocusWindow) = JSONObject().apply {
        put("id", w.id)
        put("label", w.label)
        put("days", JSONArray().apply { w.days.sorted().forEach { put(it.name) } })
        put("start", w.startMinute)
        put("end", w.endMinute)
        put("enabled", w.enabled)
    }

    private fun decodeWindow(o: JSONObject): FocusWindow {
        val days = o.optJSONArray("days")?.let { arr ->
            (0 until arr.length()).mapNotNull { enumOrNull<DayOfWeek>(arr.getString(it)) }.toSet()
        } ?: emptySet()
        return FocusWindow(
            id = o.getString("id"),
            label = o.optString("label", "Focus"),
            days = days,
            startMinute = o.getInt("start"),
            endMinute = o.getInt("end"),
            enabled = o.optBoolean("enabled", true),
        )
    }

    private inline fun <reified E : Enum<E>> enumOrNull(name: String?): E? =
        enumValues<E>().firstOrNull { it.name == name }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, fallback: E): E = enumOrNull<E>(name) ?: fallback
}
