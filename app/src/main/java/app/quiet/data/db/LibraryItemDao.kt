package app.quiet.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface LibraryItemDao {
    @Insert
    suspend fun insert(item: LibraryItemEntity): Long

    @Update
    suspend fun update(item: LibraryItemEntity)

    @Delete
    suspend fun delete(item: LibraryItemEntity)

    @Query("SELECT * FROM library_items ORDER BY updatedAt DESC")
    suspend fun all(): List<LibraryItemEntity>

    @Query("DELETE FROM library_items")
    suspend fun deleteAll()
}
