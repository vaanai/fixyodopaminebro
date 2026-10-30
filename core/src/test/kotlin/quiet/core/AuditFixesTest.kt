package quiet.core

import org.junit.Test
import quiet.core.detect.Detector
import quiet.core.settings.Defaults
import quiet.core.settings.Mode
import quiet.core.settings.Preset
import quiet.core.settings.Settings
import kotlin.test.assertEquals

class AuditFixesTest {
    private val ig = Fixtures.rules("instagram")
    private val IG = Fixtures.IG

    @Test fun `a friend's profile opened from Home is a profile, not Home`() {
        val tree = Screen(IG).apply {
            node(id = "$IG:id/profile_header_container", top = 200, bottom = 700)
            node(text = "sam posted a photo", top = 800, bottom = 900)
            Fixtures.igTabs(this, "feed_tab")
        }.build()
        assertEquals("profile", Detector.read(ig, tree).surface)
    }

    @Test fun `a friend's profile opened from Search is a profile, not Explore`() {
        val tree = Screen(IG).apply {
            node(id = "$IG:id/profile_header_actions_top_row", top = 200, bottom = 300)
            Fixtures.igTabs(this, "search_tab")
        }.build()
        assertEquals("profile", Detector.read(ig, tree).surface)
    }

    @Test fun `home and explore still read as themselves`() {
        assertEquals("home", Detector.read(ig, Fixtures.igHome()).surface)
        val explore = Screen(IG).apply {
            node(text = "a grid of things", top = 400, bottom = 500)
            Fixtures.igTabs(this, "search_tab")
        }.build()
        assertEquals("explore", Detector.read(ig, explore).surface)
        assertEquals("profile", Detector.read(ig, Fixtures.igProfile()).surface)
    }

    @Test fun `a preset keeps daily app limits and apps it doesn't cover`() {
        val before = Settings()
            .withApp("instagram") { it.copy(dailyLimitMinutes = 45, modes = it.modes + ("reels" to Mode.ALLOW)) }
            .withApp("snapchat") { it.copy(dailyLimitMinutes = 30) }
        val after = Defaults.preset(Preset.BALANCED, before)
        assertEquals(45, after.app("instagram").dailyLimitMinutes)
        assertEquals(Mode.BLOCK, after.modeFor("instagram", "reels"))
        assertEquals(30, after.app("snapchat").dailyLimitMinutes)
        assertEquals(20, after.budgetMinutes)
    }
}
