package app.quiet.data.policy

import androidx.test.core.app.ApplicationProvider
import app.quiet.data.prefs.QuietPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import quiet.core.enforce.EnforcementAction
import quiet.core.enforce.EnforcementMemory
import quiet.core.enforce.EnforcementObservation
import quiet.core.enforce.Enforcer
import quiet.core.rules.Policy
import quiet.core.rules.SurfaceKind

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class EnforcementStateRepositoryTest {
    private lateinit var preferences: QuietPreferences
    private lateinit var repository: EnforcementStateRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        preferences = QuietPreferences(context)
        runBlocking { preferences.clearAll() }
        repository = EnforcementStateRepository(preferences)
    }

    @Test
    fun grantGatePassPersistsAndHolderReflectsPass() = runBlocking {
        val memory = EnforcementMemory()
        val now = 1_000_000L
        val updated = repository.grantGatePass("instagram.reels", 10, memory, now)
        val snap = preferences.readSnapshot()
        assertTrue(snap.passesJson.contains("instagram.reels"))
        val holderPass = PolicySnapshotHolder.get().passes.firstOrNull { it.first == "instagram.reels" }
        assertNotNull(holderPass)
        assertEquals(now + 10 * 60_000L, holderPass!!.second)

        val clock = object : quiet.core.enforce.Clock {
            override fun millis(): Long = now + 1
        }
        val action = Enforcer.decide(
            EnforcementObservation(
                appId = "instagram",
                surfaceId = "instagram.reels",
                kind = SurfaceKind.feed,
                policy = Policy.BLOCK,
                enforce = true,
                messagesFound = true,
                walkTruncated = false,
                matchedSelectedTab = true,
            ),
            updated,
            clock,
        )
        assertEquals(EnforcementAction.None, action)
    }

    @Test
    fun refreshBreakAllowanceIfNewDayRefillsBeforeUi() = runBlocking {
        runBlocking {
            preferences.applySnapshot(
                preferences.readSnapshot().copy(
                    breaksRemainingToday = 0,
                    breaksPerDay = 3,
                    breakDayKey = "2025-12-31",
                ),
            )
        }
        val now = java.time.LocalDate.of(2026, 1, 1)
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli() + 60_000L
        val updated = repository.refreshBreakAllowanceIfNewDay(now)
        assertEquals(3, updated.breaksRemainingToday)
        val snap = preferences.readSnapshot()
        assertEquals(3, snap.breaksRemainingToday)
        assertEquals("2026-01-01", snap.breakDayKey)
    }

    @Test
    fun startBreakPersistsBreakUntil() = runBlocking {
        val memory = EnforcementMemory(breaksRemainingToday = 3, breakAllowancePerDay = 3, breakDayKey = "")
        val now = 5_000L
        val updated = repository.startBreak(memory, now)
        assertNotNull(updated.breakUntilEpochMs)
        val snap = preferences.readSnapshot()
        assertEquals(updated.breakUntilEpochMs, snap.breakUntilEpochMs)
        assertEquals(2, snap.breaksRemainingToday)
        assertEquals(updated.breakUntilEpochMs, PolicySnapshotHolder.get().breakUntilEpochMs)
    }
}
