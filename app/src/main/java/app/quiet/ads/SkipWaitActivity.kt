package app.quiet.ads

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.quiet.QuietApp
import app.quiet.service.QuietService
import app.quiet.ui.components.PrimaryButton
import app.quiet.ui.components.SecondaryButton
import app.quiet.ui.components.SmallButton
import app.quiet.ui.theme.Q
import app.quiet.ui.theme.QuietTheme
import kotlinx.coroutines.delay

/**
 * Quiet's own screen for the optional ad that skips an unlock's wait. The block screen and the Today
 * screen open it only when someone taps "Skip the wait". It says up front that it's one ad, and
 * waiting always stays the other way through.
 */
class SkipWaitActivity : ComponentActivity() {
    private var app = ""
    private var returnPackage: String? = null
    private val privacyOptions = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = intent.getStringExtra(EXTRA_APP).orEmpty()
        returnPackage = intent.getStringExtra(EXTRA_RETURN_PACKAGE)
        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: app
        val fromBlockScreen = returnPackage != null
        privacyOptions.value = AdConsent.privacyOptionsRequired(this)
        AdConsent.gather(this) {
            privacyOptions.value = AdConsent.privacyOptionsRequired(this)
            SkipWaitAd.load(this)
        }
        setContent {
            val settings by QuietApp.store.settings.collectAsStateWithLifecycle()
            QuietTheme(settings.accent) {
                SkipWaitScreen(
                    appName = appName,
                    fromBlockScreen = fromBlockScreen,
                    privacyOptions = privacyOptions.value,
                    onWatch = { watch() },
                    onWait = { done() },
                    onPrivacyOptions = { AdConsent.showPrivacyOptions(this) },
                )
            }
        }
    }

    private fun watch() {
        var earned = false
        SkipWaitAd.show(
            this,
            onRewarded = {
                earned = true
                QuietApp.store.mutateToday { it.copy(adsWatched = it.adsWatched + 1) }
                // From the block screen, its next showing goes straight to the unlock choices. From the
                // Today screen the caller unlocks on RESULT_OK.
                if (returnPackage != null) QuietService.instance?.skipWait(app)
                setResult(Activity.RESULT_OK)
            },
            onClosed = {
                // Watched to the end: back to the app. Closed early: stay here with a fresh ad, so
                // waiting instead is one tap away.
                if (earned) done() else SkipWaitAd.load(this)
            },
        )
    }

    private fun done() {
        // Back to the app the block screen was on. Finishing usually does that by itself; the launch
        // intent makes sure, and brings the app's task back as it was.
        returnPackage?.let { pkg ->
            packageManager.getLaunchIntentForPackage(pkg)?.let { runCatching { startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
        }
        finish()
    }

    companion object {
        private const val EXTRA_APP = "app"
        private const val EXTRA_APP_NAME = "appName"
        private const val EXTRA_RETURN_PACKAGE = "returnPackage"
        private const val STARTING_MS = 3_000L

        @Volatile
        private var startingUntil = 0L

        /**
         * The block screen is handing over to this screen. For a moment, before its window is in front,
         * the service must not draw the cover again over the way out.
         */
        val isStarting: Boolean get() = SystemClock.elapsedRealtime() < startingUntil

        fun starting() {
            startingUntil = SystemClock.elapsedRealtime() + STARTING_MS
        }

        /** [returnPackage] is the app to go back to afterwards; null when opened from inside Quiet. */
        fun intent(context: Context, app: String, appName: String, returnPackage: String?): Intent =
            Intent(context, SkipWaitActivity::class.java)
                .putExtra(EXTRA_APP, app)
                .putExtra(EXTRA_APP_NAME, appName)
                .putExtra(EXTRA_RETURN_PACKAGE, returnPackage)
    }
}

@Composable
private fun SkipWaitScreen(
    appName: String,
    fromBlockScreen: Boolean,
    privacyOptions: Boolean,
    onWatch: () -> Unit,
    onWait: () -> Unit,
    onPrivacyOptions: () -> Unit,
) {
    val c = Q.colors
    val state by SkipWaitAd.state.collectAsStateWithLifecycle()
    // Consent or loading can stall offline. After a while, say so rather than spin forever.
    var gaveUp by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(LOAD_PATIENCE_MS)
        gaveUp = true
    }
    val unavailable = state == SkipWaitAd.State.UNAVAILABLE || (gaveUp && state != SkipWaitAd.State.READY)
    Box(Modifier.fillMaxSize().background(c.bg).padding(horizontal = 28.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth().widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Skip the wait", style = MaterialTheme.typography.headlineLarge, color = c.text, textAlign = TextAlign.Center)
            Text(
                if (fromBlockScreen) {
                    "Watch 1 short ad and the wait is over. You still pick your unlock for $appName, and it still uses one of today's unlocks."
                } else {
                    "Watch 1 short ad and $appName unlocks straight away. It still uses one of today's unlocks."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = c.textDim,
                textAlign = TextAlign.Center,
            )
            Text(
                if (unavailable) "No ad is ready right now. Waiting works just as well."
                else "Ads only ever show here, when you ask for one. Rather not? Waiting works just as well.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textFaint,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            if (!unavailable) {
                PrimaryButton(
                    if (state == SkipWaitAd.State.READY) "Watch 1 ad" else "Loading the ad…",
                    Modifier.fillMaxWidth(),
                    enabled = state == SkipWaitAd.State.READY,
                    onClick = onWatch,
                )
            }
            SecondaryButton(if (unavailable) "Back to waiting" else "Wait instead", Modifier.fillMaxWidth(), onClick = onWait)
            if (privacyOptions) {
                Spacer(Modifier.height(4.dp))
                SmallButton("Ad privacy choices", onClick = onPrivacyOptions)
            }
        }
    }
}

private const val LOAD_PATIENCE_MS = 12_000L
