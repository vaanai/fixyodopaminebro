package quiet.core.settings

/**
 * The settings lock. Chosen once during setup: making Quiet less strict ([Strictness.isLoosening])
 * means starting a wait of [Settings.lockSeconds]. Once it ends, settings stay open for [OPEN_MS] so
 * several changes can be made, then lock again. Tightening never waits.
 */
object SettingsLock {
    const val OPEN_MS = 5 * 60_000L

    /** Wait lengths offered during setup. Settings also offer no lock and longer ones. */
    val SETUP_CHOICES = listOf(60, 120, 180, 240, 300)
    val ALL_CHOICES = listOf(0, 60, 120, 180, 240, 300, 600, 900, 1800)

    sealed interface State {
        /** No lock chosen: every change applies straight away. */
        data object Off : State

        data object Locked : State

        data class Waiting(val opensAt: Long) : State

        data class Open(val closesAt: Long) : State
    }

    fun state(s: Settings, now: Long): State {
        if (s.lockSeconds <= 0) return State.Off
        val opensAt = s.lockOpensAt
        if (opensAt <= 0) return State.Locked
        // A wait can't be longer than the lock: an opening time further out than that means the clock
        // went backwards, so start over rather than wait forever.
        if (opensAt - now > s.lockSeconds * 1000L) return State.Locked
        return when {
            now < opensAt -> State.Waiting(opensAt)
            now < opensAt + OPEN_MS -> State.Open(opensAt + OPEN_MS)
            else -> State.Locked
        }
    }

    /** Whether going from [old] to [new] can be saved right now, without a wait. */
    fun allows(old: Settings, new: Settings, now: Long): Boolean {
        if (!Strictness.isLoosening(old, new)) return true
        val st = state(old, now)
        return st is State.Off || st is State.Open
    }

    /** Starts the wait. Does nothing if one is already running or settings are already open. */
    fun startWait(s: Settings, now: Long): Settings = when (state(s, now)) {
        is State.Locked -> s.copy(lockOpensAt = now + s.lockSeconds * 1000L)
        else -> s
    }

    /** Locks again straight away. */
    fun relock(s: Settings): Settings = s.copy(lockOpensAt = 0L)

    fun formatWait(seconds: Int): String = when {
        seconds <= 0 -> "No lock"
        seconds % 60 == 0 -> "${seconds / 60} min"
        else -> "${seconds}s"
    }
}
