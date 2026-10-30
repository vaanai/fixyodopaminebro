package quiet.core.settings

import java.time.DayOfWeek

/** Decides whether a settings change makes Quiet easier to get past. The [SettingsLock] makes those wait. */
object Strictness {
    private fun severity(m: Mode) = when (m) {
        Mode.ALLOW -> 0
        Mode.PAUSE -> 1
        Mode.LIMIT -> 2
        Mode.BLOCK -> 3
    }

    private fun swipes(n: Int) = if (n == UNLIMITED) Int.MAX_VALUE else n

    private fun limit(n: Int) = if (n <= 0) Int.MAX_VALUE else n

    fun isLoosening(old: Settings, new: Settings): Boolean {
        if (old.protectionOn && !new.protectionOn) return true
        if (new.lockSeconds < old.lockSeconds) return true
        if (new.budgetMinutes > old.budgetMinutes) return true
        if (!old.unlocksEnabled && new.unlocksEnabled) return true
        // A new kind of unlock is looser (5 minutes can be far more than 5 videos, and picking each
        // time offers both). More videos only matter if videos are on offer; minutes always are,
        // because daily app limits unlock by time.
        val oldOffers = old.unlockKind.offers()
        val newOffers = new.unlockKind.offers()
        if (newOffers.any { it !in oldOffers }) return true
        if (new.unlockMinutes > old.unlockMinutes) return true
        if (UnlockKind.VIDEOS in newOffers && new.unlockVideos > old.unlockVideos) return true
        if (new.unlockWaitSeconds < old.unlockWaitSeconds) return true
        if (!old.adSkipsWait && new.adSkipsWait) return true
        if (new.unlocksPerDay > old.unlocksPerDay) return true
        if (new.pauseSeconds < old.pauseSeconds) return true
        if (new.pauseMinutes > old.pauseMinutes) return true
        if (!old.focusAllowsUnlocks && new.focusAllowsUnlocks) return true
        if (!old.keepMessagesOpen && new.keepMessagesOpen) return true
        if (!old.linksPlay && new.linksPlay) return true
        if (new.focusUntil < old.focusUntil) return true
        if (old.pauseVideo && !new.pauseVideo) return true
        if (focusShrinks(old.focusWindows, new.focusWindows)) return true
        for (t in old.tracked) {
            val n = new.tracked.firstOrNull { it.packageName == t.packageName }
            if (limit(n?.dailyLimitMinutes ?: 0) > limit(t.dailyLimitMinutes)) return true
        }
        val appIds = old.apps.keys + new.apps.keys + listOf("instagram", "tiktok", "youtube")
        for (id in appIds) {
            val a = old.app(id)
            val b = new.app(id)
            if (a.enabled && !b.enabled) return true
            if (limit(b.dailyLimitMinutes) > limit(a.dailyLimitMinutes)) return true
            for (surface in a.modes.keys + b.modes.keys) {
                if (severity(new.modeFor(id, surface)) < severity(old.modeFor(id, surface))) return true
            }
            for (origin in Origin.entries) {
                if (swipes(new.viewerSwipesFor(id, origin)) > swipes(old.viewerSwipesFor(id, origin))) return true
            }
        }
        return false
    }

    /** True when some minute of the week that [old] covered isn't covered by [new] any more. */
    private fun focusShrinks(old: List<FocusWindow>, new: List<FocusWindow>): Boolean {
        val was = old.filter { it.enabled }
        if (was.isEmpty() || was.all { it in new }) return false
        val now = new.filter { it.enabled }
        for (day in DayOfWeek.entries) {
            for (minute in 0 until 24 * 60) {
                if (was.any { it.contains(day, minute) } && now.none { it.contains(day, minute) }) return true
            }
        }
        return false
    }
}
