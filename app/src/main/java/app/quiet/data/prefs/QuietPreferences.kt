package app.quiet.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "quiet_prefs")

class QuietPreferences(private val context: Context) {
    private val preset = stringPreferencesKey("preset")
    private val quietHoursEnabled = booleanPreferencesKey("quiet_hours_enabled")
    private val quietHoursStart = intPreferencesKey("quiet_hours_start")
    private val quietHoursEnd = intPreferencesKey("quiet_hours_end")
    private val breaksPerDay = intPreferencesKey("breaks_per_day")
    private val breaksRemaining = intPreferencesKey("breaks_remaining")
    private val breakDayKey = stringPreferencesKey("break_day_key")
    private val breakUntil = longPreferencesKey("break_until")
    private val gateMinutes = intPreferencesKey("gate_minutes")
    private val holdSeconds = intPreferencesKey("hold_seconds")
    private val heartbeat = longPreferencesKey("heartbeat")
    private val unknownCount = intPreferencesKey("unknown_count")
    private val onboardingDone = booleanPreferencesKey("onboarding_done")
    private val appearance = stringPreferencesKey("appearance")
    private val surfacePolicies = stringPreferencesKey("surface_policies_json")
    private val friendsReels = stringPreferencesKey("friends_reels_json")
    private val passesJson = stringPreferencesKey("passes_json")
    private val settingsLoadFailed = booleanPreferencesKey("settings_load_failed")

    val snapshotFlow: Flow<SettingsSnapshot> = context.dataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs -> prefs.toSnapshot() }

    suspend fun readSnapshot(): SettingsSnapshot = snapshotFlow.first()

    suspend fun edit(block: suspend (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
    }

    suspend fun writeHeartbeat(epochMs: Long): Result<Unit> = try {
        context.dataStore.edit { it[heartbeat] = epochMs }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun incrementUnknown() {
        context.dataStore.edit { prefs ->
            val current = prefs[unknownCount] ?: 0
            prefs[unknownCount] = current + 1
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }

    private fun Preferences.toSnapshot(): SettingsSnapshot = SettingsSnapshot(
        presetId = this[preset] ?: PresetIds.BALANCED,
        quietHoursEnabled = this[quietHoursEnabled] ?: false,
        quietHoursStartMinutes = this[quietHoursStart] ?: 22 * 60,
        quietHoursEndMinutes = this[quietHoursEnd] ?: 7 * 60,
        breaksPerDay = this[breaksPerDay] ?: 3,
        breaksRemainingToday = this[breaksRemaining] ?: 3,
        breakDayKey = this[breakDayKey] ?: "",
        breakUntilEpochMs = this[breakUntil],
        gateMinutes = this[gateMinutes] ?: 10,
        holdSeconds = (this[holdSeconds] ?: 3).coerceIn(2, 5),
        heartbeatEpochMs = this[heartbeat],
        unknownSurfaceCount = this[unknownCount] ?: 0,
        onboardingComplete = this[onboardingDone] ?: false,
        appearance = this[appearance] ?: "system",
        surfacePoliciesJson = this[surfacePolicies] ?: "{}",
        friendsReelsJson = this[friendsReels] ?: "{}",
        passesJson = this[passesJson] ?: "[]",
        settingsLoadFailed = this[settingsLoadFailed] ?: false,
    )

    suspend fun applySnapshot(seed: SettingsSnapshot) {
        context.dataStore.edit { prefs ->
            prefs[preset] = seed.presetId
            prefs[quietHoursEnabled] = seed.quietHoursEnabled
            prefs[quietHoursStart] = seed.quietHoursStartMinutes
            prefs[quietHoursEnd] = seed.quietHoursEndMinutes
            prefs[breaksPerDay] = seed.breaksPerDay
            prefs[breaksRemaining] = seed.breaksRemainingToday
            prefs[breakDayKey] = seed.breakDayKey
            seed.breakUntilEpochMs?.let { prefs[breakUntil] = it } ?: prefs.remove(breakUntil)
            prefs[gateMinutes] = seed.gateMinutes
            prefs[holdSeconds] = seed.holdSeconds.coerceIn(2, 5)
            seed.heartbeatEpochMs?.let { prefs[heartbeat] = it }
            prefs[unknownCount] = seed.unknownSurfaceCount
            prefs[onboardingDone] = seed.onboardingComplete
            prefs[appearance] = seed.appearance
            prefs[surfacePolicies] = seed.surfacePoliciesJson
            prefs[friendsReels] = seed.friendsReelsJson
            prefs[passesJson] = seed.passesJson
            prefs[settingsLoadFailed] = seed.settingsLoadFailed
        }
    }

    companion object Keys {
        val PRESET = stringPreferencesKey("preset")
        val ONBOARDING = booleanPreferencesKey("onboarding_done")
    }
}

data class SettingsSnapshot(
    val presetId: String,
    val quietHoursEnabled: Boolean,
    val quietHoursStartMinutes: Int,
    val quietHoursEndMinutes: Int,
    val breaksPerDay: Int,
    val breaksRemainingToday: Int,
    val breakDayKey: String,
    val breakUntilEpochMs: Long?,
    val gateMinutes: Int,
    val holdSeconds: Int,
    val heartbeatEpochMs: Long?,
    val unknownSurfaceCount: Int,
    val onboardingComplete: Boolean,
    val appearance: String,
    val surfacePoliciesJson: String,
    val friendsReelsJson: String,
    val passesJson: String,
    val settingsLoadFailed: Boolean,
)

object PresetIds {
    const val STRICT = "strict"
    const val BALANCED = "balanced"
    const val LIGHT = "light"
    const val CUSTOM = "custom"
}
