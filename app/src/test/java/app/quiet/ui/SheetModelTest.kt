package app.quiet.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import app.quiet.ui.sheets.InterventionSheetModel

class SheetModelTest {
    @Test
    fun interventionShowsMessagesOnlyWhenFound() {
        val withMessages = InterventionSheetModel("Reels", messagesFound = true, breakEnabled = true)
        assertTrue(withMessages.showGoToMessages)
        val without = InterventionSheetModel("Reels", messagesFound = false, breakEnabled = true)
        assertFalse(without.showGoToMessages)
    }

    @Test
    fun breakDisabledAtZero() {
        val model = InterventionSheetModel("Reels", messagesFound = true, breakEnabled = false)
        assertFalse(model.breakEnabled)
    }
}
