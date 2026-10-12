package app.quiet.ui.copy

import app.quiet.data.db.EnforcementEventEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object RecentEventFormatter {
    private val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

    fun formatLine(event: EnforcementEventEntity, gateMinutes: Int): Pair<String, String> {
        val app = displayApp(event.appId)
        val surface = displaySurface(event.surfaceId)
        val action = actionLabel(event.action, gateMinutes)
        val time = timeFormatter.format(
            Instant.ofEpochMilli(event.timestamp).atZone(ZoneId.systemDefault()),
        )
        return ("$app · $surface") to ("$action · $time")
    }

    private fun displayApp(appId: String): String = when (appId) {
        "instagram" -> "Instagram"
        "tiktok" -> "TikTok"
        else -> appId.replaceFirstChar { it.uppercase() }
    }

    private fun displaySurface(surfaceId: String): String {
        val raw = surfaceId.substringAfterLast('.', surfaceId)
        return when (raw) {
            "home_feed" -> "Home feed"
            "reels" -> "Reels"
            "for_you" -> "For You"
            "following" -> "Following"
            else -> raw.replace('_', ' ').replaceFirstChar { it.uppercase() }
        }
    }

    fun actionLabel(action: String, gateMinutes: Int): String = when (action) {
        "REDIRECTED" -> "Sent to Messages"
        "SHEET_SHOWN" -> "Sheet shown"
        "GATE_SHOWN" -> "Sheet shown"
        "GATE_CONFIRMED" -> "Opened for $gateMinutes min"
        "GATE_CANCELLED" -> "Hold cancelled"
        "BREAK_STARTED" -> "Break started"
        "SUPPRESSED" -> "Left alone"
        "SWIPE_BLOCKED" -> "Swipe paused"
        "UNKNOWN_SURFACE" -> "Unrecognized screen"
        else -> action.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
    }
}
