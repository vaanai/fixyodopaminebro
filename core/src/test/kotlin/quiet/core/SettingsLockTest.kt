package quiet.core

import org.junit.Test
import quiet.core.settings.Settings
import quiet.core.settings.SettingsLock
import quiet.core.settings.SettingsLock.State
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsLockTest {
    private val now = 1_800_000_000_000L
    private val locked = Settings(lockSeconds = 120)
    private val looser = { s: Settings -> s.copy(budgetMinutes = s.budgetMinutes + 10) }
    private val tighter = { s: Settings -> s.copy(budgetMinutes = 5) }

    @Test fun `no lock lets everything through`() {
        val s = Settings()
        assertEquals(State.Off, SettingsLock.state(s, now))
        assertTrue(SettingsLock.allows(s, looser(s), now))
    }

    @Test fun `a lock holds loosening but never tightening`() {
        assertEquals(State.Locked, SettingsLock.state(locked, now))
        assertFalse(SettingsLock.allows(locked, looser(locked), now))
        assertTrue(SettingsLock.allows(locked, tighter(locked), now))
        // Making the lock longer is tightening; shorter or off is loosening.
        assertTrue(SettingsLock.allows(locked, locked.copy(lockSeconds = 300), now))
        assertFalse(SettingsLock.allows(locked, locked.copy(lockSeconds = 0), now))
    }

    @Test fun `the wait opens settings for a while, then locks again`() {
        val waiting = SettingsLock.startWait(locked, now)
        assertEquals(State.Waiting(now + 120_000), SettingsLock.state(waiting, now))
        assertFalse(SettingsLock.allows(waiting, looser(waiting), now + 119_000))

        val opened = now + 120_000
        assertEquals(State.Open(opened + SettingsLock.OPEN_MS), SettingsLock.state(waiting, opened))
        assertTrue(SettingsLock.allows(waiting, looser(waiting), opened + 60_000))

        assertEquals(State.Locked, SettingsLock.state(waiting, opened + SettingsLock.OPEN_MS))
        assertFalse(SettingsLock.allows(waiting, looser(waiting), opened + SettingsLock.OPEN_MS))
    }

    @Test fun `starting again while waiting keeps the original time`() {
        val waiting = SettingsLock.startWait(locked, now)
        assertEquals(waiting, SettingsLock.startWait(waiting, now + 60_000))
    }

    @Test fun `relock ends an open window`() {
        val open = SettingsLock.startWait(locked, now - 130_000)
        assertTrue(SettingsLock.state(open, now) is State.Open)
        assertEquals(State.Locked, SettingsLock.state(SettingsLock.relock(open), now))
    }

    @Test fun `a clock that went backwards doesn't leave you waiting forever`() {
        val waiting = SettingsLock.startWait(locked, now + 86_400_000)
        assertEquals(State.Locked, SettingsLock.state(waiting, now))
    }
}
