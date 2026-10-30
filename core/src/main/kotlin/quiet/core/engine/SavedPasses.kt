package quiet.core.engine

import org.json.JSONObject

/**
 * Unlocks and pause passes still running, kept so they outlive a restart of Quiet's service (an app update,
 * or Android stopping it). Someone who waited for an unlock, or watched an ad for one, keeps it.
 */
data class SavedPasses(
    /** Minute unlocks ("app") and pause passes ("app.surface"), to when they end (epoch ms). */
    val passes: Map<String, Long> = emptyMap(),
    /** "Watch N more" unlocks: videos left and when they end. */
    val videos: Map<String, Pair<Int, Long>> = emptyMap(),
    /** Unlocks of apps Quiet only limits as a whole, kept by the service, by package. */
    val generic: Map<String, Long> = emptyMap(),
) {
    fun liveAt(now: Long) = SavedPasses(
        passes.filterValues { it > now },
        videos.filterValues { it.second > now && it.first >= 0 },
        generic.filterValues { it > now },
    )

    companion object {
        fun encode(p: SavedPasses): String = JSONObject().apply {
            put("passes", JSONObject().apply { p.passes.forEach { (k, v) -> put(k, v) } })
            put("videos", JSONObject().apply {
                p.videos.forEach { (k, v) -> put(k, JSONObject().put("left", v.first).put("until", v.second)) }
            })
            put("generic", JSONObject().apply { p.generic.forEach { (k, v) -> put(k, v) } })
        }.toString()

        /** Anything unreadable is dropped: losing an unlock is better than not starting. */
        fun decode(text: String?): SavedPasses {
            if (text.isNullOrBlank()) return SavedPasses()
            return runCatching {
                val o = JSONObject(text)
                fun longs(name: String): Map<String, Long> {
                    val m = o.optJSONObject(name) ?: return emptyMap()
                    return m.keys().asSequence().associateWith { m.getLong(it) }
                }
                val v = o.optJSONObject("videos")
                val videos = v?.keys()?.asSequence()?.associateWith { k ->
                    val e = v.getJSONObject(k)
                    e.getInt("left") to e.getLong("until")
                } ?: emptyMap()
                SavedPasses(longs("passes"), videos, longs("generic"))
            }.getOrDefault(SavedPasses())
        }
    }
}
