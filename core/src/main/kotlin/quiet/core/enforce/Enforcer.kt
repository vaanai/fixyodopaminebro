package quiet.core.enforce

import quiet.core.QuietTiming
import quiet.core.rules.Policy
import quiet.core.rules.SurfaceKind
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

fun interface Clock {
    fun millis(): Long
    fun zone(): ZoneId = ZoneId.systemDefault()
}

object Enforcer {
    fun decide(
        observation: EnforcementObservation,
        memory: EnforcementMemory,
        clock: Clock,
    ): EnforcementAction {
        val now = clock.millis()
        if (now < memory.actionSuppressUntilEpochMs) {
            return EnforcementAction.None
        }

        val surfaceId = observation.surfaceId
        if (surfaceId == null || observation.kind == null) {
            return EnforcementAction.None
        }

        if (observation.kind == SurfaceKind.protected) {
            return EnforcementAction.None
        }

        if (!observation.enforce) {
            return EnforcementAction.None
        }

        if (observation.walkTruncated && !observation.matchedSelectedTab) {
            return EnforcementAction.None
        }

        val policy = observation.policy ?: return EnforcementAction.None

        if (policy == Policy.ALLOW || policy == Policy.OPEN_ONLY) {
            return EnforcementAction.None
        }

        val activeBreak = memory.breakUntilEpochMs?.let { it > now } == true
        if (activeBreak) {
            return EnforcementAction.None
        }

        val pass = memory.passes.firstOrNull { it.surfaceId == surfaceId && it.untilEpochMs > now }
        if (pass != null) {
            return EnforcementAction.None
        }

        var effective = policy
        if (memory.quietHoursEnabled && effective == Policy.GATE) {
            if (isInQuietHours(memory, clock)) {
                effective = Policy.BLOCK
            }
        }

        return when (effective) {
            Policy.ALLOW, Policy.OPEN_ONLY -> EnforcementAction.None
            Policy.GATE -> EnforcementAction.ShowGate
            Policy.BLOCK -> blockAction(observation, memory, now, surfaceId)
        }
    }

    private fun blockAction(
        observation: EnforcementObservation,
        memory: EnforcementMemory,
        now: Long,
        surfaceId: String,
    ): EnforcementAction {
        val last = memory.lastRedirectEpochMs[surfaceId]
        val withinCooldown = last != null && now - last < QuietTiming.REDIRECT_COOLDOWN_MS
        if (withinCooldown) {
            return sheetAction(observation, memory, surfaceId)
        }
        if (!observation.messagesFound) {
            return sheetAction(observation, memory, surfaceId)
        }
        return EnforcementAction.Redirect(surfaceId)
    }

    private fun sheetAction(
        observation: EnforcementObservation,
        memory: EnforcementMemory,
        surfaceId: String,
    ): EnforcementAction {
        val breakEnabled = memory.breaksRemainingToday > 0
        return EnforcementAction.ShowSheet(
            surfaceId = surfaceId,
            breakEnabled = breakEnabled,
            goToMessagesAvailable = observation.messagesFound,
        )
    }

    fun isInQuietHours(memory: EnforcementMemory, clock: Clock): Boolean {
        if (!memory.quietHoursEnabled) return false
        val zone = clock.zone()
        val time = Instant.ofEpochMilli(clock.millis()).atZone(zone).toLocalTime()
        val minutes = time.hour * 60 + time.minute
        val start = memory.quietHoursStartMinutes
        val end = memory.quietHoursEndMinutes
        // Window 22:00–07:00, end exclusive at 07:00
        return if (start > end) {
            minutes >= start || minutes < end
        } else {
            minutes >= start && minutes < end
        }
    }

    fun startBreak(memory: EnforcementMemory, clock: Clock): EnforcementMemory {
        if (memory.breaksRemainingToday <= 0) return memory
        val dayKey = LocalDate.ofInstant(Instant.ofEpochMilli(clock.millis()), clock.zone()).toString()
        val remaining = if (memory.breakDayKey != dayKey) memory.breakAllowancePerDay else memory.breaksRemainingToday
        if (remaining <= 0) return memory
        return memory.copy(
            breakUntilEpochMs = clock.millis() + QuietTiming.BREAK_DURATION_MS,
            breaksRemainingToday = remaining - 1,
            breakDayKey = dayKey,
            breakAllowancePerDay = memory.breakAllowancePerDay,
        )
    }

    fun endBreak(memory: EnforcementMemory): EnforcementMemory =
        memory.copy(breakUntilEpochMs = null)

    fun recordRedirect(memory: EnforcementMemory, surfaceId: String, clock: Clock): EnforcementMemory =
        memory.copy(
            lastRedirectEpochMs = memory.lastRedirectEpochMs + (surfaceId to clock.millis()),
            actionSuppressUntilEpochMs = clock.millis() + QuietTiming.ACTION_SUPPRESS_MS,
        )

    fun recordSheet(memory: EnforcementMemory, clock: Clock): EnforcementMemory =
        memory.copy(actionSuppressUntilEpochMs = clock.millis() + QuietTiming.ACTION_SUPPRESS_MS)

    fun grantPass(memory: EnforcementMemory, surfaceId: String, minutes: Int, clock: Clock): EnforcementMemory {
        val until = clock.millis() + minutes * 60_000L
        val updated = memory.passes.filterNot { it.surfaceId == surfaceId } + SurfacePass(surfaceId, until)
        return memory.copy(passes = updated)
    }
}
