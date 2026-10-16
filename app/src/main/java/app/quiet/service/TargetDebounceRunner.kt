package app.quiet.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Coalesces accessibility events into periodic walks without canceling an open debounce window.
 */
class TargetDebounceRunner(
    private val scope: CoroutineScope,
    private val orchestrator: ServiceOrchestrator,
    private val clock: () -> Long,
    private val syncPolicy: () -> Unit,
    private val runWalk: (packageName: String) -> OrchestratorEffects?,
    private val onAfterWalk: () -> Unit,
    private val onFailure: () -> Unit,
) {
    private var job: Job? = null
    private var walkPackage: String? = null

    fun onEvent(packageName: String) {
        walkPackage = packageName
        if (job?.isActive == true) return
        job = scope.launch {
            try {
                debounceLoop()
            } catch (_: Throwable) {
                onFailure()
            }
        }
    }

    internal suspend fun debounceLoop() {
        while (true) {
            val wait = orchestrator.debounce.windowOpenUntil - clock()
            if (wait > 0) {
                delay(wait)
            }
            if (!orchestrator.shouldRunWalk(clock())) {
                if (!orchestrator.debounce.dirty) break
                continue
            }
            val packageName = walkPackage ?: break
            syncPolicy()
            runWalk(packageName)
            onAfterWalk()
            if (!orchestrator.debounce.dirty) break
        }
    }
}
