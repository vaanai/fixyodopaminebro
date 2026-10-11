package quiet.core.rules

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorTest {
    @Test
    fun rejectsProtectedNonAllow() {
        val json = """
        {
          "schemaVersion": 1,
          "app": "test",
          "displayName": "Test",
          "packages": ["com.test"],
          "messagesTabLabels": ["Messages"],
          "verified": false,
          "surfaces": [
            {
              "id": "dm_inbox",
              "label": "Messages",
              "kind": "protected",
              "defaultPolicy": "BLOCK",
              "enforce": true,
              "match": { "selectedTab": ["Messages"] }
            }
          ]
        }
        """.trimIndent()
        val result = RuleParser.parseResource(json, "test.json")
        assertNull(result.first)
        assertNotNull(result.second)
    }

    @Test
    fun rejectsOpenOnlyOnFeed() {
        val json = """
        {
          "schemaVersion": 1,
          "app": "test",
          "displayName": "Test",
          "packages": ["com.test"],
          "messagesTabLabels": ["Messages"],
          "verified": false,
          "surfaces": [
            {
              "id": "feed",
              "label": "Feed",
              "kind": "feed",
              "defaultPolicy": "OPEN_ONLY",
              "enforce": true,
              "match": { "selectedTab": ["Home"] }
            }
          ]
        }
        """.trimIndent()
        val result = RuleParser.parseResource(json, "test.json")
        assertNull(result.first)
    }

    @Test
    fun rejectsLacksLabel() {
        val json = """
        {
          "schemaVersion": 1,
          "app": "test",
          "displayName": "Test",
          "packages": ["com.test"],
          "messagesTabLabels": ["Messages"],
          "verified": false,
          "surfaces": [
            {
              "id": "feed",
              "label": "Feed",
              "kind": "feed",
              "defaultPolicy": "BLOCK",
              "enforce": true,
              "match": { "lacksLabel": ["Home"] }
            }
          ]
        }
        """.trimIndent()
        val result = RuleParser.parseResource(json, "test.json")
        assertNull(result.first)
        assertTrue(result.second!!.contains("lacksLabel"))
    }

    @Test
    fun warnsOnUnverifiedWithoutThrowing() {
        val parsed = RuleParser.parseResource(readResource("rules/instagram.json"), "instagram.json")
        assertNotNull(parsed.first)
    }

    private fun readResource(path: String): String =
        ClassLoader.getSystemResource(path)?.readText() ?: error("missing")
}
