package app.quiet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.quiet.ui.theme.QuietThemeAccessor

@Composable
fun OnboardingStepIndicator(currentStep: Int, totalSteps: Int = 4) {
    val colors = QuietThemeAccessor.colors
    val radius = QuietThemeAccessor.radius
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(totalSteps) { index ->
            val step = index + 1
            val filled = step <= currentStep
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(2.dp)
                    .background(
                        if (filled) colors.accent else colors.border,
                        RoundedCornerShape(radius.radiusPill),
                    ),
            ) {}
        }
    }
}
