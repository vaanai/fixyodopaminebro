package quiet.core.settings

/** Decides whether a settings change makes Quiet easier to get past. Strict mode makes those wait. */
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
        if (old.strictMode && !new.strictMode) return true
        if (new.strictDelaySeconds < old.strictDelaySeconds) return true
        if (new.budgetMinutes > old.budgetMinutes) return true
        if (!old.unlocksEnabled && new.unlocksEnabled) return true
        if (new.unlockMinutes > old.unlockMinutes) return true
        if (new.unlockVideos > old.unlockVideos) return true
        if (new.unlockWaitSeconds < old.unlockWaitSeconds) return true
        if (new.unlocksPerDay > old.unlocksPerDay) return true
        if (new.pauseSeconds < old.pauseSeconds) return true
        if (new.pauseMinutes > old.pauseMinutes) return true
        if (!old.focusAllowsUnlocks && new.focusAllowsUnlocks) return true
        if (new.focusUntil < old.focusUntil) return true
        if (old.focusWindows.any { w -> w.enabled && w !in new.focusWindows }) return true
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
}
