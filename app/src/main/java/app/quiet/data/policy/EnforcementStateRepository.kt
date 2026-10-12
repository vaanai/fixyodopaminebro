package app.quiet.data.policy

import app.quiet.data.prefs.QuietPreferences
import app.quiet.data.prefs.SettingsSnapshot
import org.json.JSONArray
import org.json.JSONObject
import quiet.core.enforce.EnforcementMemory
import quiet.core.enforce.Enforcer
import quiet.core.enforce.SurfacePass
import quiet.core.rules.Policy

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
        PolicySnapshotHolder.update(updated.toRuntimeSnapshot())
    }

    suspend fun grantGatePass(surfaceId: String, minutes: Int, memory: EnforcementMemory, nowMs: Long): EnforcementMemory {
        val clock = object : quiet.core.enforce.Clock {
            override fun millis(): Long = nowMs
        }
        val updated = Enforcer.grantPass(memory, surfaceId, minutes, clock)
        persistMemory(updated)
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
            passes = parsePasses(snap.passesJson).map { SurfacePass(it.first, it.second) },
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

        fun parsePasses(json: String): List<Pair<String, Long>> {
            if (json.isBlank() || json == "[]") return emptyList()
            val arr = JSONArray(json)
            val out = mutableListOf<Pair<String, Long>>()
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                out.add(item.getString("surfaceId") to item.getLong("until"))
            }
            return out
        }

    }
}

private fun SettingsSnapshot.toRuntimeSnapshot(): RuntimePolicySnapshot =
    RuntimePolicySnapshot(
        loaded = !settingsLoadFailed && surfacePoliciesJson != "{}",
        settingsLoadFailed = settingsLoadFailed,
        presetId = presetId,
        surfacePolicies = parsePolicyMap(surfacePoliciesJson),
        friendsReels = parsePolicyMap(friendsReelsJson),
        quietHoursEnabled = quietHoursEnabled,
        quietHoursStartMinutes = quietHoursStartMinutes,
        quietHoursEndMinutes = quietHoursEndMinutes,
        breaksPerDay = breaksPerDay,
        breaksRemainingToday = breaksRemainingToday,
        breakDayKey = breakDayKey,
        breakUntilEpochMs = breakUntilEpochMs,
        gateMinutes = gateMinutes,
        gateHoldSeconds = gateHoldSeconds,
        passes = EnforcementStateRepository.parsePasses(passesJson),
    )

private fun parsePolicyMap(json: String): Map<String, Policy> {
    if (json.isBlank() || json == "{}") return emptyMap()
    val obj = JSONObject(json)
    return obj.keys().asSequence().associateWith { Policy.valueOf(obj.getString(it)) }
}
