package app.quiet.data.policy

import app.quiet.data.prefs.PresetIds
import app.quiet.data.prefs.QuietPreferences
import app.quiet.data.prefs.SettingsSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import quiet.core.rules.Policy

class PolicyRepository(
    private val preferences: QuietPreferences,
) {
    suspend fun applyPreset(presetId: String) = withContext(Dispatchers.IO) {
        val policies = PresetDefinitions.policiesForPreset(presetId)
        val friends = PresetDefinitions.friendsReelsForPreset(presetId)
        val breaks = PresetDefinitions.breaksForPreset(presetId)
        val (qhOn, qhStart) = PresetDefinitions.quietHoursForPreset(presetId)
        val current = preferences.readSnapshot()
        preferences.applySnapshot(
            current.copy(
                presetId = presetId,
                quietHoursEnabled = qhOn,
                quietHoursStartMinutes = qhStart,
                quietHoursEndMinutes = 7 * 60,
                breaksPerDay = breaks,
                breaksRemainingToday = breaks,
                breakDayKey = "",
                surfacePoliciesJson = policies.toJson(),
                friendsReelsJson = friends.toJson(),
                settingsLoadFailed = false,
            ),
        )
    }

    suspend fun setSurfacePolicy(surfaceId: String, policy: Policy) = withContext(Dispatchers.IO) {
        val current = preferences.readSnapshot()
        val map = current.surfacePoliciesJson.fromJson().toMutableMap()
        map[surfaceId] = policy
        preferences.applySnapshot(
            current.copy(
                presetId = PresetIds.CUSTOM,
                surfacePoliciesJson = map.toJson(),
            ),
        )
    }

    suspend fun setFriendsReels(appSurfaceId: String, policy: Policy) = withContext(Dispatchers.IO) {
        val current = preferences.readSnapshot()
        val map = current.friendsReelsJson.fromJson().toMutableMap()
        val stored = if (policy == Policy.OPEN_ONLY) Policy.ALLOW else policy
        map[appSurfaceId] = stored
        preferences.applySnapshot(
            current.copy(
                presetId = PresetIds.CUSTOM,
                friendsReelsJson = map.toJson(),
            ),
        )
    }

    suspend fun patchSettings(transform: (SettingsSnapshot) -> SettingsSnapshot) = withContext(Dispatchers.IO) {
        val current = preferences.readSnapshot()
        preferences.applySnapshot(transform(current))
        PolicySnapshotSync.refresh(preferences)
    }

    suspend fun seedBalancedIfNeeded() {
        val snap = preferences.readSnapshot()
        if (snap.surfacePoliciesJson == "{}" || snap.settingsLoadFailed) {
            applyPreset(PresetIds.BALANCED)
        }
    }

    fun policyForSurface(snapshot: SettingsSnapshot, surfaceId: String, default: Policy): Policy {
        val friends = snapshot.friendsReelsJson.fromJson()
        if (friends.containsKey(surfaceId)) {
            return friends[surfaceId] ?: default
        }
        return snapshot.surfacePoliciesJson.fromJson()[surfaceId] ?: default
    }

    private fun Map<String, Policy>.toJson(): String {
        val obj = JSONObject()
        forEach { (k, v) -> obj.put(k, v.name) }
        return obj.toString()
    }

    private fun String.fromJson(): Map<String, Policy> {
        if (isBlank() || this == "{}") return emptyMap()
        val obj = JSONObject(this)
        val out = mutableMapOf<String, Policy>()
        obj.keys().forEach { key ->
            out[key] = Policy.valueOf(obj.getString(key))
        }
        return out
    }
}
