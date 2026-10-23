package quiet.core.settings

import java.time.DayOfWeek

/** What happens when you land on a surface. */
enum class Mode {
    /** Nothing. Time is still tracked. */
    ALLOW,

    /** A short breathing pause, then you choose to continue for a few minutes or leave. */
    PAUSE,

    /** Allowed until the shared daily short-form allowance runs out, then blocked. */
    LIMIT,

    /** Covered. Unlocks are possible unless strict mode or focus says otherwise. */
    BLOCK,
}

/** Where a single video was opened from. Each origin has its own swipe allowance. */
enum class Origin(val label: String) {
    MESSAGES("From a chat"),
    PROFILE("From a profile"),
    FEED("From your feed"),
    EXPLORE("From Explore or Search"),
    STORIES("From stories"),
    OTHER("From anywhere else"),
}

enum class CoverStyle {
    /** Cover the content but leave the app's own tab bar usable. */
    ABOVE_TABS,

    /** Cover the whole screen. */
    FULL_SCREEN,
}

enum class Accent { MINT, VIOLET, AMBER, ROSE, SKY }

const val UNLIMITED = -1

data class AppSettings(
    val enabled: Boolean = true,
    /** Surface id to mode. Surfaces not listed fall back to the app's defaults, then ALLOW. */
    val modes: Map<String, Mode> = emptyMap(),
    /** Extra swipes allowed after opening a single video, per origin. [UNLIMITED] means no cap. */
    val viewerSwipes: Map<Origin, Int> = emptyMap(),
    /** Whole-app daily limit in minutes, 0 for none. */
    val dailyLimitMinutes: Int = 0,
    /** Nav target to jump to when you land on a blocked feed, or null to just cover it. */
    val autoSkipTarget: String? = null,
)

data class FocusWindow(
    val id: String,
    val label: String,
    val days: Set<DayOfWeek>,
    val startMinute: Int,
    val endMinute: Int,
    val enabled: Boolean = true,
) {
    /** True when [minuteOfDay] on [day] falls inside the window. Windows may cross midnight. */
    fun contains(day: DayOfWeek, minuteOfDay: Int): Boolean {
        if (!enabled) return false
        if (startMinute == endMinute) return false
        return if (startMinute < endMinute) {
            day in days && minuteOfDay in startMinute until endMinute
        } else {
            // Crosses midnight: the late part belongs to [day], the early part to the day before.
            (day in days && minuteOfDay >= startMinute) || (day.minus(1) in days && minuteOfDay < endMinute)
        }
    }
}

data class TrackedApp(
    val packageName: String,
    val label: String,
    /** Daily limit in minutes, 0 to only track. */
    val dailyLimitMinutes: Int = 0,
)

data class Settings(
    val protectionOn: Boolean = true,
    val apps: Map<String, AppSettings> = emptyMap(),

    /** Shared daily allowance in minutes for every surface set to LIMIT. */
    val budgetMinutes: Int = 20,

    val unlocksEnabled: Boolean = true,
    val unlockMinutes: Int = 5,
    /** Size of a "watch N more" unlock; 0 turns that option off. */
    val unlockVideos: Int = 5,
    val unlockWaitSeconds: Int = 15,
    val unlocksPerDay: Int = 3,

    val pauseSeconds: Int = 10,
    val pauseMinutes: Int = 5,

    val focusWindows: List<FocusWindow> = emptyList(),
    val focusAllowsUnlocks: Boolean = false,
    /** Manual focus session end, epoch ms. */
    val focusUntil: Long = 0L,

    val pauseVideo: Boolean = true,
    val haptics: Boolean = true,
    val showTimerPill: Boolean = true,
    /** Gentle check-in every N minutes of continuous use of a tracked app, 0 for off. */
    val nudgeEveryMinutes: Int = 0,
    val coverStyle: CoverStyle = CoverStyle.ABOVE_TABS,
    /** Daily limits on whole apps never cover chats. */
    val keepMessagesOpen: Boolean = true,

    /** No unlocks, and loosening any setting makes you wait first. */
    val strictMode: Boolean = false,
    val strictDelaySeconds: Int = 30,

    val tracked: List<TrackedApp> = Defaults.trackedApps,

    val accent: Accent = Accent.MINT,
    val verboseLogging: Boolean = true,
    val onboarded: Boolean = false,
) {
    fun app(id: String): AppSettings = apps[id] ?: Defaults.appSettings(id)

    fun modeFor(app: String, surface: String): Mode =
        app(app).modes[surface] ?: Defaults.appSettings(app).modes[surface] ?: Mode.ALLOW

    fun viewerSwipesFor(app: String, origin: Origin): Int =
        app(app).viewerSwipes[origin] ?: Defaults.appSettings(app).viewerSwipes[origin] ?: 0

    fun withApp(id: String, transform: (AppSettings) -> AppSettings): Settings =
        copy(apps = apps + (id to transform(app(id))))

    fun canUnlock(focusActive: Boolean): Boolean =
        unlocksEnabled && !strictMode && (!focusActive || focusAllowsUnlocks)
}

object Defaults {
    val trackedApps = listOf(
        TrackedApp("com.instagram.android", "Instagram"),
        TrackedApp("com.zhiliaoapp.musically", "TikTok"),
        TrackedApp("com.ss.android.ugc.trill", "TikTok"),
        TrackedApp("com.google.android.youtube", "YouTube"),
        TrackedApp("com.snapchat.android", "Snapchat"),
        TrackedApp("com.twitter.android", "X"),
        TrackedApp("com.reddit.frontpage", "Reddit"),
        TrackedApp("com.facebook.katana", "Facebook"),
        TrackedApp("com.instagram.barcelona", "Threads"),
        TrackedApp("com.pinterest", "Pinterest"),
        TrackedApp("com.discord", "Discord"),
        TrackedApp("tv.twitch.android.app", "Twitch"),
        TrackedApp("com.bereal.ft", "BeReal"),
        TrackedApp("com.linkedin.android", "LinkedIn"),
    )

    private val friendlyViewer = mapOf(
        Origin.MESSAGES to 0,
        Origin.PROFILE to 10,
        Origin.FEED to 0,
        Origin.EXPLORE to 0,
        Origin.STORIES to 0,
        Origin.OTHER to 0,
    )

    fun appSettings(app: String): AppSettings = when (app) {
        "instagram" -> AppSettings(
            enabled = true,
            modes = mapOf(
                "reels" to Mode.BLOCK,
                "home" to Mode.ALLOW,
                "explore" to Mode.ALLOW,
                "stories" to Mode.ALLOW,
            ),
            viewerSwipes = friendlyViewer,
            autoSkipTarget = "messages",
        )
        "tiktok" -> AppSettings(
            enabled = true,
            modes = mapOf(
                "for_you" to Mode.BLOCK,
                "following" to Mode.BLOCK,
                "explore" to Mode.BLOCK,
                "live" to Mode.BLOCK,
                "friends" to Mode.ALLOW,
                "shop" to Mode.BLOCK,
            ),
            viewerSwipes = friendlyViewer,
            autoSkipTarget = "inbox",
        )
        "youtube" -> AppSettings(
            enabled = false,
            modes = mapOf("shorts" to Mode.BLOCK),
            viewerSwipes = friendlyViewer + (Origin.FEED to 0),
        )
        else -> AppSettings(enabled = true)
    }

    /** Ready-made starting points offered during onboarding. */
    fun preset(name: Preset, base: Settings = Settings()): Settings = when (name) {
        Preset.GENTLE -> base.copy(
            apps = mapOf(
                "instagram" to appSettings("instagram").copy(modes = appSettings("instagram").modes + ("reels" to Mode.LIMIT)),
                "tiktok" to appSettings("tiktok").copy(
                    modes = appSettings("tiktok").modes + ("for_you" to Mode.LIMIT) + ("following" to Mode.LIMIT),
                    autoSkipTarget = null,
                ),
                "youtube" to appSettings("youtube"),
            ),
            budgetMinutes = 30,
            unlocksPerDay = 5,
            strictMode = false,
        )
        Preset.BALANCED -> base.copy(
            apps = mapOf(
                "instagram" to appSettings("instagram"),
                "tiktok" to appSettings("tiktok"),
                "youtube" to appSettings("youtube"),
            ),
            budgetMinutes = 20,
            unlocksPerDay = 3,
            strictMode = false,
        )
        Preset.STRICT -> base.copy(
            apps = mapOf(
                "instagram" to appSettings("instagram").copy(
                    modes = appSettings("instagram").modes + ("explore" to Mode.PAUSE),
                ),
                "tiktok" to appSettings("tiktok"),
                "youtube" to appSettings("youtube").copy(enabled = true),
            ),
            budgetMinutes = 10,
            unlocksPerDay = 1,
            unlockWaitSeconds = 30,
            strictMode = true,
        )
    }
}

enum class Preset(val title: String, val blurb: String) {
    GENTLE("Gentle", "Reels and For You get a shared 30 minute daily allowance. Five unlocks a day."),
    BALANCED("Balanced", "Reels and For You are blocked. Friends' videos still open. Three unlocks a day."),
    STRICT("Strict", "Everything short-form is blocked, YouTube Shorts too. No unlocks, and loosening settings takes a wait."),
}
