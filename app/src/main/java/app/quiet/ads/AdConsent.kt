package app.quiet.ads

import android.app.Activity
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Google's consent form (UMP). People in the UK, EEA and Switzerland get asked about ads before
 * any ad loads; everyone else goes straight through. The form itself is set up in AdMob under
 * Privacy & messaging.
 */
object AdConsent {
    /** Asks for consent if this person needs to be asked, then calls [onCanRequestAds] once ads may load. */
    fun gather(activity: Activity, onCanRequestAds: () -> Unit) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()
        var started = false
        val startOnce = {
            if (!started && info.canRequestAds()) {
                started = true
                onCanRequestAds()
            }
        }
        info.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { startOnce() }
            },
            { startOnce() },
        )
        // Consent from an earlier launch still counts while the update runs.
        startOnce()
    }

    /** True when the person must be able to change their ad choices from inside the app. */
    fun privacyOptionsRequired(activity: Activity): Boolean =
        UserMessagingPlatform.getConsentInformation(activity).privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { }
    }
}
