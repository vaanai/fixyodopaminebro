package app.quiet.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.quiet.data.db.LibraryItemEntity
import app.quiet.ui.components.FilterChip
import app.quiet.ui.components.QuietPrimaryButton
import app.quiet.ui.components.RatingDotsRow
import app.quiet.ui.components.QuietTextButton
import app.quiet.ui.components.QuietTextField
import app.quiet.ui.theme.QuietThemeAccessor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryEditSheet(
    initial: LibraryItemEntity?,
    onDismiss: () -> Unit,
    onSave: (LibraryItemEntity) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val spacing = QuietThemeAccessor.spacing
    val type = QuietThemeAccessor.typography
    val colors = QuietThemeAccessor.colors
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var typeSel by remember { mutableStateOf(initial?.type) }
    var status by remember { mutableStateOf(initial?.status ?: "Want") }
    var progress by remember { mutableStateOf(initial?.progress ?: "") }
    var rating by remember { mutableIntStateOf(initial?.rating ?: 0) }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    val canSave = title.isNotBlank() && typeSel != null
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(spacing.space5)) {
            Text(
                text = if (initial == null) "Add item" else "Edit item",
                style = type.headline,
                color = colors.textPrimary,
            )
            QuietTextField(label = "Title", value = title, onValueChange = { title = it }, placeholder = "Title")
            Text(text = "Type", style = type.label, color = colors.textSecondary, modifier = Modifier.padding(top = spacing.space4))
            Row {
                listOf("Shows", "Movies", "Books", "Games").forEach { t ->
                    FilterChip(text = t, selected = typeSel == t, onClick = { typeSel = t })
                }
            }
            Text(text = "Status", style = type.label, color = colors.textSecondary, modifier = Modifier.padding(top = spacing.space4))
            Row {
                listOf("Want", "Now", "Done", "Dropped").forEach { s ->
                    FilterChip(text = s, selected = status == s, onClick = { status = s })
                }
            }
            QuietTextField(
                label = "Progress",
                value = progress,
                onValueChange = { progress = it },
                placeholder = "S2 · E4, or p. 120",
            )
            Text(text = "Rating", style = type.label, color = colors.textSecondary, modifier = Modifier.padding(top = spacing.space4))
            RatingDotsRow(rating = rating, onRatingChange = { rating = it })
            QuietTextField(label = "Notes", value = notes, onValueChange = { notes = it }, singleLine = false)
            QuietPrimaryButton(
                text = "Save",
                onClick = {
                    onSave(
                        LibraryItemEntity(
                            id = initial?.id ?: 0,
                            title = title.trim(),
                            type = typeSel!!,
                            status = status,
                            progress = progress,
                            rating = rating,
                            notes = notes,
                            updatedAt = System.currentTimeMillis(),
                        ),
                    )
                },
                enabled = canSave,
                modifier = Modifier.padding(top = spacing.space6),
            )
            if (initial != null && onDelete != null) {
                QuietTextButton(text = "Delete", onClick = onDelete)
            }
        }
    }
}
