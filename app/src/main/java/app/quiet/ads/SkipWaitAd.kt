package app.quiet.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import app.quiet.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The one ad in Quiet: a rewarded ad that skips an unlock's wait. It only ever shows on Quiet's own
 * [SkipWaitActivity], never over another app and never from the background service.
 *
 * Nothing loads until someone opens that screen, so people who never choose an ad never load one.
 */
object SkipWaitAd {
    enum class State { IDLE, LOADING, READY, UNAVAILABLE }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var initialized = false
    private var ad: RewardedAd? = null
    private var loadedAt = 0L
    private val _state = MutableStateFlow(State.IDLE)
    val state: StateFlow<State> = _state.asStateFlow()

    /** Starts the SDK once and loads an ad if none is waiting. Call only after consent allows ads. */
    fun load(context: Context) {
        val app = context.applicationContext
        if (!initialized) {
            initialized = true
            scope.launch {
                // Google recommends initializing off the main thread.
                withContext(Dispatchers.IO) { MobileAds.initialize(app) {} }
                fetch(app)
            }
        } else {
            fetch(app)
        }
    }

    private fun fetch(context: Context) {
        dropIfExpired()
        if (ad != null) {
            _state.value = State.READY
            return
        }
        if (_state.value == State.LOADING) return
        _state.value = State.LOADING
        RewardedAd.load(
            context,
            BuildConfig.REWARDED_SKIP_WAIT_UNIT,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(loaded: RewardedAd) {
                    ad = loaded
                    loadedAt = SystemClock.elapsedRealtime()
                    _state.value = State.READY
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    // Offline, or no ad to show right now. The wait still works.
                    _state.value = State.UNAVAILABLE
                }
            },
        )
    }

    /**
     * Shows the ad. [onRewarded] runs only if it was watched to the end; closing it early changes
     * nothing. [onClosed] runs either way, once the ad is gone.
     */
    fun show(activity: Activity, onRewarded: () -> Unit, onClosed: () -> Unit) {
        dropIfExpired()
        val current = ad
        ad = null
        if (current == null) {
            _state.value = State.UNAVAILABLE
            onClosed()
            return
        }
        _state.value = State.IDLE
        var earned = false
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                if (earned) onRewarded()
                onClosed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                _state.value = State.UNAVAILABLE
                onClosed()
            }
        }
        current.show(activity) { earned = true }
    }

    /** A loaded rewarded ad can't be shown after about an hour. */
    private fun dropIfExpired() {
        if (ad != null && SystemClock.elapsedRealtime() - loadedAt > MAX_AGE_MS) ad = null
    }

    private const val MAX_AGE_MS = 55 * 60_000L
}
