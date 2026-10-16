package app.quiet.data.policy

import app.quiet.data.prefs.QuietPreferences
import app.quiet.data.prefs.SettingsSnapshot
import org.json.JSONArray
import org.json.JSONObject
import quiet.core.enforce.EnforcementMemory
import quiet.core.enforce.Enforcer
import quiet.core.enforce.SurfacePass
class EnforcementStateRepository(
    private val preferences: QuietPreferences,
) {
    suspend fun persistMemory(memory: EnforcementMemory) {
        val snap = preferences.readSnapshot()
        val updated = snap.copy(
            passesJson = passesToJson(memory.passes),
            breaksRemainingToday = memory.breaksRemainingToday,
            breakDayKey = memory.breakDayKey,
            breakUntilEpochMs = memory.breakUntilEpochMs,
            breaksPerDay = memory.breakAllowancePerDay,
        )
        preferences.applySnapshot(updated)
        PolicySnapshotSync.publish(updated)
    }

    suspend fun grantGatePass(surfaceId: String, minutes: Int, memory: EnforcementMemory, nowMs: Long): EnforcementMemory {
        val clock = object : quiet.core.enforce.Clock {
            override fun millis(): Long = nowMs
        }
        val updated = Enforcer.grantPass(memory, surfaceId, minutes, clock)
        persistMemory(updated)
        return updated
    }

    suspend fun refreshBreakAllowanceIfNewDay(nowMs: Long): EnforcementMemory {
        val memory = memoryFromSnapshot(preferences.readSnapshot())
        val clock = object : quiet.core.enforce.Clock {
            override fun millis(): Long = nowMs
        }
        val updated = Enforcer.syncBreakDay(memory, clock)
        if (updated != memory) {
            persistMemory(updated)
        }
        return updated
    }

    suspend fun startBreak(memory: EnforcementMemory, nowMs: Long): EnforcementMemory {
        val clock = object : quiet.core.enforce.Clock {
            override fun millis(): Long = nowMs
        }
        val updated = Enforcer.startBreak(memory, clock)
        if (updated == memory) return memory
        persistMemory(updated)
        return updated
    }

    suspend fun endBreak(memory: EnforcementMemory): EnforcementMemory {
        val updated = Enforcer.endBreak(memory)
        persistMemory(updated)
        return updated
    }

    fun memoryFromHolder(): EnforcementMemory = memoryFromRuntime(PolicySnapshotHolder.get())

    companion object {
        fun memoryFromRuntime(runtime: RuntimePolicySnapshot): EnforcementMemory = EnforcementMemory(
            passes = runtime.passes.map { SurfacePass(it.first, it.second) },
            breakUntilEpochMs = runtime.breakUntilEpochMs,
            breaksRemainingToday = runtime.breaksRemainingToday,
            breakAllowancePerDay = runtime.breaksPerDay,
            breakDayKey = runtime.breakDayKey,
            quietHoursEnabled = runtime.quietHoursEnabled,
            quietHoursStartMinutes = runtime.quietHoursStartMinutes,
            quietHoursEndMinutes = runtime.quietHoursEndMinutes,
        )

        fun memoryFromSnapshot(snap: SettingsSnapshot): EnforcementMemory = EnforcementMemory(
            passes = RuntimeSnapshotMapper.parsePasses(snap.passesJson).map { SurfacePass(it.first, it.second) },
            breakUntilEpochMs = snap.breakUntilEpochMs,
            breaksRemainingToday = snap.breaksRemainingToday,
            breakAllowancePerDay = snap.breaksPerDay,
            breakDayKey = snap.breakDayKey,
            quietHoursEnabled = snap.quietHoursEnabled,
            quietHoursStartMinutes = snap.quietHoursStartMinutes,
            quietHoursEndMinutes = snap.quietHoursEndMinutes,
        )

        fun passesToJson(passes: List<SurfacePass>): String {
            val arr = JSONArray()
            passes.forEach { pass ->
                arr.put(
                    JSONObject()
                        .put("surfaceId", pass.surfaceId)
                        .put("until", pass.untilEpochMs),
                )
            }
            return arr.toString()
        }

        fun parsePasses(json: String): List<Pair<String, Long>> =
            RuntimeSnapshotMapper.parsePasses(json)
    }
}
