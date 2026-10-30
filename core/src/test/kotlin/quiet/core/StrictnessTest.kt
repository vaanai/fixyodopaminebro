package quiet.core

import org.junit.Test
import quiet.core.settings.FocusWindow
import quiet.core.settings.Mode
import quiet.core.settings.Origin
import quiet.core.settings.Settings
import quiet.core.settings.Strictness
import quiet.core.settings.UNLIMITED
import quiet.core.settings.UnlockKind
import java.time.DayOfWeek
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StrictnessTest {
    private val base = Settings(focusWindows = listOf(FocusWindow("a", "Work", setOf(DayOfWeek.MONDAY), 540, 1020)))

    @Test fun `tightening is never loosening`() {
        assertFalse(Strictness.isLoosening(base, base.copy(budgetMinutes = 10)))
        assertFalse(Strictness.isLoosening(base, base.withApp("instagram") { it.copy(modes = it.modes + ("home" to Mode.PAUSE)) }))
        assertFalse(Strictness.isLoosening(base, base.copy(lockSeconds = 120)))
        assertFalse(Strictness.isLoosening(base.copy(unlockKind = UnlockKind.CHOOSE), base.copy(unlockKind = UnlockKind.VIDEOS)))
        assertFalse(Strictness.isLoosening(base, base.withApp("instagram") { it.copy(modes = it.modes + ("stories" to Mode.BLOCK)) }))
        // Bigger "watch N more" unlocks don't matter while unlocks give minutes.
        val minutes = base.copy(unlockKind = UnlockKind.MINUTES)
        assertFalse(Strictness.isLoosening(minutes, minutes.copy(unlockVideos = 20)))
        assertFalse(Strictness.isLoosening(base, base.withApp("instagram") { it.copy(dailyLimitMinutes = 30) }))
        assertFalse(Strictness.isLoosening(base, base.copy(adSkipsWait = false)))
    }

    @Test fun `longer or extra focus time is not loosening, shorter is`() {
        val w = base.focusWindows.single()
        assertFalse(Strictness.isLoosening(base, base.copy(focusWindows = listOf(w.copy(startMinute = 480)))))
        assertFalse(Strictness.isLoosening(base, base.copy(focusWindows = listOf(w.copy(days = setOf(DayOfWeek.MONDAY, DayOfWeek.SATURDAY))))))
        assertFalse(Strictness.isLoosening(base, base.copy(focusWindows = listOf(w.copy(label = "Deep work")))))
        assertFalse(Strictness.isLoosening(base, base.copy(focusWindows = listOf(w, FocusWindow("b", "Night", setOf(DayOfWeek.FRIDAY), 1380, 420)))))
        assertTrue(Strictness.isLoosening(base, base.copy(focusWindows = listOf(w.copy(endMinute = 960)))))
        assertTrue(Strictness.isLoosening(base, base.copy(focusWindows = listOf(w.copy(days = setOf(DayOfWeek.TUESDAY))))))
        assertTrue(Strictness.isLoosening(base, base.copy(focusWindows = listOf(w.copy(enabled = false)))))
    }

    @Test fun `letting video play under the cover is loosening`() {
        assertTrue(Strictness.isLoosening(base, base.copy(pauseVideo = false)))
        assertFalse(Strictness.isLoosening(base.copy(pauseVideo = false), base))
    }

    @Test fun `turning on the ad that skips the wait is loosening`() {
        val noAd = base.copy(adSkipsWait = false)
        assertTrue(Strictness.isLoosening(noAd, noAd.copy(adSkipsWait = true)))
    }

    @Test fun `loosening changes are caught`() {
        assertTrue(Strictness.isLoosening(base.copy(linksPlay = false), base))
        assertFalse(Strictness.isLoosening(base, base.copy(linksPlay = false)))
        assertTrue(Strictness.isLoosening(base.copy(keepMessagesOpen = false), base))
        assertTrue(Strictness.isLoosening(base, base.copy(protectionOn = false)))
        assertTrue(Strictness.isLoosening(base, base.copy(budgetMinutes = 60)))
        assertTrue(Strictness.isLoosening(base, base.withApp("instagram") { it.copy(modes = it.modes + ("reels" to Mode.LIMIT)) }))
        assertTrue(Strictness.isLoosening(base, base.withApp("tiktok") { it.copy(enabled = false) }))
        assertTrue(Strictness.isLoosening(base, base.withApp("instagram") { it.copy(viewerSwipes = it.viewerSwipes + (Origin.MESSAGES to UNLIMITED)) }))
        assertTrue(Strictness.isLoosening(base, base.copy(focusWindows = emptyList())))
        assertTrue(Strictness.isLoosening(base.copy(lockSeconds = 300), base.copy(lockSeconds = 60)))
        assertTrue(Strictness.isLoosening(base, base.copy(unlockKind = UnlockKind.MINUTES)))
        assertTrue(Strictness.isLoosening(base, base.copy(unlockKind = UnlockKind.CHOOSE)))
        assertTrue(Strictness.isLoosening(base, base.copy(unlockVideos = 10)))
        assertTrue(Strictness.isLoosening(base, base.copy(unlockMinutes = 10)))
        val noStories = base.withApp("instagram") { it.copy(modes = it.modes + ("stories" to Mode.BLOCK)) }
        assertTrue(Strictness.isLoosening(noStories, base))
        val limited = base.withApp("instagram") { it.copy(dailyLimitMinutes = 30) }
        assertTrue(Strictness.isLoosening(limited, limited.withApp("instagram") { it.copy(dailyLimitMinutes = 0) }))
    }
}
