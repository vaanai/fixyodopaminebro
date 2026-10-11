package app.quiet.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [EnforcementEventEntity::class, LibraryItemEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class QuietDatabase : RoomDatabase() {
    abstract fun enforcementEventDao(): EnforcementEventDao
    abstract fun libraryItemDao(): LibraryItemDao
}
