package app.quiet.data.policy

import quiet.core.rules.Policy
import java.util.concurrent.atomic.AtomicReference

data class RuntimePolicySnapshot(
    val loaded: Boolean,
    val settingsLoadFailed: Boolean,
    val presetId: String,
    val surfacePolicies: Map<String, Policy>,
    val friendsReels: Map<String, Policy>,
    val quietHoursEnabled: Boolean,
    val quietHoursStartMinutes: Int,
    val quietHoursEndMinutes: Int,
    val breaksPerDay: Int,
    val breaksRemainingToday: Int,
    val breakDayKey: String,
    val breakUntilEpochMs: Long?,
    val gateMinutes: Int,
    val holdSeconds: Int,
    val passes: List<Pair<String, Long>>,
)

object PolicySnapshotHolder {
    private val ref = AtomicReference(
        RuntimePolicySnapshot(
            loaded = false,
            settingsLoadFailed = false,
            presetId = "balanced",
            surfacePolicies = PresetDefinitions.policiesForPreset("balanced"),
            friendsReels = PresetDefinitions.friendsReelsForPreset("balanced"),
            quietHoursEnabled = false,
            quietHoursStartMinutes = 22 * 60,
            quietHoursEndMinutes = 7 * 60,
            breaksPerDay = 3,
            breaksRemainingToday = 3,
            breakDayKey = "",
            breakUntilEpochMs = null,
            gateMinutes = 10,
            holdSeconds = 3,
            passes = emptyList(),
        ),
    )

    fun get(): RuntimePolicySnapshot = ref.get()

    fun update(snapshot: RuntimePolicySnapshot) {
        ref.set(snapshot)
    }
}
