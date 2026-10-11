package app.quiet.ui.navigation

import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.quiet.ui.theme.QuietThemeAccessor

@Composable
fun QuietBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val colors = QuietThemeAccessor.colors
    val spacing = QuietThemeAccessor.spacing
    val type = QuietThemeAccessor.typography
    val items = listOf(
        Triple(0, "Today", Icons.Outlined.Today),
        Triple(1, "Apps", Icons.Outlined.Apps),
        Triple(2, "Library", Icons.Outlined.Book),
    )
    NavigationBar(
        modifier = Modifier.height(spacing.bottomBarHeight),
        containerColor = colors.background,
        tonalElevation = 0.dp,
    ) {
        items.forEach { (index, label, icon) ->
            val selectedItem = selected == index
            NavigationBarItem(
                selected = selectedItem,
                onClick = { onSelect(index) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                    )
                },
                label = { Text(text = label, style = type.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.accent,
                    selectedTextColor = colors.accent,
                    unselectedIconColor = colors.textSecondary,
                    unselectedTextColor = colors.textSecondary,
                    indicatorColor = colors.background,
                ),
            )
        }
    }
}
