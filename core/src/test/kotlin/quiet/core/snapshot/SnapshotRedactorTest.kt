package quiet.core.snapshot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapshotRedactorTest {
    @Test
    fun vocabularySurvives() {
        assertEquals("Home", SnapshotRedactor.redactText("Home"))
        assertEquals("for you", SnapshotRedactor.redactText("for you"))
    }

    @Test
    fun foreignStringsRedacted() {
        assertEquals("<redacted len=7>", SnapshotRedactor.redactText("caption"))
        assertEquals("<redacted len=8>", SnapshotRedactor.redactText("Homework"))
        assertEquals("<redacted len=9>", SnapshotRedactor.redactText("Directors"))
    }

    @Test
    fun mixedTreeHasNoRawForeign() {
        val tree = SnapshotRedactor.redactNode(
            SnapshotNode(
                className = "View",
                bounds = "[0,0,1,1]",
                depth = 0,
                clickable = false,
                enabled = true,
                selected = false,
                viewId = null,
                text = "Home",
                contentDescription = "secret caption",
                children = listOf(
                    SnapshotNode(
                        className = "View",
                        bounds = "[0,0,1,1]",
                        depth = 1,
                        clickable = false,
                        enabled = true,
                        selected = false,
                        viewId = null,
                        text = "username",
                        contentDescription = null,
                    ),
                ),
            ),
        )
        assertTrue(SnapshotRedactor.collectForeignStrings(tree).isEmpty())
    }
}
