package quiet.core.engine

import quiet.core.detect.Reading
import quiet.core.rules.AppRules
import quiet.core.rules.NavTarget
import quiet.core.rules.SurfaceKind
import quiet.core.settings.Mode
import quiet.core.settings.Origin
import quiet.core.settings.Settings
import quiet.core.settings.UNLIMITED
import quiet.core.settings.UnlockKind
import quiet.core.stats.DayStats
import java.time.Instant
import java.time.ZoneId

/** The most recent scroll event seen inside the current app. */
data class ScrollInfo(
    val atMs: Long,
    /** True when the event came from the app's vertical video pager. */
    val fromPager: Boolean,
    /** Index of the first visible item reported by the pager, or -1. */
    val index: Int = -1,
)

data class EngineInput(
    val now: Long,
    val zone: ZoneId,
    val rules: AppRules,
    val reading: Reading,
    val settings: Settings,
    val today: DayStats,
    val scroll: ScrollInfo? = null,
)

enum class CoverReason {
    /** The surface is set to Block. */
    BLOCKED,

    /** The shared daily allowance is used up. */
    BUDGET_DONE,

    /** A focus window or focus session is on. */
    FOCUS,

    /** Swiped past the video you opened, beyond the allowance for where it came from. */
    SWIPE_LIMIT,

    /** The whole app hit its daily limit. */
    APP_LIMIT,

    /** Mindful pause before entering. */
    PAUSE,
}

data class Cover(
    val reason: CoverReason,
    val app: String,
    val appName: String,
    val surface: String?,
    val surfaceLabel: String,
    val navTargets: List<NavTarget>,
    val offerBack: Boolean,
    val canUnlock: Boolean,
    val unlockMinutes: Int,
    /** "Watch N more" unlock size. */
    val unlockVideos: Int,
    val unlockWaitSeconds: Int,
    val unlocksLeft: Int,
    val pauseSeconds: Int,
    val pauseMinutes: Int,
    /** The cover stops at this y (the app's tab bar), or covers the full screen when null. */
    val bottomPx: Int?,
    val origin: Origin? = null,
    val allowedSwipes: Int = 0,
    val unlockKind: UnlockKind = UnlockKind.MINUTES,
    /** The cover starts at this y, leaving what's above it usable (like the Stories row), or at the top when null. */
    val topPx: Int? = null,
    /** What stays visible above the cover ("Stories"), when [topPx] is set. */
    val keptName: String? = null,
) {
    /** The unlocks the block screen offers. */
    val unlockOffers: List<UnlockKind> get() = unlockKind.offers(appLimit = reason == CoverReason.APP_LIMIT)

    /** Two covers with the same key are the same screen; the overlay is kept rather than redrawn. */
    val key: String get() = "$reason/$app/$surface"
}

sealed interface Command {
    data class Navigate(val app: String, val target: String) : Command
    data object Back : Command
}

enum class EventType { VIDEO, COVER_SHOWN, AUTO_SKIP }

data class EngineEvent(val type: EventType, val app: String, val reason: CoverReason? = null)

data class Pill(val text: String, val urgent: Boolean)

data class Decision(
    val cover: Cover? = null,
    val command: Command? = null,
    val pill: Pill? = null,
    val events: List<EngineEvent> = emptyList(),
    /** "app.surface" of the current screen, for time tracking. */
    val surfaceKey: String? = null,
    val isFeed: Boolean = false,
    val isViewer: Boolean = false,
    val chargesBudget: Boolean = false,
) {
    companion object {
        val NONE = Decision()
    }
}

/** Live state the Inspector shows. */
data class EngineSnapshot(
    val app: String?,
    val surface: String?,
    val kind: SurfaceKind?,
    val origin: Origin?,
    val swipes: Int,
    val signature: String,
    val passes: Map<String, Long>,
    val coverKey: String?,
    /** Videos left on "watch N more" unlocks, per app. */
    val videosLeft: Map<String, Int> = emptyMap(),
)

/**
 * Decides what to do with each screen reading. Pure apart from its own memory, so every rule here is
 * covered by plain JVM tests.
 *
 * The core idea: endless feeds are policed by surface (Reels tab, For You), and single videos are
 * policed by swipes. Opening a reel a friend sent is always fine; swiping past it into the
 * algorithm's next pick is what gets stopped.
 */
class Engine {
    private data class VideoSession(
        val surface: String,
        val kind: SurfaceKind,
        val origin: Origin,
        val swipes: Int,
        val signature: String,
        val lastIndex: Int,
        val lastSwipeAt: Long,
    )

    private data class Landing(val surface: String, val skipAttempts: Int = 0, val lastSkipAt: Long = 0L)

    private var app: String? = null
    private var lastKnown: Reading? = null
    private var lastKnownAt = 0L
    private var lastDecision = Decision.NONE
    private var lastPlaceKind: SurfaceKind? = null
    private var lastPlaceAt = 0L
    private var session: VideoSession? = null
    private var landing: Landing? = null
    private var prevCoverKey: String? = null
    private val passes = mutableMapOf<String, Long>()
    private data class VideoPass(val remaining: Int, val until: Long)
    private val videoPasses = mutableMapOf<String, VideoPass>()
    private val lastSkipAt = mutableMapOf<String, Long>()
    private var enteredAt = 0L
    private var launchSettled = false
    private var guessing = false

    fun evaluate(input: EngineInput): Decision {
        val now = input.now
        if (app != input.reading.app) {
            resetApp()
            app = input.reading.app
            enteredAt = now
            launchSettled = false
        }
        val raw = guessAtLaunch(input.rules, input.reading, now) ?: input.reading
        guessing = raw !== input.reading

        // A partial read that already shows a chat is trusted: chats are never covered.
        if (raw.isUnknown || (raw.truncated && raw.kind != SurfaceKind.MESSAGES)) {
            // An unreadable screen after a skip means the skip went somewhere: a later landing on
            // the same feed is a return, not a retry.
            landing?.let { if (raw.isUnknown && it.skipAttempts > 0 && now - it.lastSkipAt > SKIP_SETTLE_MS) landing = null }
            // Screens are briefly unreadable mid-transition, and a partial read can match the wrong
            // surface. Hold the last decision for a moment so the cover doesn't flicker, then fail
            // open (unknown) or go with the partial read (truncated).
            if (lastKnown != null && now - lastKnownAt < UNKNOWN_HOLD_MS) {
                return lastDecision.copy(command = null, events = emptyList())
            }
            if (raw.isUnknown) {
                lastKnown = null
                session = null
                return remember(Decision.NONE)
            }
        }
        lastKnown = raw
        lastKnownAt = now

        val surface = raw.surface!!
        val kind = raw.kind!!
        val events = mutableListOf<EngineEvent>()
        val swipesBefore = session?.takeIf { it.surface == surface }?.swipes
        trackVideo(raw, surface, kind, input.scroll, now, events)
        val swiped = swipesBefore != null && (session?.swipes ?: 0) > swipesBefore
        var passEnded = false
        if (swiped) videoPasses[raw.app]?.let { p ->
            if (p.remaining <= 0) {
                videoPasses.remove(raw.app)
                passEnded = true
            } else {
                videoPasses[raw.app] = p.copy(remaining = p.remaining - 1)
            }
        }
        passes[raw.app]?.let { if (it <= now) { passes.remove(raw.app); passEnded = true } }
        videoPasses[raw.app]?.let { if (it.until <= now) { videoPasses.remove(raw.app); passEnded = true } }
        if (landing?.surface != surface) landing = Landing(surface)
        // An unlock running out while you watch shows the cover where you are instead of yanking
        // you elsewhere; skipping resumes on the next fresh landing.
        if (passEnded) landing = landing?.copy(skipAttempts = MAX_SKIP_ATTEMPTS)
        if (kind != SurfaceKind.VIDEO_VIEWER) {
            lastPlaceKind = kind
            lastPlaceAt = now
        }

        val base = Decision(
            surfaceKey = "${raw.app}.$surface",
            isFeed = kind == SurfaceKind.VIDEO_FEED,
            isViewer = kind == SurfaceKind.VIDEO_VIEWER,
        )
        val decided = decide(input, surface, kind, base)
        val withEvents = decided.copy(events = events + decided.events + coverEvents(decided.cover))
        return remember(withEvents)
    }

    private fun decide(input: EngineInput, surface: String, kind: SurfaceKind, base: Decision): Decision {
        val s = input.settings
        val rules = input.rules
        val appId = rules.app
        val appSet = s.app(appId)
        val now = input.now
        if (!s.protectionOn || !appSet.enabled) return base

        val focus = isFocusActive(s, now, input.zone)
        val videoPass = videoPasses[appId]?.takeIf { it.until > now }
        val unlockedUntil = passes[appId]?.takeIf { it > now } ?: videoPass?.until
        val unlocksLeft = (s.unlocksPerDay - input.today.unlocks).coerceAtLeast(0)
        val label = rules.surface(surface)?.label ?: surface

        fun cover(reason: CoverReason, offerBack: Boolean = false, fullScreen: Boolean = false): Cover {
            val canUnlock = reason != CoverReason.PAUSE && s.canUnlock(focus) && unlocksLeft > 0
            val keep = rules.surface(surface)?.keep
            val top = input.reading.keepBottom?.takeIf { !fullScreen && keep != null && s.keepsVisible(appId, surface) }
            return Cover(
                reason = reason,
                app = appId,
                appName = rules.displayName,
                surface = surface,
                surfaceLabel = label,
                navTargets = rules.nav,
                offerBack = offerBack,
                canUnlock = canUnlock,
                unlockMinutes = s.unlockMinutes,
                unlockVideos = s.unlockVideos,
                unlockWaitSeconds = s.unlockWaitSeconds,
                unlocksLeft = unlocksLeft,
                pauseSeconds = s.pauseSeconds,
                pauseMinutes = s.pauseMinutes,
                bottomPx = if (fullScreen || s.coverStyle == quiet.core.settings.CoverStyle.FULL_SCREEN) null else input.reading.tabBarTop,
                origin = session?.origin,
                allowedSwipes = session?.let { s.viewerSwipesFor(appId, it.origin) } ?: 0,
                unlockKind = s.unlockKind,
                topPx = top,
                keptName = keep?.name?.takeIf { top != null },
            )
        }

        fun passPill(until: Long, prefix: String): Pill? = when {
            !s.showTimerPill -> null
            videoPass != null && passes[appId]?.takeIf { it > now } == null ->
                Pill(if (videoPass.remaining == 0) "Last one" else "${videoPass.remaining} more", videoPass.remaining == 0)
            else -> Pill("$prefix · ${formatClock(until - now)}", until - now < 60_000)
        }

        // A whole-app daily limit trumps everything except chats (when chats are kept open).
        val limit = appSet.dailyLimitMinutes
        if (limit > 0 && unlockedUntil == null &&
            input.today.appSecondsFor(rules.packages) >= limit * 60 &&
            !(s.keepMessagesOpen && kind == SurfaceKind.MESSAGES)
        ) {
            return base.copy(cover = cover(CoverReason.APP_LIMIT, fullScreen = true))
        }

        if (kind == SurfaceKind.MESSAGES) return base

        if (kind == SurfaceKind.VIDEO_VIEWER) {
            val sess = session ?: return base
            val allowed = s.viewerSwipesFor(appId, sess.origin)
            if (allowed != UNLIMITED && sess.swipes > allowed && unlockedUntil == null) {
                return base.copy(cover = cover(CoverReason.SWIPE_LIMIT, offerBack = true, fullScreen = true))
            }
            return base.copy(pill = unlockedUntil?.let { passPill(it, "Unlocked") })
        }

        var mode = s.modeFor(appId, surface)
        var reason = CoverReason.BLOCKED
        if (focus && (mode != Mode.ALLOW || kind == SurfaceKind.VIDEO_FEED)) {
            mode = Mode.BLOCK
            reason = CoverReason.FOCUS
        }

        if (unlockedUntil != null && mode != Mode.ALLOW) {
            return base.copy(pill = passPill(unlockedUntil, "Unlocked"))
        }

        return when (mode) {
            Mode.ALLOW -> base
            Mode.PAUSE -> {
                val passUntil = passes["$appId.$surface"]?.takeIf { it > now }
                if (passUntil != null) base.copy(pill = passPill(passUntil, label))
                else base.copy(cover = cover(CoverReason.PAUSE))
            }
            Mode.LIMIT -> {
                val left = s.budgetMinutes * 60 - input.today.budgetSeconds
                if (left > 0) {
                    base.copy(
                        chargesBudget = true,
                        pill = if (s.showTimerPill) Pill("${formatClock(left * 1000L)} left today", left < 120) else null,
                    )
                } else {
                    blocked(input, kind, base, cover(CoverReason.BUDGET_DONE))
                }
            }
            Mode.BLOCK -> blocked(input, kind, base, cover(reason))
        }
    }

    /**
     * An app that has just come to the front can be too busy to read in full: TikTok's first reads at a
     * cold start stop inside the video pager, before the tabs, and look like a single video. While the
     * app is starting, such a read is taken to be the surface it opens on, so For You is covered at once
     * instead of seconds later. The first complete read, or a few seconds, ends the guessing.
     */
    private fun guessAtLaunch(rules: AppRules, r: Reading, now: Long): Reading? {
        val launch = rules.launch ?: return null
        if (launchSettled) return null
        if ((!r.isUnknown && !r.truncated) || now - enteredAt > LAUNCH_GUESS_MS) {
            launchSettled = true
            return null
        }
        if (!r.truncated || launch.signal !in r.signals) return null
        if (!r.isUnknown && r.kind != SurfaceKind.VIDEO_VIEWER) return null
        val info = rules.surface(launch.surface) ?: return null
        return r.copy(surface = info.id, kind = info.kind)
    }

    /** A blocked feed: show the cover and, on a fresh landing, try to jump somewhere useful. */
    private fun blocked(input: EngineInput, kind: SurfaceKind, base: Decision, cover: Cover): Decision {
        val target = input.settings.app(input.rules.app).autoSkipTarget
            ?.takeIf { input.rules.navTarget(it) != null }
        val land = landing
        // A guessed read may not have reached the tab to skip to yet: cover now, skip on a full read.
        if (target == null || kind != SurfaceKind.VIDEO_FEED || land == null || guessing) return base.copy(cover = cover)
        // Coming straight back after a skip means you want the options: show the cover instead.
        val key = "${input.rules.app}.${land.surface}"
        val lastSkip = lastSkipAt[key]
        if (land.skipAttempts == 0 && lastSkip != null && input.now - lastSkip < RETURN_SHOWS_COVER_MS) {
            // Stay on the cover for this landing, so a pending unlock wait isn't cut short by a skip.
            landing = land.copy(skipAttempts = MAX_SKIP_ATTEMPTS)
            return base.copy(cover = cover)
        }
        if (land.skipAttempts >= MAX_SKIP_ATTEMPTS || input.now - land.lastSkipAt < SKIP_RETRY_MS) {
            return base.copy(cover = cover)
        }
        landing = land.copy(skipAttempts = land.skipAttempts + 1, lastSkipAt = input.now)
        lastSkipAt[key] = input.now
        val events = if (land.skipAttempts == 0) listOf(EngineEvent(EventType.AUTO_SKIP, input.rules.app)) else emptyList()
        return base.copy(cover = cover, command = Command.Navigate(input.rules.app, target), events = events)
    }

    private fun trackVideo(
        r: Reading,
        surface: String,
        kind: SurfaceKind,
        scroll: ScrollInfo?,
        now: Long,
        events: MutableList<EngineEvent>,
    ) {
        if (kind != SurfaceKind.VIDEO_FEED && kind != SurfaceKind.VIDEO_VIEWER) {
            session = null
            return
        }
        val current = session
        val pagerIndex = scroll?.takeIf { it.fromPager && it.index >= 0 }?.index ?: -1
        if (current == null || current.surface != surface) {
            val origin = if (kind == SurfaceKind.VIDEO_VIEWER) originFor(now) else Origin.OTHER
            session = VideoSession(surface, kind, origin, 0, r.signature, pagerIndex, now)
            events += EngineEvent(EventType.VIDEO, r.app)
            return
        }
        if (current.swipes == 0 && now - current.lastSwipeAt < SESSION_SETTLE_MS) {
            // The open transition scrolls the pager and the first reads may be partial: take the
            // fingerprint as it settles instead of counting it as a swipe.
            session = current.copy(
                signature = r.signature.ifEmpty { current.signature },
                lastIndex = if (pagerIndex >= 0) pagerIndex else current.lastIndex,
            )
            return
        }
        val recentScroll = scroll != null && now - scroll.atMs <= SWIPE_SCROLL_WINDOW_MS && scroll.atMs >= current.lastSwipeAt
        val sinceLast = now - current.lastSwipeAt
        val indexMoved = pagerIndex >= 0 && current.lastIndex >= 0 && pagerIndex != current.lastIndex
        val sigMoved = r.signature.isNotEmpty() && current.signature.isNotEmpty() && r.signature != current.signature
        // The pager index is the reliable signal when there is one. The fingerprint also changes as
        // a new video settles (captions, counts loading), so on its own it needs a longer gap.
        val swiped = recentScroll && when {
            // Once the pager has reported where it is, only a move of the pager counts.
            current.lastIndex >= 0 -> indexMoved && sinceLast >= MIN_SWIPE_GAP_MS
            // First swipe that reports an index: the fingerprint decides, then the index takes over.
            pagerIndex >= 0 -> sigMoved && sinceLast >= MIN_SWIPE_GAP_MS
            else -> sigMoved && sinceLast >= SIGNATURE_SWIPE_GAP_MS
        }
        session = if (swiped) {
            events += EngineEvent(EventType.VIDEO, r.app)
            current.copy(
                swipes = current.swipes + 1,
                signature = r.signature.ifEmpty { current.signature },
                lastIndex = if (pagerIndex >= 0) pagerIndex else current.lastIndex,
                lastSwipeAt = now,
            )
        } else {
            current.copy(
                // Keep the fingerprint fresh (captions expand, overlays come and go) without counting.
                signature = if (recentScroll) current.signature else r.signature.ifEmpty { current.signature },
                lastIndex = if (current.lastIndex < 0 && pagerIndex >= 0) pagerIndex else current.lastIndex,
            )
        }
    }

    private fun originFor(now: Long): Origin {
        val kind = lastPlaceKind ?: return Origin.OTHER
        if (now - lastPlaceAt > ORIGIN_MEMORY_MS) return Origin.OTHER
        return when (kind) {
            SurfaceKind.MESSAGES -> Origin.MESSAGES
            SurfaceKind.PROFILE -> Origin.PROFILE
            SurfaceKind.FEED -> Origin.FEED
            SurfaceKind.EXPLORE -> Origin.EXPLORE
            SurfaceKind.STORIES -> Origin.STORIES
            else -> Origin.OTHER
        }
    }

    private fun coverEvents(cover: Cover?): List<EngineEvent> {
        val key = cover?.key
        val fresh = cover != null && key != prevCoverKey
        prevCoverKey = key
        return if (fresh) listOf(EngineEvent(EventType.COVER_SHOWN, cover!!.app, cover.reason)) else emptyList()
    }

    private fun remember(d: Decision): Decision {
        lastDecision = d
        if (d.cover == null) prevCoverKey = null
        return d
    }

    /** The user left the supported apps. */
    fun onLeftApp() {
        resetApp()
        app = null
    }

    private fun resetApp() {
        lastKnown = null
        lastDecision = Decision.NONE
        session = null
        landing = null
        prevCoverKey = null
        lastPlaceKind = null
    }

    /** "Unlock for N minutes": every short-form surface of the app is open until it runs out. */
    fun unlock(app: String, minutes: Int, now: Long) {
        passes[app] = now + minutes * 60_000L
        prevCoverKey = null
    }

    /** "Watch N more": the app's feeds stay open for [count] more swipes (or half an hour, whichever ends first). */
    fun unlockVideos(app: String, count: Int, now: Long) {
        videoPasses[app] = VideoPass(count, now + VIDEO_PASS_MAX_MS)
        prevCoverKey = null
    }

    /** "Continue" on a pause screen opens that one surface for a while. */
    fun continueAfterPause(app: String, surface: String, minutes: Int, now: Long) {
        passes["$app.$surface"] = now + minutes * 60_000L
    }

    /** Ends any unlock early ("I'm done"). */
    fun relock(app: String) {
        passes.keys.removeAll { it == app || it.startsWith("$app.") }
        videoPasses.remove(app)
    }

    fun unlockedUntil(app: String, now: Long): Long? =
        passes[app]?.takeIf { it > now } ?: videoPasses[app]?.takeIf { it.until > now }?.until

    fun videosLeft(app: String, now: Long): Int? = videoPasses[app]?.takeIf { it.until > now }?.remaining

    fun snapshot(): EngineSnapshot = EngineSnapshot(
        app = app,
        surface = lastKnown?.surface,
        kind = lastKnown?.kind,
        origin = session?.origin,
        swipes = session?.swipes ?: 0,
        signature = session?.signature ?: lastKnown?.signature.orEmpty(),
        passes = passes.toMap() + videoPasses.mapValues { it.value.until },
        coverKey = prevCoverKey,
        videosLeft = videoPasses.mapValues { it.value.remaining },
    )

    companion object {
        const val UNKNOWN_HOLD_MS = 1_500L
        /** How long after an app comes to the front a partial read can be taken for its launch surface. */
        const val LAUNCH_GUESS_MS = 10_000L
        const val ORIGIN_MEMORY_MS = 120_000L
        const val SWIPE_SCROLL_WINDOW_MS = 1_500L
        const val MIN_SWIPE_GAP_MS = 800L
        const val SESSION_SETTLE_MS = 800L
        const val SKIP_SETTLE_MS = 300L
        const val SIGNATURE_SWIPE_GAP_MS = 1_200L
        const val MAX_SKIP_ATTEMPTS = 2
        const val SKIP_RETRY_MS = 1_200L
        const val VIDEO_PASS_MAX_MS = 30 * 60_000L
        const val RETURN_SHOWS_COVER_MS = 20_000L

        fun isFocusActive(s: Settings, now: Long, zone: ZoneId): Boolean {
            if (s.focusUntil > now) return true
            if (s.focusWindows.isEmpty()) return false
            val t = Instant.ofEpochMilli(now).atZone(zone)
            val minute = t.hour * 60 + t.minute
            return s.focusWindows.any { it.contains(t.dayOfWeek, minute) }
        }

        fun formatClock(ms: Long): String {
            val total = (ms.coerceAtLeast(0) + 999) / 1000
            val m = total / 60
            val sec = total % 60
            return if (m >= 60) "${m / 60}h ${m % 60}m" else "%d:%02d".format(m, sec)
        }
    }
}
