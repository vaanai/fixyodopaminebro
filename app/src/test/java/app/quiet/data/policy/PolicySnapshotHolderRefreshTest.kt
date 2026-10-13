package app.quiet.data.policy

import androidx.test.core.app.ApplicationProvider
import app.quiet.data.prefs.PresetIds
import app.quiet.data.prefs.QuietPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import quiet.core.rules.Policy

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PolicySnapshotHolderRefreshTest {
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
    fun surfacePolicyWrite_updatesHolderWithoutReconnect() = runBlocking {
        repository.applyPreset(PresetIds.BALANCED)
        repository.setSurfacePolicy("instagram.home_feed", Policy.GATE)
        val holder = PolicySnapshotHolder.get()
        assertEquals(Policy.GATE, holder.surfacePolicies["instagram.home_feed"])
    }

    @Test
    fun onboardingPresetWrite_visibleInHolderBeforeServiceConnect() = runBlocking {
        repository.applyPreset(PresetIds.STRICT)
        val holder = PolicySnapshotHolder.get()
        assertEquals(PresetIds.STRICT, holder.presetId)
        assertEquals(Policy.BLOCK, holder.surfacePolicies["instagram.reels"])
    }
}
