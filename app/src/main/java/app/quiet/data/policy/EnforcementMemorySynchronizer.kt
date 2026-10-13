package app.quiet.data.policy

import quiet.core.enforce.EnforcementMemory

/**
 * Coordinates in-process enforcement memory updates between the accessibility heartbeat
 * and main-thread gate/break handlers so neither clobbers the other.
 */
class EnforcementMemorySynchronizer {
    private val lock = Any()

    fun readMemory(getMemory: () -> EnforcementMemory): EnforcementMemory =
        synchronized(lock) { getMemory() }

    fun mergeFromHolder(
        getMemory: () -> EnforcementMemory,
        setMemory: (EnforcementMemory) -> Unit,
    ) {
        synchronized(lock) {
            setMemory(mergeComputation(getMemory()))
        }
    }

    fun commitUpdated(
        setMemory: (EnforcementMemory) -> Unit,
        updated: EnforcementMemory,
    ) {
        synchronized(lock) {
            setMemory(updated)
        }
    }

    /**
     * Test-only: reproduces assigning merged memory after releasing the lock.
     */
    internal fun mergeFromHolderBroken(
        getMemory: () -> EnforcementMemory,
        setMemory: (EnforcementMemory) -> Unit,
        betweenLockAndAssign: () -> Unit,
    ) {
        val merged = synchronized(lock) { mergeComputation(getMemory()) }
        betweenLockAndAssign()
        setMemory(merged)
    }

    private fun mergeComputation(current: EnforcementMemory): EnforcementMemory {
        val persisted = EnforcementStateRepository.memoryFromRuntime(PolicySnapshotHolder.get())
        return current.copy(
            passes = persisted.passes,
            quietHoursEnabled = persisted.quietHoursEnabled,
            quietHoursStartMinutes = persisted.quietHoursStartMinutes,
            quietHoursEndMinutes = persisted.quietHoursEndMinutes,
            breaksRemainingToday = persisted.breaksRemainingToday,
            breakAllowancePerDay = persisted.breakAllowancePerDay,
            breakDayKey = persisted.breakDayKey,
            breakUntilEpochMs = persisted.breakUntilEpochMs,
        )
    }
}
