package app.quiet.data.policy

import org.junit.Assert.assertEquals
import org.junit.Test
import quiet.core.rules.Policy

class StoredPolicyJsonTest {
    @Test
    fun unknownPolicyString_doesNotThrow() {
        val map = StoredPolicyJson.parseMap("""{"instagram.reels":"NOT_A_REAL_POLICY"}""")
        assertEquals(Policy.ALLOW, map["instagram.reels"])
    }

    @Test
    fun parseValue_usesSurfaceDefaultWhenUnknown() {
        assertEquals(Policy.BLOCK, StoredPolicyJson.parseValue("bogus", Policy.BLOCK))
    }
}
