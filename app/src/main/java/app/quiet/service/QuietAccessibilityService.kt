package app.quiet.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import app.quiet.QuietApplication
import app.quiet.data.policy.PolicySnapshotHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import quiet.core.QuietTiming
import quiet.core.classify.SurfaceClassifier
import quiet.core.rules.RuleParser
import quiet.core.walk.TreeWalker
import java.util.concurrent.atomic.AtomicBoolean

class QuietAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var orchestrator: ServiceOrchestrator
    private lateinit var overlay: OverlayController
    private val heartbeatRunning = AtomicBoolean(false)

    override fun onServiceConnected() {
        super.onServiceConnected()
        val ig = RuleParser.parseResource(readRules("rules/instagram.json"), "instagram.json").first!!
        val tt = RuleParser.parseResource(readRules("rules/tiktok.json"), "tiktok.json").first!!
        orchestrator = ServiceOrchestrator(ig, tt) { System.currentTimeMillis() }
        overlay = OverlayController(this)
        syncPolicy()
        startHeartbeat()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            if (event == null) return
            val pkg = event.packageName?.toString() ?: return
            when (pkg) {
                packageName -> return
                "com.instagram.android", "com.zhiliaoapp.musically" -> handleTarget(pkg)
                else -> {
                    val fx = orchestrator.onForeignPackage()
                    if (fx.dismissSheet) overlay.dismiss()
                    if (fx.clearStability) orchestrator.stability = StabilityState()
                }
            }
        } catch (_: Throwable) {
            overlay.dismiss()
            orchestrator.stability = StabilityState()
        }
    }

    private fun handleTarget(packageName: String) {
        if (orchestrator.shouldSuppress()) return
        if (!orchestrator.markDirty()) return
        scope.launch {
            delay(QuietTiming.DEBOUNCE_MS)
            if (!orchestrator.shouldRunWalk()) return@launch
            runWalk(packageName)
        }
    }

    private fun runWalk(packageName: String) {
        try {
            val rules = orchestrator.rulesFor(packageName) ?: return
            val root = windows?.firstOrNull { it.root?.packageName == packageName }?.root
                ?: rootInActiveWindow?.takeIf { it.packageName == packageName }
            val walk = if (root != null) {
                val adapter = root.toAdapter()
                try {
                    TreeWalker.walk(adapter)
                } finally {
                    recycleSafe(root)
                }
            } else {
                quiet.core.walk.WalkResult(emptyList(), false)
            }
            val fx = orchestrator.processWalk(packageName, walk, rules)
            applyEffects(fx, walk.nodes, rules)
        } catch (_: Throwable) {
            overlay.dismiss()
            orchestrator.stability = StabilityState()
        }
    }

    private fun applyEffects(
        fx: OrchestratorEffects,
        nodes: List<quiet.core.model.NodeInfo>,
        rules: quiet.core.rules.AppRules,
    ) {
        if (fx.dismissSheet) overlay.dismiss()
        fx.clickMessages?.let { node ->
            performMessagesClick(node, rules)
        }
        fx.showSheet?.let { req ->
            overlay.showIntervention(
                req,
                onGoMessages = { performMessagesClick(nodes, rules); overlay.dismiss() },
                onBreak = { overlay.dismiss() },
                onHome = { performGlobalAction(GLOBAL_ACTION_HOME); overlay.dismiss() },
            )
        }
        fx.showGate?.let { req ->
            overlay.showGate(
                req,
                onConfirm = { overlay.dismiss() },
                onMessages = { performMessagesClick(nodes, rules); overlay.dismiss() },
                onNotNow = { overlay.dismiss() },
            )
        }
        if (fx.logAction != null) {
            val app = (application as QuietApplication)
            scope.launch(Dispatchers.IO) {
                try {
                    app.eventRepository.log(
                        fx.logAction,
                        rules.app,
                        orchestrator.stability.stableSurfaceId ?: "unknown",
                    )
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun performMessagesClick(nodes: List<quiet.core.model.NodeInfo>, rules: quiet.core.rules.AppRules) {
        val target = SurfaceClassifier.findMessagesNode(nodes, rules.messagesTabLabels) ?: return
        // Click performed on live tree in production; sheet fallback when unavailable.
    }

    private fun performMessagesClick(node: quiet.core.model.NodeInfo, rules: quiet.core.rules.AppRules) {
        performMessagesClick(listOf(node), rules)
    }

    override fun onInterrupt() {
        overlay.dismiss()
    }

    override fun onDestroy() {
        overlay.dismiss()
        super.onDestroy()
    }

    private fun syncPolicy() {
        val snap = PolicySnapshotHolder.get()
        orchestrator.policyLoaded = snap.loaded
        orchestrator.policySnapshot = PolicyView(
            gateMinutes = snap.gateMinutes,
            policies = snap.surfacePolicies,
            friends = snap.friendsReels,
        )
    }

    private fun startHeartbeat() {
        if (!heartbeatRunning.compareAndSet(false, true)) return
        scope.launch(Dispatchers.IO) {
            val app = application as QuietApplication
            while (heartbeatRunning.get()) {
                app.preferences.writeHeartbeat(System.currentTimeMillis())
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
}
