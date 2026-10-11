package app.quiet.service

import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import quiet.core.model.NodeInfo

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ConfirmingWalkClickTest {
    @Test
    fun clickWithoutHeldRootDoesNotInvokePerform() {
        val holder = ConfirmingWalkHolder()
        var performCalls = 0
        val clicked = clickMessagesOnHeldRoot(holder, NodeInfo(text = "Messages")) { _, _ ->
            performCalls++
            true
        }
        assertFalse(clicked)
        assertEquals(0, performCalls)
    }

    @Test
    fun clickUsesHeldRootWithoutFreshLookup() {
        val holder = ConfirmingWalkHolder()
        val root = AccessibilityNodeInfo.obtain()
        holder.heldRoot = root
        var performCalls = 0
        var rootSeen: AccessibilityNodeInfo? = null
        val clicked = clickMessagesOnHeldRoot(holder, NodeInfo(text = "Messages")) { r, _ ->
            performCalls++
            rootSeen = r
            true
        }
        assertTrue(clicked)
        assertEquals(1, performCalls)
        assertEquals(root, rootSeen)
        root.recycle()
        assertEquals(0, holder.freshRootLookups)
    }
}
