package app.quiet.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "library_items")
data class LibraryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: String,
    val status: String,
    val progress: String,
    val rating: Int,
    val notes: String,
    val updatedAt: Long,
)
