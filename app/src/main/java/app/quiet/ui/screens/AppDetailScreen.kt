package app.quiet.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.quiet.ui.components.ProtectedRow
import app.quiet.ui.components.QuietScreenColumn
import app.quiet.ui.components.QuietSurfaceGroup
import app.quiet.ui.components.QuietTopBar
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
    QuietScreenColumn {
        QuietTopBar(
            title = state.appName,
            leading = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
            },
        )
        if (state.feedSurfaces.isNotEmpty()) {
            Text(
                text = "Feeds and reels",
                style = type.label,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = spacing.space6, bottom = spacing.space2),
            )
            QuietSurfaceGroup {
                state.feedSurfaces.forEachIndexed { index, row ->
                    SurfaceRow(
                        label = row.rule.label,
                        sublabel = row.sublabel,
                        policy = row.policy,
                        options = listOf(Policy.ALLOW, Policy.GATE, Policy.BLOCK),
                        onChange = { vm.setFeedPolicy(row.rule.fullId, it) },
                        showDivider = index < state.feedSurfaces.lastIndex,
                    )
                }
            }
        }
        state.friendsSurface?.let { friends ->
            Text(
                text = "Friends' reels",
                style = type.label,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = spacing.space6, bottom = spacing.space2),
            )
            QuietSurfaceGroup {
                SurfaceRow(
                    label = friends.rule.label,
                    sublabel = friends.sublabel,
                    policy = friends.policy,
                    options = listOf(Policy.BLOCK, Policy.OPEN_ONLY),
                    onChange = { vm.setFriendsPolicy(it) },
                    showDivider = false,
                    segmentEnabled = friends.rule.enforce,
                )
            }
        }
        Text(
            text = "Messages",
            style = type.label,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = spacing.space6, bottom = spacing.space2),
        )
        Text(text = "Never blocked.", style = type.body, color = colors.textSecondary)
        QuietSurfaceGroup(modifier = Modifier.padding(top = spacing.space2)) {
            ProtectedRow()
        }
        Text(
            text = "Quiet hours and breaks are in Settings.",
            style = type.label,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = spacing.space8, bottom = spacing.space8),
        )
    }
}
