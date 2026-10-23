package app.quiet.ui.components

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.quiet.ui.theme.Q

val CardShape = RoundedCornerShape(22.dp)

@Composable
fun QCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Q.colors
    Column(
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(c.surface)
            .border(1.dp, c.outline.copy(alpha = 0.6f), CardShape)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(padding),
        content = content,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = Q.colors.textFaint,
        modifier = modifier.padding(start = 6.dp, top = 22.dp, bottom = 10.dp),
    )
}

@Composable
fun ScreenHeader(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null, trailing: @Composable RowScope.() -> Unit = {}) {
    val c = Q.colors
    Column(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp)) {
        if (onBack != null) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = c.text)
            }
            Spacer(Modifier.height(8.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.headlineLarge, color = c.text)
                if (subtitle != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = c.textDim)
                }
            }
            trailing()
        }
    }
}

@Composable
fun Divider() {
    Box(Modifier.fillMaxWidth().padding(vertical = 2.dp).height(1.dp).background(Q.colors.outline.copy(alpha = 0.5f)))
}

@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    subtitle: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    val c = Q.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = c.textDim, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = if (enabled) c.text else c.textFaint)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.textDim)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = c.onAccent,
                checkedTrackColor = c.accent,
                uncheckedThumbColor = c.textDim,
                uncheckedTrackColor = c.surfaceHigh,
                uncheckedBorderColor = c.outline,
            ),
        )
    }
}

@Composable
fun NavRow(title: String, subtitle: String? = null, icon: ImageVector? = null, value: String? = null, onClick: () -> Unit) {
    val c = Q.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = c.textDim, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = c.text)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.textDim)
        }
        if (value != null) {
            Text(value, style = MaterialTheme.typography.labelLarge, color = c.textDim)
            Spacer(Modifier.width(4.dp))
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.textFaint)
    }
}

/** A labelled value with − and + buttons. [format] renders the value. */
@Composable
fun StepperRow(
    title: String,
    value: Int,
    subtitle: String? = null,
    options: List<Int>,
    format: (Int) -> String,
    onChange: (Int) -> Unit,
) {
    val c = Q.colors
    val idx = options.indexOf(value).let { if (it >= 0) it else options.indexOfFirst { o -> o >= value }.coerceAtLeast(0) }
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = c.text)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.textDim)
        }
        Spacer(Modifier.width(8.dp))
        RoundIconButton(Icons.Rounded.Remove, enabled = idx > 0) { onChange(options[idx - 1]) }
        Text(
            format(value),
            style = MaterialTheme.typography.titleSmall,
            color = c.text,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(78.dp),
            maxLines = 1,
        )
        RoundIconButton(Icons.Rounded.Add, enabled = idx < options.size - 1) { onChange(options[idx + 1]) }
    }
}

@Composable
fun RoundIconButton(icon: ImageVector, enabled: Boolean = true, size: Dp = 36.dp, onClick: () -> Unit) {
    val c = Q.colors
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (enabled) c.surfaceHigh else c.surface)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = if (enabled) c.text else c.textFaint, modifier = Modifier.size(18.dp))
    }
}

/** Segmented control. */
@Composable
fun <T> Segmented(options: List<T>, selected: T, label: (T) -> String, modifier: Modifier = Modifier, onSelect: (T) -> Unit) {
    val c = Q.colors
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.bg)
            .border(1.dp, c.outline, RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (o in options) {
            val on = o == selected
            val bg by animateColorAsState(if (on) c.accent else Color.Transparent, label = "seg")
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bg)
                    .clickable { onSelect(o) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(o),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (on) c.onAccent else c.textDim,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val c = Q.colors
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) c.onAccent else c.textDim,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) c.accent else c.surfaceHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
fun Tag(text: String, color: Color = Q.colors.accent) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 9.dp, vertical = 4.dp),
    )
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val c = Q.colors
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) c.accent else c.surfaceHigh)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.titleSmall, color = if (enabled) c.onAccent else c.textFaint)
    }
}

@Composable
fun SecondaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = Q.colors
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.surfaceHigh)
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.titleSmall, color = c.text)
    }
}

@Composable
fun SmallButton(text: String, accent: Boolean = false, onClick: () -> Unit) {
    val c = Q.colors
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = if (accent) c.onAccent else c.text,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (accent) c.accent else c.surfaceHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    )
}

/** Progress ring with content in the middle. */
@Composable
fun Ring(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 168.dp,
    stroke: Dp = 14.dp,
    color: Color = Q.colors.accent,
    content: @Composable () -> Unit,
) {
    val c = Q.colors
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(700), label = "ring")
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = stroke.toPx()
            val inset = s / 2
            drawArc(
                c.surfaceHigh, 135f, 270f, false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(this.size.width - s, this.size.height - s),
                style = Stroke(s, cap = StrokeCap.Round),
            )
            if (animated > 0f) {
                drawArc(
                    color, 135f, 270f * animated, false,
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(this.size.width - s, this.size.height - s),
                    style = Stroke(s, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}

@Composable
fun Bar(fraction: Float, modifier: Modifier = Modifier, color: Color = Q.colors.accent, height: Dp = 6.dp) {
    val c = Q.colors
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(600), label = "bar")
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(50)).background(c.surfaceHigh)) {
        Box(Modifier.fillMaxWidth(animated).height(height).clip(RoundedCornerShape(50)).background(color))
    }
}

@Composable
fun Stat(value: String, label: String, modifier: Modifier = Modifier, color: Color = Q.colors.text) {
    val c = Q.colors
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = color, maxLines = 1)
        Text(label, style = MaterialTheme.typography.bodySmall, color = c.textDim, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** The real launcher icon of an installed app, or a lettered circle. */
@Composable
fun AppIcon(packageName: String, label: String, size: Dp = 36.dp) {
    val context = LocalContext.current
    val bitmap: ImageBitmap? = remember(packageName) {
        runCatching {
            val d = context.packageManager.getApplicationIcon(packageName)
            val px = 96
            val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
            val canvas = AndroidCanvas(bmp)
            d.setBounds(0, 0, px, px)
            d.draw(canvas)
            bmp.asImageBitmap()
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(bitmap, label, Modifier.size(size).clip(RoundedCornerShape(size / 4)))
    } else {
        val c = Q.colors
        Box(Modifier.size(size).clip(RoundedCornerShape(size / 4)).background(c.surfaceHigh), contentAlignment = Alignment.Center) {
            Text(label.take(1), style = MaterialTheme.typography.titleSmall, color = c.textDim)
        }
    }
}

fun isInstalled(pm: PackageManager, pkg: String): Boolean =
    runCatching { pm.getApplicationInfo(pkg, 0); true }.getOrDefault(false)

fun formatMinutes(seconds: Int): String {
    val m = seconds / 60
    return when {
        m >= 60 -> "${m / 60}h ${m % 60}m"
        seconds in 1..59 -> "<1m"
        else -> "${m}m"
    }
}
