package app.quiet.ui.sheets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.view.accessibility.AccessibilityManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.view.ViewCompat
import app.quiet.ui.components.QuietOutlineButton
import app.quiet.ui.components.QuietPrimaryButton
import app.quiet.ui.components.QuietTextButton
import app.quiet.ui.theme.QuietThemeAccessor
import quiet.core.QuietTiming
import kotlin.math.hypot

@Composable
fun InterventionSheetContent(
    surfaceLabel: String,
    messagesFound: Boolean,
    breakEnabled: Boolean,
    onGoMessages: () -> Unit,
    onBreak: () -> Unit,
    onHome: () -> Unit,
) {
    val colors = QuietThemeAccessor.colors
    val spacing = QuietThemeAccessor.spacing
    val type = QuietThemeAccessor.typography
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(spacing.space5),
        verticalArrangement = Arrangement.spacedBy(spacing.space2),
    ) {
        Text(text = "$surfaceLabel is paused.", style = type.headline, color = colors.textPrimary)
        Text(
            text = "Messages are one tap away.",
            style = type.body,
            color = colors.textSecondary,
            modifier = Modifier.padding(bottom = spacing.space6),
        )
        if (messagesFound) {
            QuietPrimaryButton(text = "Go to Messages", onClick = onGoMessages)
        }
        QuietOutlineButton(
            text = if (breakEnabled) "Take a 5-minute break" else "No breaks left today",
            onClick = onBreak,
            enabled = breakEnabled,
        )
        QuietTextButton(text = "Go home", onClick = onHome)
    }
}

@Composable
fun GateSheetContent(
    surfaceLabel: String,
    minutes: Int,
    onConfirm: () -> Unit,
    onGoMessages: () -> Unit,
    onNotNow: () -> Unit,
) {
    val colors = QuietThemeAccessor.colors
    val spacing = QuietThemeAccessor.spacing
    val type = QuietThemeAccessor.typography
    val context = LocalContext.current
    val touchExploration = (context.getSystemService(AccessibilityManager::class.java))
        ?.isTouchExplorationEnabled == true
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(spacing.space5),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.space2),
    ) {
        Text(
            text = "Open $surfaceLabel for $minutes minutes?",
            style = type.headline,
            color = colors.textPrimary,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "This is a choice, not a wall.",
            style = type.body,
            color = colors.textSecondary,
            modifier = Modifier.fillMaxWidth(),
        )
        if (touchExploration) {
            QuietPrimaryButton(text = "Open for $minutes minutes", onClick = onConfirm)
        } else {
            HoldRing(onComplete = onConfirm)
        }
        QuietTextButton(text = "Go to Messages", onClick = onGoMessages, accent = false)
        QuietTextButton(text = "Not now", onClick = onNotNow, accent = false)
    }
}

@Composable
private fun HoldRing(onComplete: () -> Unit) {
    val colors = QuietThemeAccessor.colors
    val radius = QuietThemeAccessor.radius
    var progress by remember { mutableFloatStateOf(0f) }
    var holding by remember { mutableStateOf(false) }
    val ringSize = radius.ringSize
    Canvas(
        modifier = Modifier
            .size(ringSize)
            .semantics { contentDescription = "Hold to confirm" }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { holding = true },
                    onDragEnd = {
                        holding = false
                        if (progress < 1f) progress = 0f
                    },
                    onDragCancel = {
                        holding = false
                        progress = 0f
                    },
                    onDrag = { change, _ ->
                        val center = Offset(ringSize.toPx() / 2, ringSize.toPx() / 2)
                        val dist = hypot(change.position.x - center.x, change.position.y - center.y)
                        if (dist > ringSize.toPx() / 2 + 8.dp.toPx()) {
                            holding = false
                            progress = 0f
                        } else if (holding) {
                            progress = (progress + 16f / QuietTiming.GATE_HOLD_MS).coerceAtMost(1f)
                            if (progress >= 1f) onComplete()
                        }
                    },
                )
            },
    ) {
        val stroke = radius.ringStroke.toPx()
        drawArc(
            color = colors.border,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(stroke, stroke),
            size = Size(size.width - stroke * 2, size.height - stroke * 2),
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
        drawArc(
            color = colors.accent,
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            topLeft = Offset(stroke, stroke),
            size = Size(size.width - stroke * 2, size.height - stroke * 2),
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
    }
    Text(text = "Hold", style = QuietThemeAccessor.typography.title, color = colors.textPrimary)
}
