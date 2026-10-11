package app.quiet.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "enforcement_events")
data class EnforcementEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val appId: String,
    val surfaceId: String,
    val action: String,
)
