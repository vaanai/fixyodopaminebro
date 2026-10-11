package app.quiet.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface EnforcementEventDao {
    @Insert
    suspend fun insert(event: EnforcementEventEntity): Long

    @Query("SELECT * FROM enforcement_events ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<EnforcementEventEntity>

    @Query("SELECT COUNT(*) FROM enforcement_events WHERE timestamp >= :startOfDay")
    suspend fun countSince(startOfDay: Long): Int

    @Query("DELETE FROM enforcement_events WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long): Int

    @Query("SELECT * FROM enforcement_events ORDER BY timestamp DESC")
    suspend fun all(): List<EnforcementEventEntity>

    @Query("DELETE FROM enforcement_events")
    suspend fun deleteAll()
}
