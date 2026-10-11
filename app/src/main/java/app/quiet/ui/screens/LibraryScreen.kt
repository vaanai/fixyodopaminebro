package app.quiet.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.quiet.data.db.LibraryItemEntity
import app.quiet.ui.components.FilterChip
import app.quiet.ui.components.QuietPrimaryButton
import app.quiet.ui.components.QuietTextButton
import app.quiet.ui.library.LibraryEditSheet
import app.quiet.ui.theme.QuietThemeAccessor
import app.quiet.ui.viewmodel.LibraryViewModel

@Composable
fun LibraryScreen(vm: LibraryViewModel) {
    val state by vm.state.collectAsState()
    val spacing = QuietThemeAccessor.spacing
    val type = QuietThemeAccessor.typography
    val colors = QuietThemeAccessor.colors
    var editorOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<LibraryItemEntity?>(null) }
    var deleteConfirm by remember { mutableStateOf<LibraryItemEntity?>(null) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.space5),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(text = "Library", style = type.headline, color = colors.textPrimary, modifier = Modifier.weight(1f))
            IconButton(onClick = { editing = null; editorOpen = true }) {
                Icon(Icons.Outlined.Add, contentDescription = "Add item")
            }
        }
        Row {
            listOf("All", "Shows", "Movies", "Books", "Games").forEach { t ->
                FilterChip(text = t, selected = state.typeFilter == t, onClick = { vm.setTypeFilter(t) })
            }
        }
        Row {
            listOf("Want", "Now", "Done", "Dropped").forEach { s ->
                FilterChip(text = s, selected = state.statusFilter == s, onClick = {
                    vm.setStatusFilter(if (state.statusFilter == s) null else s)
                })
            }
        }
        if (state.items.isEmpty()) {
            Text(
                text = state.emptyMessage ?: "Track what you watch and read.",
                style = type.title,
                modifier = Modifier.padding(top = spacing.space12),
            )
            if (state.emptyMessage?.contains("Track") == true) {
                QuietPrimaryButton(text = "Add item", onClick = { editorOpen = true }, modifier = Modifier.padding(top = spacing.space6))
            } else {
                QuietTextButton(text = "Clear filters", onClick = { vm.clearFilters() }, modifier = Modifier.padding(top = spacing.space6))
            }
        } else {
            state.items.forEach { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = spacing.space3)
                        .clickable { editing = item; editorOpen = true }
                        .padding(spacing.space4),
                ) {
                    Text(text = item.title, style = type.title, color = colors.textPrimary)
                    if (item.progress.isNotBlank()) {
                        Text(text = item.progress, style = type.label, color = colors.textSecondary)
                    }
                }
            }
        }
    }
    if (editorOpen) {
        LibraryEditSheet(
            initial = editing,
            onDismiss = { editorOpen = false },
            onSave = {
                vm.save(it)
                editorOpen = false
            },
            onDelete = if (editing != null) {
                { deleteConfirm = editing; editorOpen = false }
            } else {
                null
            },
        )
    }
    deleteConfirm?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteConfirm = null },
            title = { Text("Delete this item?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { vm.delete(item); deleteConfirm = null }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirm = null }) { Text("Cancel") }
            },
        )
    }
}
