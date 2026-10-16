package app.quiet.data.policy

import app.quiet.data.prefs.SettingsSnapshot
import org.junit.Assert.assertTrue
import org.junit.Test
import quiet.core.rules.RuleParser

class AppSummaryBuilderTest {
    @Test
    fun instagramSummaryIsHonestAboutDetection() {
        val json = javaClass.classLoader!!.getResource("rules/instagram.json")!!.readText()
        val rules = RuleParser.parseResource(json, "instagram.json").first!!
        val snap = SettingsSnapshot(
            presetId = "balanced",
            quietHoursEnabled = false,
            quietHoursStartMinutes = 0,
            quietHoursEndMinutes = 0,
            breaksPerDay = 3,
            breaksRemainingToday = 3,
            breakDayKey = "",
            breakUntilEpochMs = null,
            gateMinutes = 10,
            holdSeconds = 3,
            heartbeatEpochMs = null,
            unknownSurfaceCount = 0,
            onboardingComplete = true,
            appearance = "system",
            surfacePoliciesJson = "{}",
            friendsReelsJson = "{}",
            passesJson = "[]",
            settingsLoadFailed = false,
        )
        val summary = AppSummaryBuilder.build(rules, snap)
        assertTrue(summary.contains("Home feed blocked"))
        assertTrue(summary.contains("Other surfaces not detected yet"))
        assertTrue(!summary.contains("stories", ignoreCase = true))
        assertTrue(!summary.contains("explore", ignoreCase = true))
    }
}
