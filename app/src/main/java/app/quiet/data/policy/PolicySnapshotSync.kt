package app.quiet.data.policy

import app.quiet.data.prefs.QuietPreferences
import app.quiet.data.prefs.SettingsSnapshot
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import quiet.core.rules.Policy

object PolicySnapshotSync {
    suspend fun refresh(preferences: QuietPreferences) {
        val snap = try {
            preferences.readSnapshot()
        } catch (_: Exception) {
            SettingsSnapshot(
                presetId = "balanced",
                quietHoursEnabled = false,
                quietHoursStartMinutes = 22 * 60,
                quietHoursEndMinutes = 7 * 60,
                breaksPerDay = 3,
                breaksRemainingToday = 3,
                breakDayKey = "",
                breakUntilEpochMs = null,
                gateMinutes = 10,
                heartbeatEpochMs = null,
                unknownSurfaceCount = 0,
                onboardingComplete = false,
                appearance = "system",
                surfacePoliciesJson = "{}",
                friendsReelsJson = "{}",
                passesJson = "[]",
                settingsLoadFailed = true,
            )
        }
        PolicySnapshotHolder.update(snap.toRuntime())
    }

    private fun SettingsSnapshot.toRuntime(): RuntimePolicySnapshot {
        val policies = surfacePoliciesJson.parsePolicies()
        val friends = friendsReelsJson.parsePolicies()
        val passes = passesJson.parsePasses()
        return RuntimePolicySnapshot(
            loaded = !settingsLoadFailed && surfacePoliciesJson != "{}",
            settingsLoadFailed = settingsLoadFailed,
            presetId = presetId,
            surfacePolicies = policies,
            friendsReels = friends,
            quietHoursEnabled = quietHoursEnabled,
            quietHoursStartMinutes = quietHoursStartMinutes,
            quietHoursEndMinutes = quietHoursEndMinutes,
            breaksPerDay = breaksPerDay,
            breaksRemainingToday = breaksRemainingToday,
            breakDayKey = breakDayKey,
            breakUntilEpochMs = breakUntilEpochMs,
            gateMinutes = gateMinutes,
            passes = passes,
        )
    }

    private fun String.parsePolicies(): Map<String, Policy> {
        if (isBlank() || this == "{}") return emptyMap()
        val obj = org.json.JSONObject(this)
        return obj.keys().asSequence().associateWith { Policy.valueOf(obj.getString(it)) }
    }

    private fun String.parsePasses(): List<Pair<String, Long>> {
        if (isBlank() || this == "[]") return emptyList()
        val arr = JSONArray(this)
        val out = mutableListOf<Pair<String, Long>>()
        for (i in 0 until arr.length()) {
            val item = arr.getJSONObject(i)
            out.add(item.getString("surfaceId") to item.getLong("until"))
        }
        return out
    }
}
