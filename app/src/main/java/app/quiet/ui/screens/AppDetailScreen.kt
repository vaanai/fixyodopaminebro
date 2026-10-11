package app.quiet.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.quiet.ui.components.ProtectedRow
import app.quiet.ui.components.QuietTextButton
import app.quiet.ui.components.SurfaceRow
import app.quiet.ui.theme.QuietThemeAccessor
import app.quiet.ui.viewmodel.AppDetailViewModel
import quiet.core.rules.Policy
@Composable
fun AppDetailScreen(appId: String, onBack: () -> Unit) {
    val vm: AppDetailViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        key = appId,
        factory = AppDetailViewModel.factory(appId),
    )
    val state by vm.state.collectAsState()
    val spacing = QuietThemeAccessor.spacing
    val type = QuietThemeAccessor.typography
    val colors = QuietThemeAccessor.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.space5),
    ) {
        QuietTextButton(text = "Back", onClick = onBack, accent = true)
        Text(text = state.appName, style = type.headline, color = colors.textPrimary)
        Text(
            text = "Feeds and reels",
            style = type.label,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = spacing.space6, bottom = spacing.space2),
        )
        state.feedSurfaces.forEach { row ->
            SurfaceRow(
                label = row.rule.label,
                sublabel = row.sublabel,
                policy = row.policy,
                options = listOf(Policy.ALLOW, Policy.GATE, Policy.BLOCK),
                onChange = { vm.setFeedPolicy(row.rule.fullId, it) },
            )
        }
        state.friendsSurface?.let { friends ->
            Text(
                text = "Friends' reels",
                style = type.label,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = spacing.space6, bottom = spacing.space2),
            )
            SurfaceRow(
                label = friends.rule.label,
                sublabel = friends.sublabel,
                policy = friends.policy,
                options = listOf(Policy.BLOCK, Policy.ALLOW),
                onChange = { vm.setFriendsPolicy(it) },
            )
        }
        Text(
            text = "Messages",
            style = type.label,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = spacing.space6, bottom = spacing.space2),
        )
        Text(text = "Never blocked.", style = type.body, color = colors.textSecondary)
        ProtectedRow()
        Text(
            text = "Quiet hours and breaks are in Settings.",
            style = type.label,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = spacing.space8),
        )
    }
}
