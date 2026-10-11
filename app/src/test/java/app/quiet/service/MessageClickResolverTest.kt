package app.quiet.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import quiet.core.model.NodeInfo

class MessageClickResolverTest {
    @Test
    fun ancestorWithin4xAllowsClick() {
        val label = NodeInfo(text = "Messages", boundsLeft = 10, boundsTop = 10, boundsRight = 20, boundsBottom = 20)
        val tab = NodeInfo(
            text = "Messages",
            isClickable = true,
            boundsLeft = 8,
            boundsTop = 8,
            boundsRight = 28,
            boundsBottom = 28,
        )
        assertTrue(MessageClickResolver.planClickForTest(label, listOf(label, tab)))
    }

    @Test
    fun ancestorTooLargeBlocksClick() {
        val label = NodeInfo(text = "Messages", boundsLeft = 10, boundsTop = 10, boundsRight = 20, boundsBottom = 20)
        val huge = NodeInfo(
            text = "Messages",
            isClickable = true,
            boundsLeft = 0,
            boundsTop = 0,
            boundsRight = 200,
            boundsBottom = 200,
        )
        assertFalse(MessageClickResolver.planClickForTest(label, listOf(label, huge)))
    }
}
