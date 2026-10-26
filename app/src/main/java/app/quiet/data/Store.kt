package app.quiet.data

import android.content.Context
import android.util.Log
import app.quiet.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import quiet.core.engine.CoverReason
import quiet.core.engine.EngineEvent
import quiet.core.engine.EventType
import quiet.core.rules.AppRules
import quiet.core.settings.Settings
import quiet.core.settings.SettingsCodec
import quiet.core.stats.DayStats
import quiet.core.stats.StatsCodec
import quiet.core.stats.plusCount
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** One line in the activity log on the Today screen. */
data class LogEntry(val at: Long, val app: String, val text: String)

data class RulesState(
    val apps: Map<String, AppRules> = emptyMap(),
    /** app id to "bundled" or "override". */
    val sources: Map<String, String> = emptyMap(),
    val errors: Map<String, String> = emptyMap(),
) {
    fun forPackage(pkg: String): AppRules? = apps.values.firstOrNull { pkg in it.packages }
}

/**
 * The app's single source of truth: settings, daily stats, activity log and detection rules.
 * Lives in the Application so the UI and the accessibility service (same process) share it.
 * Everything is small JSON in the app's files directory.
 */
class Store(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dir = context.filesDir
    private val settingsFile = File(dir, "settings.json")
    private val statsFile = File(dir, "stats.json")
    private val logFile = File(dir, "log.json")

    private val _settings = MutableStateFlow(SettingsCodec.decodeOrDefault(settingsFile.readTextOrNull()))
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private val _history = MutableStateFlow(
        statsFile.readTextOrNull()?.let { runCatching { StatsCodec.decode(it) }.getOrNull() } ?: emptyMap(),
    )
    val history: StateFlow<Map<String, DayStats>> = _history.asStateFlow()

    private val _log = MutableStateFlow(readLog())
    val log: StateFlow<List<LogEntry>> = _log.asStateFlow()

    private val _rules = MutableStateFlow(RulesState())
    val rules: StateFlow<RulesState> = _rules.asStateFlow()

    private var statsSaveJob: Job? = null

    init {
        reloadRules()
    }

    fun updateSettings(transform: (Settings) -> Settings) {
        val next = _settings.updateAndGet(transform)
        scope.launch { writeAtomic(settingsFile, SettingsCodec.encode(next)) }
    }

    fun replaceSettings(s: Settings) = updateSettings { s }

    fun today(now: Long = System.currentTimeMillis()): DayStats {
        val key = dayKey(now)
        return _history.value[key] ?: DayStats(key)
    }

    fun mutateToday(now: Long = System.currentTimeMillis(), f: (DayStats) -> DayStats) {
        val key = dayKey(now)
        _history.update { h ->
            val updated = f(h[key] ?: DayStats(key))
            prune(h + (key to updated), now)
        }
        scheduleStatsSave()
    }

    /** Records what the engine reported, into today's numbers and the activity log. */
    fun record(events: List<EngineEvent>, appName: (String) -> String) {
        if (events.isEmpty()) return
        val now = System.currentTimeMillis()
        mutateToday(now) { d ->
            events.fold(d) { acc, e ->
                when (e.type) {
                    EventType.VIDEO -> acc.copy(videos = acc.videos.plusCount(e.app))
                    EventType.AUTO_SKIP -> acc.copy(skips = acc.skips + 1)
                    EventType.COVER_SHOWN -> when (e.reason) {
                        CoverReason.SWIPE_LIMIT -> acc.copy(swipesStopped = acc.swipesStopped + 1, blocks = acc.blocks + 1)
                        CoverReason.PAUSE -> acc
                        else -> acc.copy(blocks = acc.blocks + 1)
                    }
                }
            }
        }
        // A skip shows the cover for a moment too: log it once, as the skip.
        val skipped = events.any { it.type == EventType.AUTO_SKIP }
        for (e in events) {
            if (skipped && e.type == EventType.COVER_SHOWN) continue
            val text = when (e.type) {
                EventType.VIDEO -> null
                EventType.AUTO_SKIP -> "Skipped the feed"
                EventType.COVER_SHOWN -> when (e.reason) {
                    CoverReason.BLOCKED -> "Blocked a feed"
                    CoverReason.BUDGET_DONE -> "Allowance used up"
                    CoverReason.FOCUS -> "Blocked during focus"
                    CoverReason.SWIPE_LIMIT -> "Stopped a swipe past a single video"
                    CoverReason.APP_LIMIT -> "Daily limit reached"
                    CoverReason.PAUSE -> "Paused before a feed"
                    null -> null
                }
            } ?: continue
            addLog(appName(e.app), text)
        }
    }

    fun addLog(app: String, text: String) {
        val entry = LogEntry(System.currentTimeMillis(), app, text)
        _log.update { (listOf(entry) + it).take(LOG_LIMIT) }
        scope.launch { writeLog(_log.value) }
    }

    fun clearStats() {
        _history.value = emptyMap()
        _log.value = emptyList()
        scope.launch {
            statsFile.delete()
            logFile.delete()
        }
    }

    fun flushNow() {
        val snapshot = _history.value
        scope.launch { writeAtomic(statsFile, StatsCodec.encode(snapshot)) }
    }

    private fun scheduleStatsSave() {
        if (statsSaveJob?.isActive == true) return
        statsSaveJob = scope.launch {
            delay(STATS_SAVE_DELAY_MS)
            writeAtomic(statsFile, StatsCodec.encode(_history.value))
        }
    }

    // ---- Rules ----

    /**
     * Folder on shared storage where rule overrides can be dropped (adb push works without root).
     * Debug builds only, so a Play build always runs the rules it shipped with.
     */
    fun rulesOverrideDir(): File? = if (BuildConfig.DEBUG) context.getExternalFilesDir("rules") else null

    /**
     * Where Inspector screen dumps go. Dumps include text from other apps' screens, so they are debug
     * builds only: a Play build never writes screen content to storage, as its accessibility disclosure says.
     */
    fun dumpsDir(): File? = if (BuildConfig.DEBUG) context.getExternalFilesDir("dumps") else null

    fun reloadRules() {
        val apps = mutableMapOf<String, AppRules>()
        val sources = mutableMapOf<String, String>()
        val errors = mutableMapOf<String, String>()
        for (name in BUNDLED_RULES) {
            val bundled = runCatching {
                quiet.core.rules.RuleParser.parse(javaClass.classLoader!!.getResource("rules/$name.json")!!.readText())
            }
            bundled.onSuccess {
                apps[it.app] = it
                sources[it.app] = "bundled v${it.version}"
            }.onFailure { errors[name] = "bundled: ${it.message}" }

            val override = rulesOverrideDir()?.let { File(it, "$name.json") }?.takeIf { it.isFile }
            if (override != null) {
                runCatching { quiet.core.rules.RuleParser.parse(override.readText()) }
                    .onSuccess {
                        apps[it.app] = it
                        sources[it.app] = "override v${it.version}"
                    }
                    .onFailure { errors[name] = "override ignored: ${it.message}" }
            }
        }
        _rules.value = RulesState(apps, sources, errors)
        Log.i(TAG, "rules loaded: $sources errors=$errors")
    }

    // ---- Export ----

    fun exportJson(): String = JSONObject().apply {
        put("settings", JSONObject(SettingsCodec.encode(_settings.value)))
        put("stats", JSONObject(StatsCodec.encode(_history.value)))
    }.toString(2)

    // ---- helpers ----

    private fun prune(h: Map<String, DayStats>, now: Long): Map<String, DayStats> {
        if (h.size <= KEEP_DAYS) return h
        val cutoff = LocalDate.parse(dayKey(now)).minusDays(KEEP_DAYS.toLong()).toString()
        return h.filterKeys { it >= cutoff }
    }

    private fun readLog(): List<LogEntry> = runCatching {
        val arr = JSONArray(logFile.readText())
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            LogEntry(o.getLong("at"), o.getString("app"), o.getString("text"))
        }
    }.getOrElse { emptyList() }

    private fun writeLog(entries: List<LogEntry>) {
        val arr = JSONArray()
        entries.forEach { arr.put(JSONObject().put("at", it.at).put("app", it.app).put("text", it.text)) }
        writeAtomic(logFile, arr.toString())
    }

    private fun writeAtomic(file: File, text: String) {
        try {
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(text)
            if (!tmp.renameTo(file)) {
                file.writeText(text)
                tmp.delete()
            }
        } catch (e: Exception) {
            Log.w(TAG, "write ${file.name} failed", e)
        }
    }

    private fun File.readTextOrNull(): String? = if (isFile) runCatching { readText() }.getOrNull() else null

    companion object {
        const val TAG = "Quiet"
        private const val LOG_LIMIT = 200
        private const val KEEP_DAYS = 120
        private const val STATS_SAVE_DELAY_MS = 15_000L
        val BUNDLED_RULES = listOf("instagram", "tiktok", "youtube")

        fun dayKey(now: Long): String = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate().toString()
    }
}
