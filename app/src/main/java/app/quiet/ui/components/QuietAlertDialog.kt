package app.quiet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.quiet.ui.theme.QuietThemeAccessor

@Composable
fun QuietAlertDialog(
    title: String,
    body: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = QuietThemeAccessor.colors
    val spacing = QuietThemeAccessor.spacing
    val radius = QuietThemeAccessor.radius
    val type = QuietThemeAccessor.typography
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.space5)
                .background(colors.surfaceRaised, RoundedCornerShape(radius.radiusL))
                .border(radius.borderWidth, colors.border, RoundedCornerShape(radius.radiusL))
                .padding(spacing.space5),
        ) {
            Text(text = title, style = type.headline, color = colors.textPrimary)
            Text(
                text = body,
                style = type.body,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = spacing.space2, bottom = spacing.space6),
            )
            QuietPrimaryButton(text = confirmLabel, onClick = onConfirm)
            QuietTextButton(
                text = dismissLabel,
                onClick = onDismiss,
                modifier = Modifier.padding(top = spacing.space2),
            )
        }
    }
}
