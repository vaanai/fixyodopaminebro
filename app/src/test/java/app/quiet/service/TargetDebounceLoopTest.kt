package app.quiet.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import quiet.core.QuietTiming
import quiet.core.rules.RuleParser

/**
 * Simulates the accessibility debounce contract: events every 100 ms must not cancel the
 * open window, and walks still run about every [QuietTiming.DEBOUNCE_MS].
 */
class TargetDebounceLoopTest {
    private lateinit var instagram: quiet.core.rules.AppRules
    private var now = 0L
    private lateinit var orchestrator: ServiceOrchestrator
    private var walkCount = 0

    @Before
    fun setup() {
        val json = javaClass.classLoader!!.getResource("rules/instagram.json")!!.readText()
        instagram = RuleParser.parseResource(json, "instagram.json").first!!
        now = 0L
        walkCount = 0
        orchestrator = ServiceOrchestrator(instagram, instagram, clock = { now })
        orchestrator.policyLoaded = true
        orchestrator.policySnapshot = PolicyView(
            gateMinutes = 10,
            policies = mapOf("instagram.home_feed" to quiet.core.rules.Policy.BLOCK),
            friends = emptyMap(),
        )
    }

    @Test
    fun burstEventsEvery100msStillYieldWalks() {
        var loopRunning = false
        repeat(50) { tick ->
            orchestrator.markDirty()
            if (!loopRunning) {
                loopRunning = true
            }
            now += 100
            if (orchestrator.shouldRunWalk()) {
                walkCount++
                orchestrator.debounce.dirty = false
            }
        }
        assertTrue(walkCount >= 3)
    }

    @Test
    fun policySyncBetweenWalks() {
        var walks = 0
        orchestrator.markDirty()
        now += QuietTiming.DEBOUNCE_MS
        if (orchestrator.shouldRunWalk()) {
            orchestrator.policySnapshot = PolicyView(
                gateMinutes = 10,
                policies = mapOf("instagram.home_feed" to quiet.core.rules.Policy.BLOCK),
                friends = emptyMap(),
            )
            walks++
            orchestrator.debounce.dirty = false
        }
        orchestrator.markDirty()
        now += QuietTiming.DEBOUNCE_MS
        if (orchestrator.shouldRunWalk()) {
            orchestrator.policySnapshot = PolicyView(
                gateMinutes = 10,
                policies = mapOf("instagram.home_feed" to quiet.core.rules.Policy.ALLOW),
                friends = emptyMap(),
            )
            walks++
        }
        assertEquals(2, walks)
        assertEquals(quiet.core.rules.Policy.ALLOW, orchestrator.policySnapshot.policies["instagram.home_feed"])
    }
}
