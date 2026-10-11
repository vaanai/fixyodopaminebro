package quiet.core.enforce

import quiet.core.rules.Policy
import quiet.core.rules.SurfaceKind

data class EnforcementObservation(
    val appId: String,
    val surfaceId: String?,
    val kind: SurfaceKind?,
    val policy: Policy?,
    val enforce: Boolean,
    val messagesFound: Boolean,
    val walkTruncated: Boolean,
    val matchedSelectedTab: Boolean,
)

sealed class EnforcementAction {
    data object None : EnforcementAction()
    data class Redirect(val surfaceId: String) : EnforcementAction()
    data object ShowGate : EnforcementAction()
    data class ShowSheet(
        val surfaceId: String,
        val breakEnabled: Boolean,
        val goToMessagesAvailable: Boolean,
    ) : EnforcementAction()
}

data class SurfacePass(val surfaceId: String, val untilEpochMs: Long)

data class EnforcementMemory(
    val passes: List<SurfacePass> = emptyList(),
    val breakUntilEpochMs: Long? = null,
    val breaksRemainingToday: Int = 3,
    val breakAllowancePerDay: Int = 3,
    val breakDayKey: String = "",
    val quietHoursEnabled: Boolean = false,
    val quietHoursStartMinutes: Int = 22 * 60,
    val quietHoursEndMinutes: Int = 7 * 60,
    val lastRedirectEpochMs: Map<String, Long> = emptyMap(),
    val actionSuppressUntilEpochMs: Long = 0L,
    val sheetCooldownUntil: Map<String, Long> = emptyMap(),
)

data class EffectivePolicy(val policy: Policy, val enforce: Boolean)
