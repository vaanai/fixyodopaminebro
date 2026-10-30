package quiet.core.stats

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Everything Quiet counts for one calendar day. */
data class DayStats(
    val date: String,
    /** Foreground seconds per tracked package. */
    val appSeconds: Map<String, Int> = emptyMap(),
    /** Seconds per "app.surface", e.g. "instagram.reels". */
    val surfaceSeconds: Map<String, Int> = emptyMap(),
    /** Seconds spent in endless video feeds (Reels tab, For You, Shorts). */
    val feedSeconds: Int = 0,
    /** Seconds spent watching single videos opened from chats, profiles and so on. */
    val viewerSeconds: Int = 0,
    /** Seconds charged against the daily allowance (surfaces set to LIMIT). */
    val budgetSeconds: Int = 0,
    /** Short-form seconds per hour of day. */
    val hourShortForm: List<Int> = List(24) { 0 },
    /** Tracked-app seconds per hour of day. */
    val hourApps: List<Int> = List(24) { 0 },
    val opens: Map<String, Int> = emptyMap(),
    /** Videos seen per app (each swipe is one more). */
    val videos: Map<String, Int> = emptyMap(),
    /** Times a block screen appeared. */
    val blocks: Int = 0,
    /** Times Quiet jumped you off a feed automatically. */
    val skips: Int = 0,
    /** Times a swipe past a friend's video was stopped. */
    val swipesStopped: Int = 0,
    val unlocks: Int = 0,
    /** Rewarded ads watched to the end (each one skips one unlock's wait). */
    val adsWatched: Int = 0,
    /** Times you chose to leave at a pause screen. */
    val pausesLeft: Int = 0,
    /** Times you chose to continue at a pause screen. */
    val pausesContinued: Int = 0,
) {
    val shortFormSeconds: Int get() = feedSeconds + viewerSeconds
    val totalAppSeconds: Int get() = appSeconds.values.sum()

    fun appSecondsFor(packages: Collection<String>): Int = packages.sumOf { appSeconds[it] ?: 0 }

    fun secondsForApp(app: String): Int =
        surfaceSeconds.filterKeys { it.startsWith("$app.") }.values.sum()
}

fun <K> Map<K, Int>.plusCount(key: K, by: Int = 1): Map<K, Int> = this + (key to ((this[key] ?: 0) + by))

private fun List<Int>.plusAt(i: Int, by: Int): List<Int> = mapIndexed { idx, v -> if (idx == i) v + by else v }

/** Kinds of time a tick can be charged to. */
data class TickTarget(
    val packageName: String?,
    val tracked: Boolean,
    /** "app.surface" when inside a supported app and the surface is known. */
    val surfaceKey: String?,
    val isFeed: Boolean,
    val isViewer: Boolean,
    val chargesBudget: Boolean,
)

object StatsMath {
    fun addTime(day: DayStats, target: TickTarget, seconds: Int, hour: Int): DayStats {
        if (seconds <= 0 || target.packageName == null || !target.tracked) return day
        var d = day.copy(
            appSeconds = day.appSeconds.plusCount(target.packageName, seconds),
            hourApps = day.hourApps.plusAt(hour, seconds),
        )
        if (target.surfaceKey != null) d = d.copy(surfaceSeconds = d.surfaceSeconds.plusCount(target.surfaceKey, seconds))
        if (target.isFeed) d = d.copy(feedSeconds = d.feedSeconds + seconds)
        if (target.isViewer) d = d.copy(viewerSeconds = d.viewerSeconds + seconds)
        if (target.isFeed || target.isViewer) d = d.copy(hourShortForm = d.hourShortForm.plusAt(hour, seconds))
        if (target.chargesBudget) d = d.copy(budgetSeconds = d.budgetSeconds + seconds)
        return d
    }

    /**
     * Consecutive good days ending today (if today is still good) or yesterday. A good day keeps
     * endless-feed time within [budgetMinutes]. Days with no record count as good: the phone
     * may simply have been off, and a missing file shouldn't wipe out a streak.
     */
    fun streak(history: Map<String, DayStats>, today: LocalDate, budgetMinutes: Int, maxDays: Int = 365): Int {
        if (history.isEmpty()) return 0
        val earliest = history.keys.minOf { LocalDate.parse(it) }
        fun good(date: LocalDate): Boolean {
            val d = history[date.toString()] ?: return true
            return d.feedSeconds <= budgetMinutes * 60
        }
        var count = 0
        var cursor = if (good(today)) today else today.minusDays(1)
        while (count < maxDays && !cursor.isBefore(earliest) && good(cursor)) {
            count++
            cursor = cursor.minusDays(1)
        }
        return count
    }

    fun lastDays(history: Map<String, DayStats>, today: LocalDate, n: Int): List<DayStats> =
        (n - 1 downTo 0).map { back ->
            val date = today.minusDays(back.toLong()).toString()
            history[date] ?: DayStats(date)
        }
}

object StatsCodec {
    fun encode(history: Map<String, DayStats>): String = JSONObject().apply {
        put("version", 1)
        put("days", JSONArray().apply { history.values.sortedBy { it.date }.forEach { put(encodeDay(it)) } })
    }.toString()

    fun decode(json: String): Map<String, DayStats> {
        val arr = JSONObject(json).optJSONArray("days") ?: return emptyMap()
        return (0 until arr.length()).mapNotNull { i ->
            runCatching { decodeDay(arr.getJSONObject(i)) }.getOrNull()
        }.associateBy { it.date }
    }

    fun encodeDay(d: DayStats): JSONObject = JSONObject().apply {
        put("date", d.date)
        put("appSeconds", mapJson(d.appSeconds))
        put("surfaceSeconds", mapJson(d.surfaceSeconds))
        put("feedSeconds", d.feedSeconds)
        put("viewerSeconds", d.viewerSeconds)
        put("budgetSeconds", d.budgetSeconds)
        put("hourShortForm", JSONArray(d.hourShortForm))
        put("hourApps", JSONArray(d.hourApps))
        put("opens", mapJson(d.opens))
        put("videos", mapJson(d.videos))
        put("blocks", d.blocks)
        put("skips", d.skips)
        put("swipesStopped", d.swipesStopped)
        put("unlocks", d.unlocks)
        put("adsWatched", d.adsWatched)
        put("pausesLeft", d.pausesLeft)
        put("pausesContinued", d.pausesContinued)
    }

    fun decodeDay(o: JSONObject): DayStats = DayStats(
        date = o.getString("date"),
        appSeconds = jsonMap(o.optJSONObject("appSeconds")),
        surfaceSeconds = jsonMap(o.optJSONObject("surfaceSeconds")),
        feedSeconds = o.optInt("feedSeconds"),
        viewerSeconds = o.optInt("viewerSeconds"),
        budgetSeconds = o.optInt("budgetSeconds"),
        hourShortForm = hours(o.optJSONArray("hourShortForm")),
        hourApps = hours(o.optJSONArray("hourApps")),
        opens = jsonMap(o.optJSONObject("opens")),
        videos = jsonMap(o.optJSONObject("videos")),
        blocks = o.optInt("blocks"),
        skips = o.optInt("skips"),
        swipesStopped = o.optInt("swipesStopped"),
        unlocks = o.optInt("unlocks"),
        adsWatched = o.optInt("adsWatched"),
        pausesLeft = o.optInt("pausesLeft"),
        pausesContinued = o.optInt("pausesContinued"),
    )

    private fun mapJson(m: Map<String, Int>) = JSONObject().apply { m.forEach { (k, v) -> put(k, v) } }

    private fun jsonMap(o: JSONObject?): Map<String, Int> {
        if (o == null) return emptyMap()
        return o.keys().asSequence().associateWith { o.optInt(it) }
    }

    private fun hours(a: JSONArray?): List<Int> = List(24) { i -> a?.optInt(i) ?: 0 }
}
