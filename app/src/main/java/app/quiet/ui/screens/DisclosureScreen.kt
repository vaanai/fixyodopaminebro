package app.quiet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.quiet.LocalNav
import app.quiet.QuietApp
import app.quiet.ui.components.PrimaryButton
import app.quiet.ui.components.ScreenHeader
import app.quiet.ui.components.SecondaryButton
import app.quiet.ui.theme.Q

/**
 * The accessibility disclosure Google Play asks for: what Quiet reads with the accessibility service,
 * what it does with it, and an explicit choice, shown before anyone is sent to switch the service on.
 */
@Composable
fun DisclosureBody() {
    val c = Q.colors
    Text(
        "To block Reels, For You and Shorts, Quiet needs to see what's on screen in the apps you choose. " +
            "It does that with Android's Accessibility service. Quiet isn't an assistive tool; this is the only way an app can do this.",
        style = MaterialTheme.typography.bodyLarge,
        color = c.textDim,
    )
    Spacer(Modifier.height(18.dp))
    DisclosurePoint(
        Icons.Rounded.Visibility,
        "What it reads",
        "Which app is open, and the layout of Instagram, TikTok and YouTube screens: tab names, button labels and view IDs.",
    )
    DisclosurePoint(
        Icons.Rounded.Block,
        "What it does with it",
        "Tells a feed from a chat, covers the feeds you blocked, and counts your time in social apps.",
    )
    DisclosurePoint(
        Icons.Rounded.TouchApp,
        "What it does for you",
        "Taps a tab like Messages to skip a blocked feed, and presses Back or Home when you choose to leave.",
    )
    DisclosurePoint(
        Icons.Rounded.PhoneAndroid,
        "Where it goes",
        "Nowhere. It all happens on your phone. Quiet doesn't collect, store or share screen content, messages or anything you type.",
    )
}

@Composable
private fun DisclosurePoint(icon: ImageVector, title: String, body: String) {
    val c = Q.colors
    Row(Modifier.padding(vertical = 8.dp)) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(c.accentDim), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = c.accent, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = c.text)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = c.textDim)
        }
    }
}

/** The disclosure on its own, for turning Quiet on after setup. Agreeing opens Accessibility settings. */
@Composable
fun DisclosureScreen() {
    val nav = LocalNav.current
    val context = LocalContext.current
    Page {
        ScreenHeader("Before you turn Quiet on", onBack = { nav.back() })
        Spacer(Modifier.height(8.dp))
        DisclosureBody()
        Spacer(Modifier.height(24.dp))
        PrimaryButton("Agree and turn on") {
            QuietApp.store.updateSettings { it.copy(disclosureAccepted = true) }
            nav.back()
            openAccessibilitySettings(context)
        }
        Spacer(Modifier.height(8.dp))
        SecondaryButton("No thanks") { nav.back() }
    }
}
