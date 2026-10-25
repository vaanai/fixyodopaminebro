package app.quiet.service

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.quiet.ui.components.unlockLabel
import app.quiet.ui.theme.Q
import kotlinx.coroutines.delay
import quiet.core.engine.Cover
import quiet.core.engine.CoverReason
import quiet.core.settings.Origin
import quiet.core.settings.UnlockKind

private data class CoverCopy(val icon: ImageVector, val title: String, val body: String)

private fun copyFor(c: Cover): CoverCopy = when (c.reason) {
    CoverReason.BLOCKED -> CoverCopy(
        Icons.Rounded.Block,
        "${c.surfaceLabel} is off",
        if (c.keptName != null) "${c.keptName} are still up there. Messages and profiles work too."
        else "Messages, profiles and the people you follow are still here. The endless part isn't.",
    )
    CoverReason.BUDGET_DONE -> CoverCopy(
        Icons.Rounded.HourglassBottom,
        "That's today's allowance",
        "You've used your short-form minutes for today. They come back at midnight.",
    )
    CoverReason.FOCUS -> CoverCopy(
        Icons.Rounded.Bedtime,
        "Focus time",
        "Feeds are off while focus is on. Chats still work.",
    )
    CoverReason.SWIPE_LIMIT -> CoverCopy(
        Icons.Rounded.Spa,
        "That's the one you came for",
        when (c.origin) {
            Origin.MESSAGES -> "Swiping on hands you to the algorithm. Head back to the chat."
            Origin.PROFILE -> "You've seen a few from this profile. Head back when you're ready."
            else -> "Swiping on hands you to the algorithm. Head back to where you were."
        },
    )
    CoverReason.APP_LIMIT -> CoverCopy(
        Icons.Rounded.Timer,
        "${c.appName} is done for today",
        "You reached the daily limit you set. It resets at midnight.",
    )
    CoverReason.PAUSE -> CoverCopy(
        Icons.Rounded.SelfImprovement,
        "Take a breath",
        "You're about to open ${c.surfaceLabel}. Still want to?",
    )
}

@Composable
fun CoverContent(model: CoverModel, actions: CoverActions) {
    val c = Q.colors
    val cover = model.cover
    val copy = copyFor(cover)
    // Under a kept area (the Stories row) there's no status bar to clear, and less room.
    val compact = cover.topPx != null
    Box(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .background(
                Brush.radialGradient(
                    listOf(c.accent.copy(alpha = 0.18f), Color.Transparent),
                    center = Offset(540f, 0f),
                    radius = 1400f,
                ),
            )
            // Swallow taps so nothing reaches the feed underneath.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        key(cover.key) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp)
                    .padding(top = if (compact) 24.dp else 72.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (cover.reason == CoverReason.PAUSE) {
                    BreathRing(cover.pauseSeconds)
                } else {
                    Box(
                        Modifier.size(if (compact) 56.dp else 72.dp).clip(CircleShape).background(c.accentDim),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(copy.icon, null, tint = c.accent, modifier = Modifier.size(if (compact) 26.dp else 34.dp))
                    }
                }
                Spacer(Modifier.height(if (compact) 18.dp else 28.dp))
                Text(
                    copy.title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = c.text,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    copy.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = c.textDim,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 360.dp),
                )
                model.todayLine?.let {
                    Spacer(Modifier.height(16.dp))
                    Text(it, style = MaterialTheme.typography.labelMedium, color = c.accent, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(if (compact) 24.dp else 36.dp))
                Buttons(cover, actions)
            }
        }
    }
}

@Composable
private fun Buttons(cover: Cover, actions: CoverActions) {
    val c = Q.colors
    Column(
        Modifier.fillMaxWidth().widthIn(max = 420.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when (cover.reason) {
            CoverReason.PAUSE -> {
                var remaining by remember { mutableIntStateOf(cover.pauseSeconds) }
                LaunchedEffect(Unit) {
                    while (remaining > 0) {
                        delay(1000)
                        remaining--
                    }
                }
                CoverButton(
                    if (remaining > 0) "Continue in $remaining" else "Continue for ${cover.pauseMinutes} min",
                    primary = false,
                    enabled = remaining == 0,
                ) { actions.onContinue(cover) }
                CoverButton("Not now", primary = true) { actions.onLeave(cover) }
            }
            CoverReason.SWIPE_LIMIT -> {
                CoverButton("Go back", primary = true, icon = Icons.AutoMirrored.Rounded.ArrowBack) { actions.onBack(cover) }
            }
            else -> {
                cover.navTargets.take(2).forEachIndexed { i, t ->
                    CoverButton("Go to ${t.label}", primary = i == 0) { actions.onNav(cover, t.id) }
                }
                if (cover.navTargets.isEmpty()) CoverButton("Go back", primary = true) { actions.onBack(cover) }
            }
        }
        if (cover.reason != CoverReason.PAUSE) {
            CoverButton("Leave ${cover.appName}", primary = false) { actions.onLeave(cover) }
        }
        if (cover.canUnlock) {
            Spacer(Modifier.height(6.dp))
            UnlockButton(cover, actions)
        }
    }
}

/**
 * "I want to watch a bit" names exactly what an unlock gives. Tapping it starts the wait; after the
 * wait you confirm, or pick when the user chose to decide each time.
 */
@Composable
private fun UnlockButton(cover: Cover, actions: CoverActions) {
    val c = Q.colors
    var waiting by remember { mutableStateOf(false) }
    var remaining by remember { mutableIntStateOf(cover.unlockWaitSeconds) }
    LaunchedEffect(waiting) {
        if (!waiting) return@LaunchedEffect
        while (remaining > 0) {
            delay(1000)
            remaining--
        }
    }
    val offers = cover.unlockOffers
    val left = "${cover.unlocksLeft} unlock${if (cover.unlocksLeft == 1) "" else "s"} left today"
    if (waiting && remaining == 0) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (kind in offers) {
                Box(Modifier.weight(1f)) {
                    CoverButton(
                        (if (offers.size == 1) "Unlock " else "") + unlockLabel(kind, cover.unlockVideos, cover.unlockMinutes),
                        primary = false,
                    ) { actions.onUnlock(cover, videos = kind == UnlockKind.VIDEOS) }
                }
            }
        }
        Text(
            left,
            style = MaterialTheme.typography.labelMedium,
            color = c.textFaint,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }
    val what = if (offers.size == 1) unlockLabel(offers[0], cover.unlockVideos, cover.unlockMinutes) else "to watch a bit"
    val label = when {
        !waiting -> "I want $what · ${cover.unlocksLeft} left today"
        else -> "Hold on… $remaining"
    }
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = if (waiting) c.textFaint else c.textDim,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = !waiting) {
                waiting = true
            }
            .padding(vertical = 14.dp),
    )
}

@Composable
private fun CoverButton(
    label: String,
    primary: Boolean,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    val c = Q.colors
    val bg = when {
        !enabled -> c.surface
        primary -> c.accent
        else -> c.surfaceHigh
    }
    val fg = when {
        !enabled -> c.textFaint
        primary -> c.onAccent
        else -> c.text
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
        }
        Text(label, style = MaterialTheme.typography.titleSmall, color = fg)
    }
}

@Composable
private fun BreathRing(seconds: Int) {
    val c = Q.colors
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMillis = seconds.coerceAtLeast(1) * 1000, easing = LinearEasing))
    }
    val breathe = rememberInfiniteTransition(label = "breathe")
    val scale by breathe.animateFloat(
        initialValue = 0.62f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000), RepeatMode.Reverse),
        label = "scale",
    )
    Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 6.dp.toPx()
            drawCircle(c.outline, style = Stroke(stroke))
            drawArc(
                c.accent,
                startAngle = -90f,
                sweepAngle = 360f * progress.value,
                useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            drawCircle(c.accent.copy(alpha = 0.22f), radius = size.minDimension / 2.6f * scale)
        }
        Text(if (scale > 0.81f) "out" else "in", style = MaterialTheme.typography.labelLarge, color = c.textDim)
    }
}

@Composable
fun PillContent(text: String, urgent: Boolean) {
    val c = Q.colors
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = if (urgent) c.onAccent else c.text,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (urgent) c.warn else c.surfaceHigh.copy(alpha = 0.92f))
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

@Composable
fun ToastContent(text: String) {
    val c = Q.colors
    Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = c.text,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(c.surfaceHigh.copy(alpha = 0.96f))
                .padding(horizontal = 20.dp, vertical = 14.dp),
        )
    }
}
