package app.quiet.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.quiet.ui.theme.QuietThemeAccessor

@Composable
fun QuietTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    singleLine: Boolean = true,
) {
    val colors = QuietThemeAccessor.colors
    val spacing = QuietThemeAccessor.spacing
    val radius = QuietThemeAccessor.radius
    val type = QuietThemeAccessor.typography
    Column(modifier = Modifier.padding(top = spacing.space4)) {
        Text(text = label, style = type.label, color = colors.textSecondary)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = type.body.copy(color = colors.textPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (singleLine) 52.dp else 96.dp)
                .padding(top = spacing.space2)
                .border(radius.borderWidth, colors.border, RoundedCornerShape(radius.radiusM))
                .padding(spacing.space4),
            decorationBox = { inner ->
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(text = placeholder, style = type.body, color = colors.textTertiary)
                }
                inner()
            },
        )
    }
}
