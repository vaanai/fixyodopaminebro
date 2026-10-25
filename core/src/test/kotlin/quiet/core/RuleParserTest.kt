package quiet.core

import org.junit.Test
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

    @Test fun `keep areas parse and must name a known signal`() {
        val withKeep = minimal().replace(
            """{"id": "s", "label": "S", "kind": "VIDEO_FEED"}""",
            """{"id": "s", "label": "S", "kind": "VIDEO_FEED", "keep": {"signal": "a", "name": "Stories"}}""",
        )
        assertEquals("Stories", RuleParser.parse(withKeep).surface("s")?.keep?.name)
        assertFailsWith<RuleParseException> { RuleParser.parse(withKeep.replace("\"signal\": \"a\"", "\"signal\": \"nope\"")) }
    }

    @Test fun `instagram home keeps the stories row`() {
        assertEquals("storiesTray", Fixtures.rules("instagram").surface("home")?.keep?.signal)
    }

    @Test fun `auto skip must be a nav target`() {
        assertFailsWith<RuleParseException> { RuleParser.parse(minimal(extra = """, "autoSkipTarget": "inbox"""")) }
    }
}
