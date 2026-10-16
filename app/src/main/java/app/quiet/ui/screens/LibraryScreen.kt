package app.quiet.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import app.quiet.ui.components.QuietAlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import app.quiet.data.db.LibraryItemEntity
import app.quiet.ui.components.FilterChip
import app.quiet.ui.components.LibraryCard
import app.quiet.ui.components.QuietPrimaryButton
import app.quiet.ui.components.QuietTextButton
import app.quiet.ui.components.QuietTopBar
import app.quiet.ui.copy.QuietCopy
import app.quiet.ui.library.LibraryEditSheet
import app.quiet.ui.theme.QuietThemeAccessor
import app.quiet.ui.viewmodel.LibraryViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize

@Composable
fun LibraryScreen(vm: LibraryViewModel) {
    val state by vm.state.collectAsState()
    val spacing = QuietThemeAccessor.spacing
    val type = QuietThemeAccessor.typography
    val colors = QuietThemeAccessor.colors
    var editorOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<LibraryItemEntity?>(null) }
    var deleteConfirm by remember { mutableStateOf<LibraryItemEntity?>(null) }
    val typeScroll = rememberScrollState()
    val statusScroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = spacing.space5),
    ) {
        QuietTopBar(
            title = "Library",
            trailing = {
                IconButton(onClick = { editing = null; editorOpen = true }) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add item")
                }
            },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(typeScroll)
                .padding(vertical = spacing.space2),
        ) {
            listOf("All", "Shows", "Movies", "Books", "Games").forEach { t ->
                FilterChip(text = t, selected = state.typeFilter == t, onClick = { vm.setTypeFilter(t) })
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(statusScroll)
                .padding(bottom = spacing.space2),
        ) {
            listOf("Want", "Now", "Done", "Dropped").forEach { s ->
                FilterChip(text = s, selected = state.statusFilter == s, onClick = {
                    vm.setStatusFilter(if (state.statusFilter == s) null else s)
                })
            }
        }
        if (state.items.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = state.emptyMessage ?: "Track what you watch and read.",
                    style = type.title,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = spacing.space12),
                )
                if (state.emptyMessage?.contains("Track") == true) {
                    Text(
                        text = QuietCopy.LIBRARY_EMPTY_BODY,
                        style = type.body,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = spacing.space2),
                    )
                    QuietPrimaryButton(
                        text = "Add item",
                        onClick = { editorOpen = true },
                        modifier = Modifier.padding(top = spacing.space6),
                    )
                } else {
                    QuietTextButton(
                        text = "Clear filters",
                        onClick = { vm.clearFilters() },
                        modifier = Modifier.padding(top = spacing.space6),
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                state.items.forEach { item ->
                    LibraryCard(
                        item = item,
                        onClick = { editing = item; editorOpen = true },
                        modifier = Modifier.padding(top = spacing.space3),
                    )
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
        QuietAlertDialog(
            title = "Delete this item?",
            body = "This can't be undone.",
            confirmLabel = "Delete",
            dismissLabel = "Cancel",
            onConfirm = {
                vm.delete(item)
                deleteConfirm = null
            },
            onDismiss = { deleteConfirm = null },
        )
    }
}
