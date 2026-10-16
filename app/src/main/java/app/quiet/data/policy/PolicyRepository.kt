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
        val next = current.copy(
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
        )
        preferences.applySnapshot(next)
        PolicySnapshotSync.publish(next)
    }

    suspend fun setSurfacePolicy(surfaceId: String, policy: Policy) = withContext(Dispatchers.IO) {
        val current = preferences.readSnapshot()
        val map = current.surfacePoliciesJson.fromJson().toMutableMap()
        map[surfaceId] = policy
        val next = current.copy(
            presetId = PresetIds.CUSTOM,
            surfacePoliciesJson = map.toJson(),
        )
        preferences.applySnapshot(next)
        PolicySnapshotSync.publish(next)
    }

    suspend fun setFriendsReels(appSurfaceId: String, policy: Policy) = withContext(Dispatchers.IO) {
        val current = preferences.readSnapshot()
        val map = current.friendsReelsJson.fromJson().toMutableMap()
        map[appSurfaceId] = policy
        val next = current.copy(
            presetId = PresetIds.CUSTOM,
            friendsReelsJson = map.toJson(),
        )
        preferences.applySnapshot(next)
        PolicySnapshotSync.publish(next)
    }

    suspend fun patchSettings(transform: (SettingsSnapshot) -> SettingsSnapshot) = withContext(Dispatchers.IO) {
        val current = preferences.readSnapshot()
        val next = transform(current)
        preferences.applySnapshot(next)
        PolicySnapshotSync.publish(next)
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
            val stored = friends[surfaceId] ?: default
            return if (stored == Policy.ALLOW) Policy.OPEN_ONLY else stored
        }
        return snapshot.surfacePoliciesJson.fromJson()[surfaceId] ?: default
    }

    private fun Map<String, Policy>.toJson(): String {
        val obj = JSONObject()
        forEach { (k, v) -> obj.put(k, v.name) }
        return obj.toString()
    }

    private fun String.fromJson(): Map<String, Policy> = StoredPolicyJson.parseMap(this)
}
