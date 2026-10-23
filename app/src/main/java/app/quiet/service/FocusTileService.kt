package app.quiet.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import app.quiet.QuietApp

/** Quick Settings tile: tap to start an hour of focus, tap again to end it. */
class FocusTileService : TileService() {
    override fun onStartListening() {
        render()
    }

    override fun onClick() {
        val now = System.currentTimeMillis()
        QuietApp.store.updateSettings { s ->
            if (s.focusUntil > now) {
                if (s.strictMode) s else s.copy(focusUntil = 0L)
            } else {
                s.copy(focusUntil = now + 60 * 60_000L)
            }
        }
        QuietService.instance?.refresh()
        render()
    }

    private fun render() {
        val tile = qsTile ?: return
        val until = QuietApp.store.settings.value.focusUntil
        val on = until > System.currentTimeMillis()
        tile.state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "Focus"
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            tile.subtitle = if (on) "until " + java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(until)) else "1 hour"
        }
        tile.updateTile()
    }
}
