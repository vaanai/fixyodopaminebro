package quiet.core.enforce

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import quiet.core.QuietTiming
import quiet.core.rules.Policy
import quiet.core.rules.SurfaceKind
import java.time.LocalDate
import java.time.ZoneId

class EnforcerTest {
    private val zone = ZoneId.of("UTC")

    private fun obs(
        surfaceId: String? = "instagram.home_feed",
        kind: SurfaceKind? = SurfaceKind.feed,
        policy: Policy? = Policy.BLOCK,
        enforce: Boolean = true,
        messagesFound: Boolean = true,
        truncated: Boolean = false,
        selectedTab: Boolean = true,
    ) = EnforcementObservation(
        appId = "instagram",
        surfaceId = surfaceId,
        kind = kind,
        policy = policy,
        enforce = enforce,
        messagesFound = messagesFound,
        walkTruncated = truncated,
        matchedSelectedTab = selectedTab,
    )

    @Test
    fun protectedAndUnenforcedAndUnknownAreNone() {
        val clock = FakeClock(0, zone)
        assertEquals(
            EnforcementAction.None,
            Enforcer.decide(obs(kind = SurfaceKind.protected, policy = Policy.ALLOW), EnforcementMemory(), clock),
        )
        assertEquals(
            EnforcementAction.None,
            Enforcer.decide(obs(enforce = false), EnforcementMemory(), clock),
        )
        assertEquals(
            EnforcementAction.None,
            Enforcer.decide(
                EnforcementObservation(
                    appId = "instagram",
                    surfaceId = null,
                    kind = null,
                    policy = null,
                    enforce = false,
                    messagesFound = false,
                    walkTruncated = false,
                    matchedSelectedTab = false,
                ),
                EnforcementMemory(),
                clock,
            ),
        )
    }

    @Test
    fun truncatedWeakMatchIsNone() {
        val clock = FakeClock(0, zone)
        val action = Enforcer.decide(
            obs(truncated = true, selectedTab = false),
            EnforcementMemory(),
            clock,
        )
        assertEquals(EnforcementAction.None, action)
    }

    @Test
    fun truncatedSelectedTabMayBlock() {
        val clock = FakeClock(0, zone)
        val action = Enforcer.decide(
            obs(truncated = true, selectedTab = true),
            EnforcementMemory(),
            clock,
        )
        assertTrue(action is EnforcementAction.Redirect)
    }

    @Test
    fun breakAllowsBlock() {
        val clock = FakeClock(1_000, zone)
        val memory = EnforcementMemory(breakUntilEpochMs = 10_000)
        assertEquals(EnforcementAction.None, Enforcer.decide(obs(), memory, clock))
    }

    @Test
    fun passForSurfaceOnly() {
        val clock = FakeClock(1_000, zone)
        val memory = EnforcementMemory(
            passes = listOf(SurfacePass("instagram.home_feed", 5_000)),
        )
        assertEquals(EnforcementAction.None, Enforcer.decide(obs(), memory, clock))
        assertTrue(Enforcer.decide(obs(surfaceId = "instagram.reels"), memory, clock) is EnforcementAction.Redirect)
    }

    @Test
    fun quietHoursGateToBlock() {
        val clock = FakeClock(atHour(23), zone)
        val memory = EnforcementMemory(quietHoursEnabled = true)
        val escalated = Enforcer.decide(obs(policy = Policy.GATE), memory, clock)
        assertTrue(escalated is EnforcementAction.Redirect)
        val allow = Enforcer.decide(
            obs(policy = Policy.ALLOW, enforce = false),
            memory,
            clock,
        )
        assertEquals(EnforcementAction.None, allow)
    }

    @Test
    fun quietHoursWindowBounds() {
        val memory = EnforcementMemory(quietHoursEnabled = true)
        assertTrue(Enforcer.isInQuietHours(memory, FakeClock(atHour(23), zone)))
        assertTrue(Enforcer.isInQuietHours(memory, FakeClock(atHour(6), zone)))
        assertTrue(!Enforcer.isInQuietHours(memory, FakeClock(atHour(12), zone)))
        assertTrue(!Enforcer.isInQuietHours(memory, FakeClock(atHour(7), zone)))
    }

    @Test
    fun openOnlyIsAllow() {
        val clock = FakeClock(0, zone)
        assertEquals(
            EnforcementAction.None,
            Enforcer.decide(obs(policy = Policy.OPEN_ONLY, kind = SurfaceKind.shared_media), EnforcementMemory(), clock),
        )
    }

    @Test
    fun redirectThenSheetWithin60s() {
        val clock = FakeClock(0, zone)
        var memory = EnforcementMemory()
        val first = Enforcer.decide(obs(), memory, clock)
        assertTrue(first is EnforcementAction.Redirect)
        memory = Enforcer.recordRedirect(memory, "instagram.home_feed", clock)
        clock.advance(QuietTiming.REDIRECT_COOLDOWN_MS - 1)
        val second = Enforcer.decide(obs(), memory, clock)
        assertTrue(second is EnforcementAction.ShowSheet)
    }

    @Test
    fun suppressWindow() {
        val clock = FakeClock(0, zone)
        val memory = EnforcementMemory(actionSuppressUntilEpochMs = 5_000)
        assertEquals(EnforcementAction.None, Enforcer.decide(obs(), memory, clock))
    }

    @Test
    fun noMessagesShowsSheetUnavailable() {
        val clock = FakeClock(0, zone)
        val action = Enforcer.decide(obs(messagesFound = false), EnforcementMemory(), clock)
        assertTrue(action is EnforcementAction.ShowSheet)
        assertEquals(false, (action as EnforcementAction.ShowSheet).goToMessagesAvailable)
    }

    @Test
    fun breakAllowanceZeroDisablesButton() {
        val clock = FakeClock(0, zone)
        val memory = EnforcementMemory(breaksRemainingToday = 0)
        val action = Enforcer.decide(
            obs(messagesFound = false),
            memory,
            clock,
        ) as EnforcementAction.ShowSheet
        assertEquals(false, action.breakEnabled)
    }

    @Test
    fun startBreakDecrementsAndSetsUntil() {
        val clock = FakeClock(0, zone)
        val memory = EnforcementMemory(breaksRemainingToday = 3, breakAllowancePerDay = 3, breakDayKey = "2026-01-01")
        val updated = Enforcer.startBreak(memory, clock)
        assertEquals(2, updated.breaksRemainingToday)
        assertEquals(QuietTiming.BREAK_DURATION_MS.toLong(), updated.breakUntilEpochMs)
    }

    @Test
    fun missingPolicyNone() {
        val clock = FakeClock(0, zone)
        assertEquals(
            EnforcementAction.None,
            Enforcer.decide(obs(policy = null), EnforcementMemory(), clock),
        )
    }

    private fun atHour(hour: Int): Long {
        return LocalDate.of(2026, 1, 1).atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()
    }
}

private class FakeClock(private var millis: Long, private val zoneId: ZoneId) : Clock {
    override fun millis(): Long = millis
    override fun zone(): ZoneId = zoneId
    fun advance(by: Long) {
        millis += by
    }
}
