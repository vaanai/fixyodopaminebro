package app.quiet.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import quiet.core.QuietTiming
import quiet.core.model.NodeInfo
import quiet.core.rules.AppRules
import quiet.core.rules.RuleParser
import quiet.core.walk.WalkResult

class ServiceSequenceTest {
    private lateinit var instagram: AppRules
    private var now = 0L
    private lateinit var orchestrator: ServiceOrchestrator

    @Before
    fun setup() {
        val json = javaClass.classLoader.getResource("rules/instagram.json")!!.readText()
        instagram = RuleParser.parseResource(json, "instagram.json").first!!
        now = 0L
        orchestrator = ServiceOrchestrator(instagram, instagram, clock = { now })
        orchestrator.policyLoaded = true
        orchestrator.policySnapshot = PolicyView(
            gateMinutes = 10,
            policies = mapOf("instagram.home_feed" to quiet.core.rules.Policy.BLOCK),
            friends = emptyMap(),
        )
    }

    @Test
    fun debounceCoalescesBurst() {
        repeat(50) { orchestrator.markDirty() }
        now += QuietTiming.DEBOUNCE_MS
        assertTrue(orchestrator.shouldRunWalk())
        val first = orchestrator.processWalk(
            "com.instagram.android",
            WalkResult(homeFeed(), false),
            instagram,
        )
        repeat(50) { orchestrator.markDirty() }
        now += QuietTiming.DEBOUNCE_MS
        val second = orchestrator.processWalk(
            "com.instagram.android",
            WalkResult(homeFeed(), false),
            instagram,
        )
        assertTrue(first == second || first.clickMessages == null)
    }

    @Test
    fun homeThenMessagesNoAction() {
        process(homeFeed())
        now += 100
        process(dmInbox())
        assertNull(orchestrator.stability.stableSurfaceId)
    }

    @Test
    fun stableFeedRedirectsOnce() {
        process(homeFeed())
        now += QuietTiming.STABILITY_GAP_MS
        val fx = process(homeFeed())
        assertNotNull(fx.clickMessages)
    }

    @Test
    fun foreignPackageDismisses() {
        val fx = orchestrator.onForeignPackage()
        assertTrue(fx.dismissSheet)
        assertTrue(fx.clearStability)
    }

    @Test
    fun emptyTreeClearsStability() {
        process(homeFeed())
        val fx = process(emptyList())
        assertTrue(fx.clearStability)
    }

    @Test
    fun policyAbsentNoAction() {
        orchestrator.policyLoaded = false
        process(homeFeed())
        now += QuietTiming.STABILITY_GAP_MS
        val fx = process(homeFeed())
        assertNull(fx.clickMessages)
    }

    private fun homeFeed() = listOf(
        NodeInfo(text = "Home", isSelected = true),
        NodeInfo(text = "Messages"),
    )

    private fun dmInbox() = listOf(
        NodeInfo(text = "Messages", isSelected = true),
    )

    private fun process(nodes: List<NodeInfo>): OrchestratorEffects {
        orchestrator.markDirty()
        now += QuietTiming.DEBOUNCE_MS
        assertTrue(orchestrator.shouldRunWalk())
        return orchestrator.processWalk(
            "com.instagram.android",
            WalkResult(nodes, truncated = false),
            instagram,
        )
    }
}
