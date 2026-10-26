package quiet.core.settings

import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek

/** Settings to and from JSON. Unknown or missing fields fall back to defaults so old files keep loading. */
object SettingsCodec {
    fun encode(s: Settings): String = JSONObject().apply {
        put("version", 2)
        put("protectionOn", s.protectionOn)
        put("apps", JSONObject().apply { s.apps.forEach { (k, v) -> put(k, encodeApp(v)) } })
        put("budgetMinutes", s.budgetMinutes)
        put("unlocksEnabled", s.unlocksEnabled)
        put("unlockKind", s.unlockKind.name)
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
        put("linksPlay", s.linksPlay)
        put("lockSeconds", s.lockSeconds)
        put("lockOpensAt", s.lockOpensAt)
        put("lockPromptSeen", s.lockPromptSeen)
        put("tracked", JSONArray().apply {
            s.tracked.forEach {
                put(JSONObject().put("package", it.packageName).put("label", it.label).put("limit", it.dailyLimitMinutes))
            }
        })
        put("accent", s.accent.name)
        put("verboseLogging", s.verboseLogging)
        put("disclosureAccepted", s.disclosureAccepted)
        put("onboarded", s.onboarded)
    }.toString(2)

    fun decode(json: String): Settings {
        val o = JSONObject(json)
        val d = Settings()
        val apps = mutableMapOf<String, AppSettings>()
        o.optJSONObject("apps")?.let { a -> for (k in a.keys()) apps[k] = decodeApp(k, a.getJSONObject(k)) }
        // Version 1 had strict mode (no unlocks, and a short wait before loosening) instead of the
        // settings lock, and "watch N more" set to 0 meant minutes only.
        val legacyStrict = !o.has("lockSeconds") && o.optBoolean("strictMode", false)
        val legacyVideos = o.optInt("unlockVideos", d.unlockVideos)
        val unlockKind = if (o.has("unlockKind")) {
            enumOr(o.optString("unlockKind"), d.unlockKind)
        } else if (o.has("unlockVideos")) {
            if (legacyVideos <= 0) UnlockKind.MINUTES else UnlockKind.CHOOSE
        } else {
            d.unlockKind
        }
        val lockSeconds = if (legacyStrict) {
            // Round the old wait up to whole minutes, at least one.
            ((o.optInt("strictDelaySeconds", 60) + 59) / 60 * 60).coerceAtLeast(60)
        } else {
            o.optInt("lockSeconds", d.lockSeconds)
        }
        return Settings(
            protectionOn = o.optBoolean("protectionOn", d.protectionOn),
            apps = apps,
            budgetMinutes = o.optInt("budgetMinutes", d.budgetMinutes),
            unlocksEnabled = o.optBoolean("unlocksEnabled", d.unlocksEnabled) && !legacyStrict,
            unlockKind = unlockKind,
            unlockMinutes = o.optInt("unlockMinutes", d.unlockMinutes),
            unlockVideos = if (legacyVideos <= 0) d.unlockVideos else legacyVideos,
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
            linksPlay = o.optBoolean("linksPlay", d.linksPlay),
            lockSeconds = lockSeconds,
            lockOpensAt = o.optLong("lockOpensAt", d.lockOpensAt),
            lockPromptSeen = o.optBoolean("lockPromptSeen", d.lockPromptSeen),
            tracked = o.optJSONArray("tracked")?.let { arr ->
                (0 until arr.length()).map { i ->
                    val t = arr.getJSONObject(i)
                    TrackedApp(t.getString("package"), t.optString("label", t.getString("package")), t.optInt("limit", 0))
                }
            } ?: d.tracked,
            accent = enumOr(o.optString("accent"), d.accent),
            verboseLogging = o.optBoolean("verboseLogging", d.verboseLogging),
            // Anyone set up before the disclosure screen existed already turned the service on themselves.
            disclosureAccepted = o.optBoolean("disclosureAccepted", o.optBoolean("onboarded", false)),
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
        // Before 2.2, "Block all of it" on Instagram Home was a blocked Home with the Stories row not kept.
        // Home now stays open at the top unless Stories is blocked, so that choice becomes blocking Stories.
        val keptStories = o.optJSONArray("keepVisible")?.let { arr -> (0 until arr.length()).any { arr.optString(it) == "home" } }
        if (id == "instagram" && keptStories == false && modes["home"] == Mode.BLOCK) modes["stories"] = Mode.BLOCK
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
