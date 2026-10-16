package app.quiet.data.policy

import app.quiet.data.prefs.QuietPreferences
import app.quiet.data.prefs.SettingsSnapshot

object PolicySnapshotSync {
    suspend fun refresh(preferences: QuietPreferences) {
        val snap = try {
            preferences.readSnapshot()
        } catch (_: Exception) {
            failOpenSnapshot()
        }
        publish(snap)
    }

    fun publish(snapshot: SettingsSnapshot) {
        PolicySnapshotHolder.update(RuntimeSnapshotMapper.fromSettings(snapshot))
    }

    private fun failOpenSnapshot(): SettingsSnapshot =
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
            holdSeconds = 3,
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
