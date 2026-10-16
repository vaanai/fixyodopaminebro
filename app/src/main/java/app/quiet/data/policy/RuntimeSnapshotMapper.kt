package app.quiet.data.policy

import app.quiet.data.prefs.SettingsSnapshot
import org.json.JSONArray
import quiet.core.rules.Policy

object RuntimeSnapshotMapper {
    fun fromSettings(snapshot: SettingsSnapshot): RuntimePolicySnapshot =
        RuntimePolicySnapshot(
            loaded = !snapshot.settingsLoadFailed && snapshot.surfacePoliciesJson != "{}",
            settingsLoadFailed = snapshot.settingsLoadFailed,
            presetId = snapshot.presetId,
            surfacePolicies = StoredPolicyJson.parseMap(snapshot.surfacePoliciesJson),
            friendsReels = StoredPolicyJson.parseMap(snapshot.friendsReelsJson),
            quietHoursEnabled = snapshot.quietHoursEnabled,
            quietHoursStartMinutes = snapshot.quietHoursStartMinutes,
            quietHoursEndMinutes = snapshot.quietHoursEndMinutes,
            breaksPerDay = snapshot.breaksPerDay,
            breaksRemainingToday = snapshot.breaksRemainingToday,
            breakDayKey = snapshot.breakDayKey,
            breakUntilEpochMs = snapshot.breakUntilEpochMs,
            gateMinutes = snapshot.gateMinutes,
            holdSeconds = snapshot.holdSeconds.coerceIn(2, 5),
            passes = parsePasses(snapshot.passesJson),
        )

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
