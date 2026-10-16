package app.quiet.data.policy

import androidx.test.core.app.ApplicationProvider
import app.quiet.QuietApplication
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import quiet.core.rules.Policy

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FriendsReelsPolicyTest {
    @Test
    fun friendsOpenOnlyStoredAsOpenOnly() {
        val app = ApplicationProvider.getApplicationContext<QuietApplication>()
        runBlocking {
            app.policyRepository.setFriendsReels("instagram.dm_shared_reel", Policy.OPEN_ONLY)
            val snap = app.preferences.readSnapshot()
            val map = org.json.JSONObject(snap.friendsReelsJson)
            assertEquals("OPEN_ONLY", map.getString("instagram.dm_shared_reel"))
        }
    }
}
