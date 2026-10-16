package app.quiet.data.policy

import androidx.test.core.app.ApplicationProvider
import app.quiet.data.prefs.QuietPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class OnboardingFinishPublishTest {
    private lateinit var repository: PolicyRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val prefs = QuietPreferences(context)
        runBlocking { prefs.clearAll() }
        repository = PolicyRepository(prefs)
    }

    @Test
    fun finishOnboarding_publishesCompleteFlag() = runBlocking {
        repository.patchSettings { it.copy(onboardingComplete = true) }
        assertTrue(PolicySnapshotHolder.get().loaded || PolicySnapshotHolder.get().presetId.isNotBlank())
        val snap = QuietPreferences(ApplicationProvider.getApplicationContext()).readSnapshot()
        assertTrue(snap.onboardingComplete)
    }
}
