package app.quiet.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import app.quiet.QuietApplication
import app.quiet.data.policy.EnforcementMemorySynchronizer
import app.quiet.data.policy.PolicySnapshotHolder
import app.quiet.snapshot.SnapshotCapture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import quiet.core.QuietTiming
import quiet.core.rules.RuleParser
import quiet.core.walk.TreeWalker
import java.util.concurrent.atomic.AtomicBoolean

class QuietAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var orchestrator: ServiceOrchestrator
    private lateinit var engine: EnforcementEngine
    private lateinit var overlay: OverlayController
    private lateinit var debounceRunner: TargetDebounceRunner
    private val heartbeatRunning = AtomicBoolean(false)
    private var sheetTimeoutJob: kotlinx.coroutines.Job? = null
    private val confirmingWalkHolder = ConfirmingWalkHolder()
    private val enforcementMemorySync = EnforcementMemorySynchronizer()

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        val ig = RuleParser.parseResource(readRules("rules/instagram.json"), "instagram.json").first!!
        val tt = RuleParser.parseResource(readRules("rules/tiktok.json"), "tiktok.json").first!!
        orchestrator = ServiceOrchestrator(ig, tt) { System.currentTimeMillis() }
        overlay = OverlayController(this)
        engine = EnforcementEngine(
            orchestrator,
            clock = { System.currentTimeMillis() },
            callbacks = buildCallbacks(),
        )
        debounceRunner = TargetDebounceRunner(
            scope = scope,
            orchestrator = orchestrator,
            clock = { System.currentTimeMillis() },
            syncPolicy = { syncPolicyFromHolder() },
            runWalk = { packageName ->
                val fx = engine.runDebouncedWalk(packageName)
                if (fx != null) {
                    applyWalkEffects(fx)
                }
                fx
            },
            onAfterWalk = { confirmingWalkHolder.release(::recycleSafe) },
            onFailure = { failOpen() },
        )
        syncPolicyFromHolder()
        startHeartbeat()
    }

    private fun buildCallbacks(): EngineCallbacks = EngineCallbacks(
        walk = { packageName -> walkTargetWindow(packageName) },
        clickMessages = { label, _, _ ->
            clickMessagesOnHeldRoot(
                confirmingWalkHolder,
                label,
                MessageClickResolver::performClickFromConfirmingLabel,
            )
        },
        showIntervention = { req ->
            overlay.showIntervention(
                req,
                onGoMessages = {
                    val rules = orchestrator.rulesForAppId(req.surfaceId.substringBefore('.')) ?: return@showIntervention
                    val pkg = rules.packages.first()
                    findTargetRoot(pkg)?.let { root ->
                        try {
                            MessageClickResolver.performClick(root, rules.messagesTabLabels)
                        } finally {
                            recycleSafe(root)
                        }
                    }
                    overlay.dismiss()
                    orchestrator.sheetShownForSurface = null
                },
                onBreak = {
                    scope.launch(Dispatchers.IO) {
                        val app = application as QuietApplication
                        val current = enforcementMemorySync.readMemory { orchestrator.memory }
                        val updated = app.enforcementStateRepository.startBreak(
                            current,
                            System.currentTimeMillis(),
                        )
                        enforcementMemorySync.commitUpdated({ orchestrator.memory = it }, updated)
                        withContext(Dispatchers.Main) {
                            overlay.dismiss()
                            orchestrator.sheetShownForSurface = null
                        }
                    }
                },
                onHome = {
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    overlay.dismiss()
                    orchestrator.sheetShownForSurface = null
                },
            )
        },
        showGate = { req ->
            overlay.showGate(
                req,
                messagesFound = req.messagesAvailable,
                onConfirm = {
                    scope.launch(Dispatchers.IO) {
                        val app = application as QuietApplication
                        val now = System.currentTimeMillis()
                        val current = enforcementMemorySync.readMemory { orchestrator.memory }
                        val updated = app.enforcementStateRepository.grantGatePass(
                            req.surfaceId,
                            req.minutes,
                            current,
                            now,
                        )
                        enforcementMemorySync.commitUpdated({ orchestrator.memory = it }, updated)
                        logEffect(
                            OrchestratorEffects(logAction = "GATE_CONFIRMED"),
                        )
                        withContext(Dispatchers.Main) {
                            overlay.dismiss()
                            orchestrator.sheetShownForSurface = null
                        }
                    }
                },
                onMessages = {
                    val rules = orchestrator.rulesForAppId(req.surfaceId.substringBefore('.')) ?: return@showGate
                    val pkg = rules.packages.first()
                    findTargetRoot(pkg)?.let { root ->
                        try {
                            val clicked = MessageClickResolver.performClick(root, rules.messagesTabLabels)
                            if (!clicked) return@showGate
                        } finally {
                            recycleSafe(root)
                        }
                    } ?: return@showGate
                    overlay.dismiss()
                    orchestrator.sheetShownForSurface = null
                },
                onNotNow = {
                    orchestrator.recordSurfaceCooldown(req.surfaceId)
                    overlay.dismiss()
                    orchestrator.sheetShownForSurface = null
                },
                onGoHome = {
                    orchestrator.recordSurfaceCooldown(req.surfaceId)
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    overlay.dismiss()
                    orchestrator.sheetShownForSurface = null
                },
            )
        },
        dismissSheet = {
            overlay.dismiss()
            sheetTimeoutJob?.cancel()
        },
        scheduleRecheck = { generation, packageName ->
            scope.launch {
                delay(QuietTiming.RECHECK_DELAY_MS)
                engine.runRecheck(packageName, generation)
            }
        },
        scheduleSheetTimeout = {
            sheetTimeoutJob?.cancel()
            sheetTimeoutJob = scope.launch {
                delay(QuietTiming.SHEET_TIMEOUT_MS)
                engine.onSheetTimeout()
            }
        },
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            if (event == null) return
            val pkg = event.packageName?.toString() ?: return
            when (pkg) {
                packageName -> return
                "com.instagram.android", "com.zhiliaoapp.musically" -> handleTarget(pkg)
                else -> engine.onForeignPackage()
            }
        } catch (_: Throwable) {
            failOpen()
        }
    }

    private fun handleTarget(packageName: String) {
        if (!engine.onTargetEvent(packageName)) return
        debounceRunner.onEvent(packageName)
    }

    private fun applyWalkEffects(fx: OrchestratorEffects) {
        logEffect(fx)
        if (fx.recordUnknown) {
            scope.launch(Dispatchers.IO) {
                try {
                    (application as QuietApplication).preferences.incrementUnknown()
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun failOpen() {
        overlay.dismiss()
        orchestrator.stability = StabilityState()
    }

    private fun logEffect(fx: OrchestratorEffects) {
        val action = fx.logAction ?: return
        val appId = orchestrator.stability.stableSurfaceId?.substringBefore('.') ?: return
        scope.launch(Dispatchers.IO) {
            try {
                (application as QuietApplication).eventRepository.log(
                    action,
                    appId,
                    orchestrator.stability.stableSurfaceId ?: "unknown",
                )
            } catch (_: Exception) {
            }
        }
    }

    private fun walkTargetWindow(packageName: String): quiet.core.walk.WalkResult {
        confirmingWalkHolder.release(::recycleSafe)
        val root = findTargetRoot(packageName) ?: return quiet.core.walk.WalkResult(emptyList(), false)
        confirmingWalkHolder.heldRoot = root
        confirmingWalkHolder.heldPackage = packageName
        val adapter = root.toAdapter()
        return TreeWalker.walk(adapter)
    }

    private fun findTargetRoot(packageName: String): AccessibilityNodeInfo? {
        confirmingWalkHolder.freshRootLookups++
        val fromWindows = windows?.firstOrNull { it.root?.packageName?.toString() == packageName }?.root
        if (fromWindows != null) return AccessibilityNodeInfo.obtain(fromWindows)
        val active = rootInActiveWindow
        if (active != null && active.packageName?.toString() == packageName) {
            return AccessibilityNodeInfo.obtain(active)
        }
        return null
    }

    fun captureSnapshotForDiagnostics(): SnapshotCapture.Result =
        SnapshotCapture.capture(this, orchestrator::rulesFor)

    fun refreshPolicyFromHolder() {
        syncPolicyFromHolder()
    }

    override fun onInterrupt() {
        overlay.dismiss()
        sheetTimeoutJob?.cancel()
    }

    override fun onDestroy() {
        instance = null
        overlay.dismiss()
        sheetTimeoutJob?.cancel()
        super.onDestroy()
    }

    private fun syncPolicyFromHolder() {
        val snap = PolicySnapshotHolder.get()
        orchestrator.policyLoaded = snap.loaded
        orchestrator.policySnapshot = PolicyView(
            gateMinutes = snap.gateMinutes,
            holdSeconds = snap.holdSeconds,
            policies = snap.surfacePolicies,
            friends = snap.friendsReels,
        )
        syncMemoryFromHolder()
    }

    private fun syncMemoryFromHolder() {
        enforcementMemorySync.mergeFromHolder(
            getMemory = { orchestrator.memory },
            setMemory = { orchestrator.memory = it },
        )
    }

    private fun startHeartbeat() {
        if (!heartbeatRunning.compareAndSet(false, true)) return
        scope.launch(Dispatchers.IO) {
            val app = application as QuietApplication
            while (heartbeatRunning.get()) {
                app.preferences.writeHeartbeat(System.currentTimeMillis())
                app.enforcementStateRepository.refreshBreakAllowanceIfNewDay(System.currentTimeMillis())
                withContext(Dispatchers.Main) {
                    syncPolicyFromHolder()
                }
                delay(QuietTiming.HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    private fun recycleSafe(node: AccessibilityNodeInfo?) {
        if (node == null) return
        try {
            node.recycle()
        } catch (_: Exception) {
        }
    }

    private fun readRules(path: String): String =
        javaClass.classLoader.getResource(path)?.readText() ?: error("Missing $path")

    companion object {
        @Volatile
        var instance: QuietAccessibilityService? = null
    }
}
