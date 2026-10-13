package app.quiet.data.policy

import quiet.core.enforce.EnforcementMemory

/**
 * Coordinates in-process enforcement memory updates between the accessibility heartbeat
 * and main-thread gate/break handlers so neither clobbers the other.
 */
class EnforcementMemorySynchronizer {
    private val lock = Any()

    fun <T> withLock(block: () -> T): T = synchronized(lock, block)

    fun mergeFromHolder(current: EnforcementMemory): EnforcementMemory = withLock {
        val persisted = EnforcementStateRepository.memoryFromRuntime(PolicySnapshotHolder.get())
        current.copy(
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

    fun applyUpdated(updated: EnforcementMemory): EnforcementMemory = withLock { updated }
}
