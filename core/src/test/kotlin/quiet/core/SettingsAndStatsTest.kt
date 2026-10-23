package quiet.core

import org.junit.Test
import quiet.core.settings.Accent
import quiet.core.settings.Defaults
import quiet.core.settings.FocusWindow
import quiet.core.settings.Mode
import quiet.core.settings.Origin
import quiet.core.settings.Preset
import quiet.core.settings.Settings
import quiet.core.settings.SettingsCodec
import quiet.core.stats.DayStats
import quiet.core.stats.StatsCodec
import quiet.core.stats.StatsMath
import quiet.core.stats.TickTarget
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.test.assertEquals

class SettingsAndStatsTest {
    @Test fun `settings round trip`() {
        val s = Defaults.preset(Preset.STRICT).copy(
            focusWindows = listOf(FocusWindow("a", "School", setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), 480, 900)),
            accent = Accent.ROSE,
            onboarded = true,
        ).withApp("instagram") { it.copy(viewerSwipes = it.viewerSwipes + (Origin.MESSAGES to 2), dailyLimitMinutes = 45) }
        assertEquals(s, SettingsCodec.decode(SettingsCodec.encode(s)))
    }

    @Test fun `garbage settings fall back to defaults`() {
        assertEquals(Settings(), SettingsCodec.decodeOrDefault("{not json"))
        assertEquals(Settings(), SettingsCodec.decodeOrDefault(null))
    }

    @Test fun `defaults block reels and for you but allow home and friends`() {
        val s = Settings()
        assertEquals(Mode.BLOCK, s.modeFor("instagram", "reels"))
        assertEquals(Mode.ALLOW, s.modeFor("instagram", "home"))
        assertEquals(Mode.BLOCK, s.modeFor("tiktok", "for_you"))
        assertEquals(Mode.ALLOW, s.modeFor("tiktok", "friends"))
        assertEquals(Mode.ALLOW, s.modeFor("instagram", "unknown_surface"))
        assertEquals(0, s.viewerSwipesFor("instagram", Origin.MESSAGES))
    }

    @Test fun `ticks add up per surface and hour`() {
        var d = DayStats("2026-10-22")
        val t = TickTarget("com.instagram.android", true, "instagram.reels", isFeed = true, isViewer = false, chargesBudget = true)
        d = StatsMath.addTime(d, t, 5, 13)
        d = StatsMath.addTime(d, t, 5, 13)
        assertEquals(10, d.appSeconds["com.instagram.android"])
        assertEquals(10, d.surfaceSeconds["instagram.reels"])
        assertEquals(10, d.feedSeconds)
        assertEquals(10, d.budgetSeconds)
        assertEquals(10, d.hourShortForm[13])
        assertEquals(10, d.secondsForApp("instagram"))
    }

    @Test fun `untracked apps are not counted`() {
        val d = StatsMath.addTime(DayStats("x"), TickTarget("com.android.chrome", false, null, false, false, false), 5, 1)
        assertEquals(0, d.totalAppSeconds)
    }

    @Test fun `stats round trip`() {
        val d = DayStats("2026-10-22", appSeconds = mapOf("a" to 3), feedSeconds = 9, blocks = 2,
            hourShortForm = List(24) { it }, videos = mapOf("instagram" to 4))
        assertEquals(mapOf(d.date to d), StatsCodec.decode(StatsCodec.encode(mapOf(d.date to d))))
    }

    @Test fun `streak counts good days back from today`() {
        val today = LocalDate.of(2026, 10, 22)
        val h = mapOf(
            "2026-10-18" to DayStats("2026-10-18", feedSeconds = 5000),
            "2026-10-19" to DayStats("2026-10-19", feedSeconds = 60),
            "2026-10-21" to DayStats("2026-10-21", feedSeconds = 0),
            "2026-10-22" to DayStats("2026-10-22", feedSeconds = 30),
        )
        assertEquals(4, StatsMath.streak(h, today, 20))
        val badToday = h + ("2026-10-22" to DayStats("2026-10-22", feedSeconds = 5000))
        assertEquals(3, StatsMath.streak(badToday, today, 20))
    }
}
