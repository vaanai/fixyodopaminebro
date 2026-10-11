package app.quiet.service

import quiet.core.QuietTiming
import quiet.core.classify.SurfaceClassifier
import quiet.core.enforce.EnforcementAction
import quiet.core.model.NodeInfo
import quiet.core.rules.AppRules
import quiet.core.rules.ClassificationResult
import quiet.core.rules.SurfaceKind
import quiet.core.walk.WalkResult

data class EngineCallbacks(
    val walk: (String) -> WalkResult,
    val clickMessages: (NodeInfo, List<NodeInfo>, AppRules) -> Boolean,
    val showIntervention: (SheetRequest) -> Unit,
    val showGate: (GateRequest) -> Unit,
    val dismissSheet: () -> Unit,
    val scheduleRecheck: (generation: Long, packageName: String) -> Unit,
    val scheduleSheetTimeout: () -> Unit,
)

class EnforcementEngine(
    private val orchestrator: ServiceOrchestrator,
    private val clock: () -> Long,
    private val callbacks: EngineCallbacks,
) {
    var overlayWalkSkipped: Boolean = false

    fun onForeignPackage(walkerCalled: Boolean = false): OrchestratorEffects {
        overlayWalkSkipped = walkerCalled
        val fx = orchestrator.onForeignPackage()
        if (fx.dismissSheet) callbacks.dismissSheet()
        if (fx.clearStability) orchestrator.stability = StabilityState()
        orchestrator.sheetShownForSurface = null
        return fx
    }

    fun onTargetEvent(packageName: String): Boolean {
        if (orchestrator.shouldSuppress()) return false
        orchestrator.markDirty()
        return true
    }

    fun runDebouncedWalk(packageName: String): OrchestratorEffects? {
        if (!orchestrator.shouldRunWalk()) return null
        val rules = orchestrator.rulesFor(packageName) ?: return null
        val walk = callbacks.walk(packageName)
        return handleWalk(packageName, walk, rules)
    }

    fun runRecheck(packageName: String, generation: Long) {
        if (generation != orchestrator.recheckGeneration) return
        val rules = orchestrator.rulesFor(packageName) ?: return
        val walk = callbacks.walk(packageName)
        val nodes = walk.nodes
        if (nodes.isEmpty()) return
        val classification = try {
            SurfaceClassifier.classify(nodes, rules, walk.truncated)
        } catch (_: Throwable) {
            return
        }
        if (classification.isUnknown) return
        val surface = rules.surfaces.firstOrNull { it.fullId == classification.surfaceId } ?: return
        if (!surface.enforce || surface.kind == SurfaceKind.protected) {
            callbacks.dismissSheet()
            return
        }
        val policy = orchestrator.policySnapshot.policyFor(surface.fullId, surface.defaultPolicy)
        val observation = quiet.core.enforce.EnforcementObservation(
            appId = rules.app,
            surfaceId = classification.surfaceId,
            kind = classification.kind,
            policy = policy,
            enforce = surface.enforce,
            messagesFound = SurfaceClassifier.findMessagesNode(nodes, rules.messagesTabLabels) != null,
            walkTruncated = walk.truncated,
            matchedSelectedTab = classification.matchedKey == quiet.core.rules.MatchKey.selectedTab,
        )
        var effective = policy
        if (orchestrator.memory.quietHoursEnabled && effective == quiet.core.rules.Policy.GATE &&
            quiet.core.enforce.Enforcer.isInQuietHours(orchestrator.memory, object : quiet.core.enforce.Clock {
                override fun millis(): Long = clock()
            })
        ) {
            effective = quiet.core.rules.Policy.BLOCK
        }
        val stillBlocked = surface.enforce && effective == quiet.core.rules.Policy.BLOCK
        if (stillBlocked) {
            showSheetIfAllowed(
                SheetRequest(
                    surfaceId = surface.fullId,
                    label = surface.label,
                    breakEnabled = orchestrator.memory.breaksRemainingToday > 0,
                    messagesAvailable = observation.messagesFound,
                ),
            )
        }
    }

    fun handleWalk(packageName: String, walk: WalkResult, rules: AppRules): OrchestratorEffects {
        val fx = orchestrator.processWalk(packageName, walk, rules)
        applyEffects(fx, walk.nodes, rules)
        return fx
    }

    fun handleClassificationThrow() {
        orchestrator.stability = StabilityState()
        callbacks.dismissSheet()
    }

    fun onSheetTimeout() {
        val surface = orchestrator.sheetShownForSurface
        callbacks.dismissSheet()
        orchestrator.sheetShownForSurface = null
        if (surface != null) {
            val until = clock() + QuietTiming.SHEET_COOLDOWN_MS
            orchestrator.sheetCooldownUntil = orchestrator.sheetCooldownUntil + (surface to until)
        }
    }

    private fun applyEffects(
        fx: OrchestratorEffects,
        nodes: List<NodeInfo>,
        rules: AppRules,
    ) {
        if (fx.dismissSheet) {
            callbacks.dismissSheet()
            orchestrator.sheetShownForSurface = null
        }
        if (fx.clickMessages != null) {
            val label = SurfaceClassifier.findMessagesNode(nodes, rules.messagesTabLabels)
            val clicked = if (label != null && MessageClickResolver.planClickForTest(label, nodes)) {
                callbacks.clickMessages(label, nodes, rules)
            } else {
                false
            }
            if (!clicked) {
                val surface = orchestrator.stability.stableSurfaceId ?: return
                val surfaceRule = rules.surfaces.first { it.fullId == surface }
                showSheetIfAllowed(
                    SheetRequest(
                        surfaceId = surface,
                        label = surfaceRule.label,
                        breakEnabled = orchestrator.memory.breaksRemainingToday > 0,
                        messagesAvailable = label != null,
                    ),
                )
            } else if (fx.scheduleRecheck) {
                val gen = orchestrator.recheckGeneration
                callbacks.scheduleRecheck(gen, rules.packages.first())
            }
        }
        fx.showSheet?.let { showSheetIfAllowed(it) }
        fx.showGate?.let { showGateIfAllowed(it) }
    }

    internal fun showSheetIfAllowed(req: SheetRequest) {
        val now = clock()
        val cooldown = orchestrator.sheetCooldownUntil[req.surfaceId] ?: 0L
        if (now < cooldown) return
        orchestrator.suppressUntil = now + QuietTiming.ACTION_SUPPRESS_MS
        orchestrator.sheetShownForSurface = req.surfaceId
        callbacks.showIntervention(req)
        callbacks.scheduleSheetTimeout()
    }

    private fun showGateIfAllowed(req: GateRequest) {
        val now = clock()
        val cooldown = orchestrator.sheetCooldownUntil[req.surfaceId] ?: 0L
        if (now < cooldown) return
        orchestrator.suppressUntil = now + QuietTiming.ACTION_SUPPRESS_MS
        orchestrator.sheetShownForSurface = req.surfaceId
        callbacks.showGate(req)
        callbacks.scheduleSheetTimeout()
    }

    fun isOverlayPackage(packageName: String): Boolean = packageName == "app.quiet"
}
