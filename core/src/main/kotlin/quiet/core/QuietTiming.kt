package quiet.core

object QuietTiming {
    const val DEBOUNCE_MS = 250L
    const val STABILITY_GAP_MS = 300L
    const val WALK_TIME_CAP_MS = 30L
    const val WALK_NODE_CAP = 500
    const val REDIRECT_COOLDOWN_MS = 60_000L
    const val ACTION_SUPPRESS_MS = 3_000L
    const val RECHECK_DELAY_MS = 500L
    const val SHEET_TIMEOUT_MS = 20_000L
    const val SHEET_COOLDOWN_MS = 60_000L
    const val HEARTBEAT_INTERVAL_MS = 60_000L
    const val HEARTBEAT_STALE_MS = 120_000L
    const val BREAK_DURATION_MS = 5 * 60_000L
    const val DEFAULT_GATE_MINUTES = 10
    const val GATE_HOLD_MS = 3_000L
    const val EVENT_PRUNE_DAYS = 90
}
