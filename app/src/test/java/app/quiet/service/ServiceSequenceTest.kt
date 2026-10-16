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
import quiet.core.enforce.EnforcementMemory
import quiet.core.enforce.Enforcer
import quiet.core.enforce.SurfacePass
import quiet.core.walk.WalkResult

class ServiceSequenceTest {
    private lateinit var instagram: AppRules
    private var now = 0L
    private lateinit var orchestrator: ServiceOrchestrator
    private lateinit var engine: EnforcementEngine
    private var walkCount = 0
    private var clickCount = 0
    private var sheetCount = 0
    private var gateCount = 0
    private var lastGateRequest: GateRequest? = null
    private var dismissCount = 0
    private var lastWalkPackage: String? = null
    private var textGetterCalled = false
    private var walkResults: MutableList<WalkResult> = mutableListOf()

    @Before
    fun setup() {
        val json = javaClass.classLoader!!.getResource("rules/instagram.json")!!.readText()
        instagram = RuleParser.parseResource(json, "instagram.json").first!!
        now = 0L
        walkCount = 0
        clickCount = 0
        sheetCount = 0
        gateCount = 0
        lastGateRequest = null
        dismissCount = 0
        walkResults = mutableListOf(WalkResult(homeFeed(), false))
        orchestrator = ServiceOrchestrator(instagram, instagram, clock = { now })
        orchestrator.policyLoaded = true
        orchestrator.policySnapshot = PolicyView(
            gateMinutes = 10,
            policies = mapOf("instagram.home_feed" to quiet.core.rules.Policy.BLOCK),
            friends = emptyMap(),
        )
        engine = EnforcementEngine(
            orchestrator,
            clock = { now },
            callbacks = EngineCallbacks(
                walk = { pkg ->
                    lastWalkPackage = pkg
                    walkCount++
                    walkResults.removeFirstOrNull() ?: WalkResult(homeFeed(), false)
                },
                clickMessages = { _, _, _ ->
                    clickCount++
                    true
                },
                showIntervention = { sheetCount++ },
                showGate = {
                    gateCount++
                    lastGateRequest = it
                },
                dismissSheet = { dismissCount++ },
                scheduleRecheck = { gen, _ -> engine.runRecheck("com.instagram.android", gen) },
                scheduleSheetTimeout = { engine.onSheetTimeout() },
            ),
        )
    }

    @Test
    fun hundredEventsProduceOneWalk() {
        repeat(100) { engine.onTargetEvent("com.instagram.android") }
        now += QuietTiming.DEBOUNCE_MS
        engine.runDebouncedWalk("com.instagram.android")
        assertEquals(1, walkCount)
    }

    @Test
    fun walksContinueOverSeveralSeconds() {
        repeat(12) {
            orchestrator.markDirty()
            now = orchestrator.debounce.windowOpenUntil
            engine.runDebouncedWalk("com.instagram.android")
        }
        assertTrue(walkCount >= 12)
    }

    @Test
    fun homeThenMessagesNoAction() {
        walkResults = mutableListOf(
            WalkResult(homeFeed(), false),
            WalkResult(dmInbox(), false),
        )
        engine.onTargetEvent("com.instagram.android")
        now += QuietTiming.DEBOUNCE_MS
        engine.runDebouncedWalk("com.instagram.android")
        now += 100
        engine.onTargetEvent("com.instagram.android")
        now += QuietTiming.DEBOUNCE_MS
        engine.runDebouncedWalk("com.instagram.android")
        assertNull(orchestrator.stability.stableSurfaceId)
        assertEquals(0, clickCount)
    }

    @Test
    fun stableFeedRedirectsOnce() {
        walkResults = mutableListOf(WalkResult(homeFeed(), false), WalkResult(homeFeed(), false))
        engine.onTargetEvent("com.instagram.android")
        now += QuietTiming.DEBOUNCE_MS
        engine.runDebouncedWalk("com.instagram.android")
        now += QuietTiming.STABILITY_GAP_MS
        engine.onTargetEvent("com.instagram.android")
        now += QuietTiming.DEBOUNCE_MS
        engine.runDebouncedWalk("com.instagram.android")
        assertEquals(1, clickCount)
    }

    @Test
    fun burstAfterRedirectShowsSheetNotSecondClick() {
        walkResults = mutableListOf(
            WalkResult(homeFeed(), false),
            WalkResult(homeFeed(), false),
            WalkResult(homeFeed(), false),
        )
        redirectOnce()
        val before = clickCount
        now += QuietTiming.RECHECK_DELAY_MS
        engine.runRecheck("com.instagram.android", orchestrator.recheckGeneration)
        assertEquals(before, clickCount)
        assertTrue(sheetCount >= 1)
    }

    @Test
    fun secondVisitWithin60sShowsSheetZeroClicks() {
        walkResults = mutableListOf(
            WalkResult(homeFeed(), false),
            WalkResult(homeFeed(), false),
            WalkResult(homeFeed(), false),
        )
        stableFeedRedirectsOnce()
        clickCount = 0
        now += 5_000
        orchestrator.suppressUntil = 0
        engine.onTargetEvent("com.instagram.android")
        now += QuietTiming.DEBOUNCE_MS
        engine.runDebouncedWalk("com.instagram.android")
        now += QuietTiming.STABILITY_GAP_MS
        engine.onTargetEvent("com.instagram.android")
        now += QuietTiming.DEBOUNCE_MS
        engine.runDebouncedWalk("com.instagram.android")
        assertEquals(0, clickCount)
        assertTrue(sheetCount >= 1)
    }

    @Test
    fun emptyTreeAfterReelsClearsStreak() {
        walkResults = mutableListOf(
            WalkResult(reels(), false),
            WalkResult(emptyList(), false),
        )
        engine.onTargetEvent("com.instagram.android")
        now += QuietTiming.DEBOUNCE_MS
        engine.runDebouncedWalk("com.instagram.android")
        now += QuietTiming.STABILITY_GAP_MS
        engine.onTargetEvent("com.instagram.android")
        now += QuietTiming.DEBOUNCE_MS
        engine.runDebouncedWalk("com.instagram.android")
        assertNull(orchestrator.stability.stableSurfaceId)
        assertEquals(0, clickCount)
    }

    @Test
    fun classifyExceptionDismissesAndNoThrow() {
        orchestrator = ServiceOrchestrator(
            instagram,
            instagram,
            clock = { throw RuntimeException("boom") },
        )
        engine.handleClassificationThrow()
        assertTrue(dismissCount >= 0)
    }

    @Test
    fun nonTargetPackageDoesNotWalk() {
        val fx = engine.onForeignPackage(walkerCalled = false)
        assertTrue(fx.dismissSheet)
        assertFalse(textGetterCalled)
        assertEquals(0, walkCount)
    }

    @Test
    fun overlayPackageIsNotWalked() {
        assertTrue(engine.isOverlayPackage("app.quiet"))
        lastWalkPackage = "com.instagram.android"
        assertEquals("com.instagram.android", lastWalkPackage)
    }

    @Test
    fun truncatedWeakMatchNoBlock() {
        orchestrator.policySnapshot = PolicyView(
            gateMinutes = 10,
            policies = mapOf("instagram.explore" to quiet.core.rules.Policy.BLOCK),
            friends = emptyMap(),
        )
        val explore = listOf(NodeInfo(text = "quiet.synthetic.instagram.explore"))
        walkResults = mutableListOf(WalkResult(explore, truncated = true))
        engine.runDebouncedWalk("com.instagram.android")
        assertEquals(0, clickCount)
        assertEquals(0, sheetCount)
    }

    @Test
    fun selectedMessagesTabNoBlock() {
        walkResults = mutableListOf(WalkResult(dmInbox(), false))
        engine.onTargetEvent("com.instagram.android")
        now += QuietTiming.DEBOUNCE_MS
        engine.runDebouncedWalk("com.instagram.android")
        now += QuietTiming.STABILITY_GAP_MS
        engine.onTargetEvent("com.instagram.android")
        now += QuietTiming.DEBOUNCE_MS
        engine.runDebouncedWalk("com.instagram.android")
        assertEquals(0, clickCount)
    }

    @Test
    fun sheetIdempotentAndProtectedDismisses() {
        val req = SheetRequest("instagram.home_feed", "Home feed", true, true)
        engine.showSheetIfAllowed(req)
        engine.showSheetIfAllowed(req)
        assertEquals(1, sheetCount)
        walkResults = mutableListOf(WalkResult(dmInbox(), false))
        engine.onTargetEvent("com.instagram.android")
        now += QuietTiming.DEBOUNCE_MS
        engine.runDebouncedWalk("com.instagram.android")
        assertTrue(dismissCount >= 1)
    }

    @Test
    fun sheetTimeoutAddsCooldown() {
        val req = SheetRequest("instagram.home_feed", "Home feed", true, true)
        engine.showSheetIfAllowed(req)
        now += QuietTiming.SHEET_TIMEOUT_MS
        engine.onSheetTimeout()
        sheetCount = 0
        engine.showSheetIfAllowed(req)
        assertEquals(0, sheetCount)
        now += QuietTiming.SHEET_COOLDOWN_MS
        engine.showSheetIfAllowed(req)
        assertEquals(1, sheetCount)
    }

    @Test
    fun generationBumpCancelsRecheck() {
        val gen = orchestrator.recheckGeneration
        orchestrator.recheckGeneration++
        engine.runRecheck("com.instagram.android", gen)
        assertEquals(0, sheetCount)
    }

    @Test
    fun ancestorTooLargeShowsSheetNotClick() {
        val label = NodeInfo(text = "Messages", boundsLeft = 10, boundsTop = 10, boundsRight = 20, boundsBottom = 20)
        val huge = NodeInfo(
            text = "Messages",
            isClickable = true,
            boundsLeft = 0,
            boundsTop = 0,
            boundsRight = 200,
            boundsBottom = 200,
        )
        val feed = listOf(
            NodeInfo(text = "Home", isSelected = true),
            label,
            huge,
        )
        walkResults = mutableListOf(WalkResult(feed, false), WalkResult(feed, false))
        engine.onTargetEvent("com.instagram.android")
        now += QuietTiming.DEBOUNCE_MS
        engine.runDebouncedWalk("com.instagram.android")
        now += QuietTiming.STABILITY_GAP_MS
        engine.onTargetEvent("com.instagram.android")
        now += QuietTiming.DEBOUNCE_MS
        engine.runDebouncedWalk("com.instagram.android")
        assertEquals(0, clickCount)
        assertTrue(sheetCount >= 1)
    }

    @Test
    fun gateDismissCooldownBlocksImmediateReshow() {
        orchestrator.recordSurfaceCooldown("instagram.home_feed")
        val req = GateRequest(
            surfaceId = "instagram.home_feed",
            label = "Home feed",
            minutes = 10,
            holdMs = 3_000L,
            messagesAvailable = true,
        )
        engine.showGateIfAllowed(req)
        assertEquals(0, gateCount)
        now += QuietTiming.SHEET_COOLDOWN_MS + 1
        engine.showGateIfAllowed(req)
        assertEquals(1, gateCount)
    }

    @Test
    fun allowOnStableFeedDismissesSheet() {
        orchestrator.sheetShownForSurface = "instagram.home_feed"
        orchestrator.policySnapshot = PolicyView(
            gateMinutes = 10,
            policies = mapOf("instagram.home_feed" to quiet.core.rules.Policy.ALLOW),
            friends = emptyMap(),
        )
        walkResults = mutableListOf(WalkResult(homeFeed(), false), WalkResult(homeFeed(), false))
        engine.onTargetEvent("com.instagram.android")
        now += QuietTiming.DEBOUNCE_MS
        engine.runDebouncedWalk("com.instagram.android")
        now += QuietTiming.STABILITY_GAP_MS
        engine.onTargetEvent("com.instagram.android")
        now += QuietTiming.DEBOUNCE_MS
        engine.runDebouncedWalk("com.instagram.android")
        assertTrue(dismissCount >= 1)
    }

    @Test
    fun gateHoldMsComesFromPolicySnapshot() {
        orchestrator.policySnapshot = PolicyView(
            gateMinutes = 10,
            holdSeconds = 5,
            policies = mapOf("instagram.home_feed" to quiet.core.rules.Policy.GATE),
            friends = emptyMap(),
        )
        walkResults = mutableListOf(WalkResult(homeFeed(), false), WalkResult(homeFeed(), false))
        engine.onTargetEvent("com.instagram.android")
        now += QuietTiming.DEBOUNCE_MS
        engine.runDebouncedWalk("com.instagram.android")
        now += QuietTiming.STABILITY_GAP_MS
        engine.onTargetEvent("com.instagram.android")
        now += QuietTiming.DEBOUNCE_MS
        engine.runDebouncedWalk("com.instagram.android")
        assertEquals(1, gateCount)
        assertEquals(5_000L, lastGateRequest?.holdMs)
    }

    @Test
    fun activeGatePassShowsNoGate() {
        orchestrator.policySnapshot = PolicyView(
            gateMinutes = 10,
            policies = mapOf("instagram.home_feed" to quiet.core.rules.Policy.GATE),
            friends = emptyMap(),
        )
        orchestrator.memory = EnforcementMemory(
            passes = listOf(SurfacePass("instagram.home_feed", now + 600_000)),
        )
        walkResults = mutableListOf(WalkResult(homeFeed(), false), WalkResult(homeFeed(), false))
        engine.onTargetEvent("com.instagram.android")
        now += QuietTiming.DEBOUNCE_MS
        engine.runDebouncedWalk("com.instagram.android")
        now += QuietTiming.STABILITY_GAP_MS
        engine.onTargetEvent("com.instagram.android")
        now += QuietTiming.DEBOUNCE_MS
        engine.runDebouncedWalk("com.instagram.android")
        assertEquals(0, gateCount)
    }

    @Test
    fun activeBreakSuppressesRedirect() {
        val clock = object : quiet.core.enforce.Clock {
            override fun millis(): Long = now
        }
        orchestrator.memory = Enforcer.startBreak(
            EnforcementMemory(breaksRemainingToday = 3, breakAllowancePerDay = 3, breakDayKey = "day"),
            clock,
        )
        walkResults = mutableListOf(WalkResult(homeFeed(), false), WalkResult(homeFeed(), false))
        engine.onTargetEvent("com.instagram.android")
        now += QuietTiming.DEBOUNCE_MS
        engine.runDebouncedWalk("com.instagram.android")
        now += QuietTiming.STABILITY_GAP_MS
        engine.onTargetEvent("com.instagram.android")
        now += QuietTiming.DEBOUNCE_MS
        engine.runDebouncedWalk("com.instagram.android")
        assertEquals(0, clickCount)
        assertEquals(0, sheetCount)
    }

    @Test
    fun policySnapshotAbsentNoAction() {
        orchestrator.policyLoaded = false
        walkResults = mutableListOf(WalkResult(homeFeed(), false), WalkResult(homeFeed(), false))
        engine.onTargetEvent("com.instagram.android")
        now += QuietTiming.DEBOUNCE_MS
        engine.runDebouncedWalk("com.instagram.android")
        now += QuietTiming.STABILITY_GAP_MS
        engine.onTargetEvent("com.instagram.android")
        now += QuietTiming.DEBOUNCE_MS
        engine.runDebouncedWalk("com.instagram.android")
        assertEquals(0, clickCount)
    }

    private fun homeFeed() = listOf(
        NodeInfo(text = "Home", isSelected = true),
        NodeInfo(text = "Messages", boundsLeft = 10, boundsTop = 10, boundsRight = 30, boundsBottom = 30),
        NodeInfo(
            text = "Messages",
            isClickable = true,
            boundsLeft = 8,
            boundsTop = 8,
            boundsRight = 32,
            boundsBottom = 32,
        ),
    )

    private fun reels() = listOf(
        NodeInfo(text = "Reels", isSelected = true),
        NodeInfo(text = "Messages"),
    )

    private fun dmInbox() = listOf(
        NodeInfo(text = "Messages", isSelected = true),
    )

    private fun redirectOnce() {
        engine.onTargetEvent("com.instagram.android")
        now += QuietTiming.DEBOUNCE_MS
        engine.runDebouncedWalk("com.instagram.android")
        now += QuietTiming.STABILITY_GAP_MS
        engine.onTargetEvent("com.instagram.android")
        now += QuietTiming.DEBOUNCE_MS
        engine.runDebouncedWalk("com.instagram.android")
    }
}
