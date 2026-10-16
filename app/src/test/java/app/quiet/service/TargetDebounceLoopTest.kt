package app.quiet.service

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import quiet.core.QuietTiming
import quiet.core.rules.RuleParser

@OptIn(ExperimentalCoroutinesApi::class)
class TargetDebounceLoopTest {
    private lateinit var instagram: quiet.core.rules.AppRules
    private lateinit var tiktok: quiet.core.rules.AppRules
    private lateinit var orchestrator: ServiceOrchestrator

    @Before
    fun setup() {
        val igJson = javaClass.classLoader!!.getResource("rules/instagram.json")!!.readText()
        val ttJson = javaClass.classLoader!!.getResource("rules/tiktok.json")!!.readText()
        instagram = RuleParser.parseResource(igJson, "instagram.json").first!!
        tiktok = RuleParser.parseResource(ttJson, "tiktok.json").first!!
    }

    private fun orchestratorFor(clock: () -> Long): ServiceOrchestrator {
        val o = ServiceOrchestrator(instagram, tiktok, clock = clock)
        o.policyLoaded = true
        o.policySnapshot = PolicyView(
            gateMinutes = 10,
            policies = mapOf("instagram.home_feed" to quiet.core.rules.Policy.BLOCK),
            friends = emptyMap(),
        )
        return o
    }

    @Test
    fun burstEventsEvery100msStillYieldWalks() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = kotlinx.coroutines.CoroutineScope(SupervisorJob() + dispatcher)
        val clock = { testScheduler.currentTime }
        orchestrator = orchestratorFor(clock)
        val walked = mutableListOf<String>()
        var syncCount = 0
        val runner = TargetDebounceRunner(
            scope = scope,
            orchestrator = orchestrator,
            clock = clock,
            syncPolicy = { syncCount++ },
            runWalk = { pkg ->
                walked.add(pkg)
                orchestrator.debounce.dirty = false
                null
            },
            onAfterWalk = {},
            onFailure = {},
        )
        val igPkg = instagram.packages.first()
        repeat(50) {
            orchestrator.markDirty()
            runner.onEvent(igPkg)
            advanceTimeBy(100)
        }
        advanceUntilIdle()
        assertTrue(walked.size >= 3)
        assertTrue(syncCount >= 3)
        walked.forEach { assertEquals(igPkg, it) }
    }

    @Test
    fun policySyncBetweenWalks() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = kotlinx.coroutines.CoroutineScope(SupervisorJob() + dispatcher)
        val clock = { testScheduler.currentTime }
        orchestrator = orchestratorFor(clock)
        var syncCount = 0
        var policyAtWalk = quiet.core.rules.Policy.BLOCK
        val runner = TargetDebounceRunner(
            scope = scope,
            orchestrator = orchestrator,
            clock = clock,
            syncPolicy = {
                syncCount++
                policyAtWalk = orchestrator.policySnapshot.policies["instagram.home_feed"] ?: quiet.core.rules.Policy.ALLOW
            },
            runWalk = {
                orchestrator.debounce.dirty = false
                null
            },
            onAfterWalk = {},
            onFailure = {},
        )
        val igPkg = instagram.packages.first()
        orchestrator.markDirty()
        runner.onEvent(igPkg)
        advanceTimeBy(QuietTiming.DEBOUNCE_MS)
        advanceUntilIdle()
        assertEquals(1, syncCount)
        assertEquals(quiet.core.rules.Policy.BLOCK, policyAtWalk)

        orchestrator.policySnapshot = orchestrator.policySnapshot.copy(
            policies = mapOf("instagram.home_feed" to quiet.core.rules.Policy.ALLOW),
        )
        orchestrator.markDirty()
        runner.onEvent(igPkg)
        advanceTimeBy(QuietTiming.DEBOUNCE_MS)
        advanceUntilIdle()
        assertEquals(2, syncCount)
        assertEquals(quiet.core.rules.Policy.ALLOW, policyAtWalk)
    }

    @Test
    fun packageSwitchWalksLatestApp() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = kotlinx.coroutines.CoroutineScope(SupervisorJob() + dispatcher)
        val clock = { testScheduler.currentTime }
        orchestrator = orchestratorFor(clock)
        val walked = mutableListOf<String>()
        val runner = TargetDebounceRunner(
            scope = scope,
            orchestrator = orchestrator,
            clock = clock,
            syncPolicy = {},
            runWalk = { pkg ->
                walked.add(pkg)
                orchestrator.debounce.dirty = false
                null
            },
            onAfterWalk = {},
            onFailure = {},
        )
        val igPkg = instagram.packages.first()
        val ttPkg = tiktok.packages.first()
        orchestrator.markDirty()
        runner.onEvent(igPkg)
        advanceTimeBy(QuietTiming.DEBOUNCE_MS)
        advanceUntilIdle()
        assertEquals(listOf(igPkg), walked)

        repeat(10) {
            orchestrator.markDirty()
            runner.onEvent(ttPkg)
            advanceTimeBy(100)
        }
        advanceTimeBy(QuietTiming.DEBOUNCE_MS)
        advanceUntilIdle()
        assertTrue(walked.size >= 2)
        assertEquals(ttPkg, walked.last())
    }
}
