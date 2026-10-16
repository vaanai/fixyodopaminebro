package app.quiet.data.policy

import androidx.test.core.app.ApplicationProvider
import app.quiet.data.prefs.QuietPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HoldSecondsPolicyTest {
    private lateinit var preferences: QuietPreferences
    private lateinit var repository: PolicyRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        preferences = QuietPreferences(context)
        runBlocking { preferences.clearAll() }
        repository = PolicyRepository(preferences)
        runBlocking { PolicySnapshotSync.refresh(preferences) }
    }

    @Test
    fun holdSecondsWrite_updatesHolderWithoutReconnect() = runBlocking {
        repository.patchSettings { it.copy(holdSeconds = 5) }
        val holder = PolicySnapshotHolder.get()
        assertEquals(5, holder.holdSeconds)
    }

    @Test
    fun corruptHoldSeconds_coercedInSnapshot() = runBlocking {
        repository.patchSettings { it.copy(holdSeconds = 99) }
        val holder = PolicySnapshotHolder.get()
        assertEquals(5, holder.holdSeconds)
    }
}
