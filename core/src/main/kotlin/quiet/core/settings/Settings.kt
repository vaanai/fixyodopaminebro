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

    /** Covered. Unlocks are possible unless they are turned off or focus says otherwise. */
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

/** What one unlock from the block screen gives you. */
enum class UnlockKind {
    /** "Watch N more": the next N swipes are open, then it locks again. */
    VIDEOS,

    /** "N minutes": the app's feeds are open for a while. */
    MINUTES,

    /** Offer both and pick each time. */
    CHOOSE;

    /**
     * The unlocks to offer. Whole-app limits and screens that aren't videos (Explore, LIVE, a feed of
     * posts) can only unlock by time: videos can't be counted there.
     */
    fun offers(appLimit: Boolean = false): List<UnlockKind> = when {
        appLimit -> listOf(MINUTES)
        this == CHOOSE -> listOf(VIDEOS, MINUTES)
        else -> listOf(this)
    }
}

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
    val unlockKind: UnlockKind = UnlockKind.VIDEOS,
    /** Size of a "watch N more" unlock. */
    val unlockVideos: Int = 5,
    /** Length of an "N minutes" unlock. Daily app limits always unlock this way. */
    val unlockMinutes: Int = 5,
    /** The free wait before an unlock. Long enough that one short ad is the quicker way through. */
    val unlockWaitSeconds: Int = 60,
    /** During the wait you can watch one short rewarded ad instead. The wait always works too. */
    val adSkipsWait: Boolean = true,
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
    /**
     * A video link opened from another app (a TikTok someone texted you) plays, even when the app opens it
     * inside a blocked feed. Swiping on is limited like a video sent in a chat.
     */
    val linksPlay: Boolean = true,

    /** The settings lock: making Quiet less strict waits this long first. 0 is no lock. See [SettingsLock]. */
    val lockSeconds: Int = 0,
    /** When the current lock wait ends (epoch ms); settings then stay open for a while. 0 when locked. */
    val lockOpensAt: Long = 0L,
    /** The Today screen has offered the settings lock to someone who set Quiet up before it existed. */
    val lockPromptSeen: Boolean = false,

    val tracked: List<TrackedApp> = Defaults.trackedApps,

    val accent: Accent = Accent.MINT,
    val verboseLogging: Boolean = false,
    /** The user agreed to the accessibility disclosure. */
    val disclosureAccepted: Boolean = false,
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
        unlocksEnabled && (!focusActive || focusAllowsUnlocks)
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

    /**
     * Ready-made starting points offered during onboarding and in Settings. Feed modes, unlocks and the
     * allowance come from the preset; daily app limits and apps the preset doesn't cover stay as they were.
     */
    fun preset(name: Preset, base: Settings = Settings()): Settings {
        val picked = presetOnly(name, base)
        val apps = base.apps.toMutableMap()
        for ((id, a) in picked.apps) apps[id] = a.copy(dailyLimitMinutes = base.apps[id]?.dailyLimitMinutes ?: a.dailyLimitMinutes)
        return picked.copy(apps = apps)
    }

    private fun presetOnly(name: Preset, base: Settings): Settings = when (name) {
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
            unlocksEnabled = true,
            unlocksPerDay = 5,
            unlockWaitSeconds = 30,
        )
        Preset.BALANCED -> base.copy(
            apps = mapOf(
                "instagram" to appSettings("instagram"),
                "tiktok" to appSettings("tiktok"),
                "youtube" to appSettings("youtube"),
            ),
            budgetMinutes = 20,
            unlocksEnabled = true,
            unlocksPerDay = 3,
            unlockWaitSeconds = 60,
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
            unlocksEnabled = false,
            unlocksPerDay = 1,
            unlockWaitSeconds = 120,
        )
    }
}

enum class Preset(val title: String, val blurb: String) {
    GENTLE("Gentle", "Reels and For You get a shared 30 minute daily allowance. Five unlocks a day."),
    BALANCED("Balanced", "Reels and For You are blocked. Friends' videos still open. Three unlocks a day."),
    STRICT("Strict", "Everything short-form is blocked, YouTube Shorts too. Explore gets a pause first. No unlocks."),
}
