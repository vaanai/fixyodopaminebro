package quiet.core

import org.junit.Test
import quiet.core.rules.LaunchGuess
import quiet.core.rules.RuleParseException
import quiet.core.rules.RuleParser
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RuleParserTest {
    @Test fun `bundled rules parse`() {
        for (name in listOf("instagram", "tiktok", "youtube")) {
            val r = Fixtures.rules(name)
            assertEquals(name, r.app)
            assertTrue(r.rules.isNotEmpty())
            assertTrue(r.nav.isNotEmpty())
        }
    }

    private fun minimal(extra: String = "", signals: String = """"a": [{"id": "x"}]""", rules: String = """{"surface": "s", "all": ["a"]}""") = """
        {"schemaVersion": 2, "app": "t", "displayName": "T", "packages": ["p"],
         "signals": {$signals},
         "surfaces": [{"id": "s", "label": "S", "kind": "VIDEO_FEED"}],
         "rules": [$rules] $extra}
    """

    @Test fun `minimal rules parse`() {
        assertEquals("t", RuleParser.parse(minimal()).app)
    }

    @Test fun `unknown signal is rejected`() {
        assertFailsWith<RuleParseException> { RuleParser.parse(minimal(rules = """{"surface": "s", "all": ["nope"]}""")) }
    }

    @Test fun `typo in matcher key is rejected`() {
        assertFailsWith<RuleParseException> { RuleParser.parse(minimal(signals = """"a": [{"idd": "x"}]""")) }
    }

    @Test fun `matcher with only flags is rejected`() {
        assertFailsWith<RuleParseException> { RuleParser.parse(minimal(signals = """"a": [{"selected": true}]""")) }
    }

    @Test fun `wrong schema is rejected`() {
        assertFailsWith<RuleParseException> { RuleParser.parse(minimal().replace("\"schemaVersion\": 2", "\"schemaVersion\": 1")) }
    }

    @Test fun `open tops parse and must name a known signal`() {
        val withKeep = minimal().replace(
            """{"id": "s", "label": "S", "kind": "VIDEO_FEED"}""",
            """{"id": "s", "label": "S", "kind": "VIDEO_FEED", "openTop": {"signal": "a", "name": "Stories"}}""",
        )
        assertEquals("Stories", RuleParser.parse(withKeep).surface("s")?.openTop?.name)
        assertFailsWith<RuleParseException> { RuleParser.parse(withKeep.replace("\"signal\": \"a\"", "\"signal\": \"nope\"")) }
        assertFailsWith<RuleParseException> { RuleParser.parse(withKeep.replace("\"name\": \"Stories\"", "\"name\": \"Stories\", \"surface\": \"nope\"")) }
        assertFailsWith<RuleParseException> { RuleParser.parse(withKeep.replace("\"name\": \"Stories\"", "\"name\": \"Stories\", \"back\": \"nope\"")) }
    }

    @Test fun `instagram home stays open at the stories row`() {
        val top = Fixtures.rules("instagram").surface("home")?.openTop
        assertEquals("storiesTray", top?.signal)
        assertEquals("stories", top?.surface)
        assertEquals("home", top?.back)
    }

    @Test fun `launch guess parses and must name a known surface and signal`() {
        assertEquals(LaunchGuess("s", "a"), RuleParser.parse(minimal(extra = """, "launch": {"surface": "s", "signal": "a"}""")).launch)
        assertFailsWith<RuleParseException> { RuleParser.parse(minimal(extra = """, "launch": {"surface": "nope", "signal": "a"}""")) }
        assertFailsWith<RuleParseException> { RuleParser.parse(minimal(extra = """, "launch": {"surface": "s", "signal": "nope"}""")) }
        assertEquals(LaunchGuess("for_you", "videoActions"), Fixtures.rules("tiktok").launch)
    }

    @Test fun `auto skip must be a nav target`() {
        assertFailsWith<RuleParseException> { RuleParser.parse(minimal(extra = """, "autoSkipTarget": "inbox"""")) }
    }
}
