package app.quiet.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test
import quiet.core.rules.Policy

class PolicyLabelsTest {
    @Test
    fun openOnlyLabel_isOpenOnlyNotAllow() {
        assertEquals("Open-only", policySegmentLabel(Policy.OPEN_ONLY))
    }
}
