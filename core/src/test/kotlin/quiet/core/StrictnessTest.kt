package quiet.core

import org.junit.Test
import quiet.core.settings.FocusWindow
import quiet.core.settings.Mode
import quiet.core.settings.Origin
import quiet.core.settings.Settings
import quiet.core.settings.Strictness
import quiet.core.settings.UNLIMITED
import java.time.DayOfWeek
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StrictnessTest {
    private val base = Settings(focusWindows = listOf(FocusWindow("a", "Work", setOf(DayOfWeek.MONDAY), 540, 1020)))

    @Test fun `tightening is never loosening`() {
        assertFalse(Strictness.isLoosening(base, base.copy(budgetMinutes = 10)))
        assertFalse(Strictness.isLoosening(base, base.withApp("instagram") { it.copy(modes = it.modes + ("home" to Mode.PAUSE)) }))
        assertFalse(Strictness.isLoosening(base, base.copy(strictMode = true)))
        assertFalse(Strictness.isLoosening(base, base.withApp("instagram") { it.copy(dailyLimitMinutes = 30) }))
    }

    @Test fun `loosening changes are caught`() {
        assertTrue(Strictness.isLoosening(base, base.copy(protectionOn = false)))
        assertTrue(Strictness.isLoosening(base, base.copy(budgetMinutes = 60)))
        assertTrue(Strictness.isLoosening(base, base.withApp("instagram") { it.copy(modes = it.modes + ("reels" to Mode.LIMIT)) }))
        assertTrue(Strictness.isLoosening(base, base.withApp("tiktok") { it.copy(enabled = false) }))
        assertTrue(Strictness.isLoosening(base, base.withApp("instagram") { it.copy(viewerSwipes = it.viewerSwipes + (Origin.MESSAGES to UNLIMITED)) }))
        assertTrue(Strictness.isLoosening(base, base.copy(focusWindows = emptyList())))
        val limited = base.withApp("instagram") { it.copy(dailyLimitMinutes = 30) }
        assertTrue(Strictness.isLoosening(limited, limited.withApp("instagram") { it.copy(dailyLimitMinutes = 0) }))
    }
}
