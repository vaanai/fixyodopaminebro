package app.quiet.service

import quiet.core.QuietTiming
import quiet.core.classify.SurfaceClassifier
import quiet.core.enforce.EnforcementAction
import quiet.core.enforce.EnforcementMemory
import quiet.core.enforce.EnforcementObservation
import quiet.core.enforce.Enforcer
import quiet.core.model.NodeInfo
import quiet.core.rules.AppRules
import quiet.core.rules.ClassificationResult
import quiet.core.rules.Policy
import quiet.core.rules.SurfaceKind
import quiet.core.walk.WalkResult

data class StabilityState(
    var lastSurfaceId: String? = null,
    var lastSeenAt: Long = 0L,
    var stableSurfaceId: String? = null,
)

data class DebounceState(
    var windowOpenUntil: Long = 0L,
    var dirty: Boolean = false,
)

data class OrchestratorEffects(
    val dismissSheet: Boolean = false,
    val showSheet: SheetRequest? = null,
    val showGate: GateRequest? = null,
    val clickMessages: NodeInfo? = null,
    val scheduleRecheck: Boolean = false,
    val clearStability: Boolean = false,
    val logAction: String? = null,
)

data class SheetRequest(
    val surfaceId: String,
    val label: String,
    val breakEnabled: Boolean,
    val messagesAvailable: Boolean,
)

data class GateRequest(
    val surfaceId: String,
    val label: String,
    val minutes: Int,
    val holdMs: Long,
)

class ServiceOrchestrator(
    private val instagramRules: AppRules,
    private val tiktokRules: AppRules,
    private val clock: () -> Long,
) {
    var stability = StabilityState()
    var debounce = DebounceState()
    var suppressUntil: Long = 0L
    var recheckGeneration: Long = 0L
    var policyLoaded: Boolean = false
    var policySnapshot: PolicyView = PolicyView.allowAll()
    var memory: EnforcementMemory = EnforcementMemory()
    var sheetShownForSurface: String? = null
    var sheetCooldownUntil: Map<String, Long> = emptyMap()

    fun onForeignPackage(): OrchestratorEffects =
        OrchestratorEffects(dismissSheet = true, clearStability = true)

    fun onQuietPackage(): OrchestratorEffects = OrchestratorEffects()

    fun shouldSuppress(): Boolean = clock() < suppressUntil

    fun markDirty(): Boolean {
        val now = clock()
        if (now >= debounce.windowOpenUntil) {
            debounce.windowOpenUntil = now + QuietTiming.DEBOUNCE_MS
            debounce.dirty = true
            return true
        }
        debounce.dirty = true
        return false
    }

    fun shouldRunWalk(now: Long = clock()): Boolean =
        debounce.dirty && now >= debounce.windowOpenUntil

    fun processWalk(
        packageName: String,
        walk: WalkResult,
        rules: AppRules,
    ): OrchestratorEffects {
        debounce.dirty = false
        val nodes = walk.nodes
        if (nodes.isEmpty()) {
            stability = StabilityState()
            return OrchestratorEffects(clearStability = true)
        }
        val classification = try {
            SurfaceClassifier.classify(nodes, rules, walk.truncated)
        } catch (_: Throwable) {
            stability = StabilityState()
            return OrchestratorEffects(dismissSheet = true, clearStability = true)
        }
        if (classification.isUnknown) {
            stability = StabilityState()
            if (sheetShownForSurface != null) {
                return OrchestratorEffects(dismissSheet = true, clearStability = true)
            }
            return OrchestratorEffects(clearStability = true)
        }
        val now = clock()
        updateStability(classification.surfaceId!!, now)
        if (stability.stableSurfaceId != classification.surfaceId) {
            return OrchestratorEffects()
        }
        if (!policyLoaded) {
            return OrchestratorEffects()
        }
        val surface = rules.surfaces.first { it.fullId == classification.surfaceId }
        val policy = policySnapshot.policyFor(surface.fullId, surface.defaultPolicy)
        val messagesNode = SurfaceClassifier.findMessagesNode(nodes, rules.messagesTabLabels)
        val observation = EnforcementObservation(
            appId = rules.app,
            surfaceId = classification.surfaceId,
            kind = classification.kind,
            policy = policy,
            enforce = surface.enforce,
            messagesFound = messagesNode != null,
            walkTruncated = walk.truncated,
            matchedSelectedTab = classification.matchedKey == quiet.core.rules.MatchKey.selectedTab,
        )
        val action = Enforcer.decide(observation, memory, object : quiet.core.enforce.Clock {
            override fun millis(): Long = clock()
        })
        return mapAction(action, observation, messagesNode, surface.label)
    }

    private fun updateStability(surfaceId: String, now: Long) {
        if (stability.lastSurfaceId != surfaceId) {
            stability = StabilityState(lastSurfaceId = surfaceId, lastSeenAt = now)
            return
        }
        if (now - stability.lastSeenAt >= QuietTiming.STABILITY_GAP_MS) {
            stability.stableSurfaceId = surfaceId
        }
    }

    private fun mapAction(
        action: EnforcementAction,
        observation: EnforcementObservation,
        messagesNode: NodeInfo?,
        surfaceLabel: String,
    ): OrchestratorEffects {
        return when (action) {
            EnforcementAction.None -> {
                val dismiss = observation.kind == SurfaceKind.protected ||
                    !observation.enforce ||
                    observation.surfaceId == null
                OrchestratorEffects(dismissSheet = dismiss && sheetShownForSurface != null)
            }
            is EnforcementAction.Redirect -> {
                memory = Enforcer.recordRedirect(memory, action.surfaceId, object : quiet.core.enforce.Clock {
                    override fun millis(): Long = clock()
                })
                suppressUntil = clock() + QuietTiming.ACTION_SUPPRESS_MS
                recheckGeneration++
                OrchestratorEffects(
                    clickMessages = messagesNode,
                    scheduleRecheck = true,
                    logAction = "REDIRECTED",
                )
            }
            EnforcementAction.ShowGate -> {
                suppressUntil = clock() + QuietTiming.ACTION_SUPPRESS_MS
                OrchestratorEffects(
                    showGate = GateRequest(
                        observation.surfaceId!!,
                        surfaceLabel,
                        policySnapshot.gateMinutes,
                        QuietTiming.GATE_HOLD_MS,
                    ),
                    logAction = "GATE_SHOWN",
                )
            }
            is EnforcementAction.ShowSheet -> {
                memory = Enforcer.recordSheet(memory, object : quiet.core.enforce.Clock {
                    override fun millis(): Long = clock()
                })
                suppressUntil = clock() + QuietTiming.ACTION_SUPPRESS_MS
                sheetShownForSurface = action.surfaceId
                OrchestratorEffects(
                    showSheet = SheetRequest(
                        surfaceId = action.surfaceId,
                        label = surfaceLabel,
                        breakEnabled = action.breakEnabled,
                        messagesAvailable = action.goToMessagesAvailable,
                    ),
                    logAction = "SHEET_SHOWN",
                )
            }
        }
    }

    fun rulesFor(packageName: String): AppRules? = when (packageName) {
        "com.instagram.android" -> instagramRules
        "com.zhiliaoapp.musically" -> tiktokRules
        else -> null
    }

    fun rulesForAppId(appId: String): AppRules? = when (appId) {
        "instagram" -> instagramRules
        "tiktok" -> tiktokRules
        else -> null
    }
}

data class PolicyView(
    val gateMinutes: Int,
    val policies: Map<String, Policy>,
    val friends: Map<String, Policy>,
) {
    fun policyFor(surfaceId: String, default: Policy): Policy =
        friends[surfaceId] ?: policies[surfaceId] ?: default

    companion object {
        fun allowAll() = PolicyView(gateMinutes = 10, policies = emptyMap(), friends = emptyMap())
    }
}
