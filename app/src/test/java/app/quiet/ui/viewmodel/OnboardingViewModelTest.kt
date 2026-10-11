package app.quiet.ui.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class OnboardingViewModelTest {
    @Test
    fun serviceOffKeepsBanner() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = OnboardingViewModel(app)
        vm.refreshBanner()
        assertTrue(vm.state.value.showHealthBanner)
    }
}
