package app.quiet.data.repo

import app.quiet.data.db.EnforcementEventDao
import app.quiet.data.db.EnforcementEventEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class EventRepository(private val dao: EnforcementEventDao) {
    suspend fun log(action: String, appId: String, surfaceId: String, timestamp: Long = System.currentTimeMillis()) {
        withContext(Dispatchers.IO) {
            dao.insert(
                EnforcementEventEntity(
                    timestamp = timestamp,
                    appId = appId,
                    surfaceId = surfaceId,
                    action = action,
                ),
            )
        }
    }

    suspend fun recent(limit: Int = 5) = withContext(Dispatchers.IO) { dao.recent(limit) }

    suspend fun countBlockedToday(startOfDay: Long) = withContext(Dispatchers.IO) {
        dao.countSince(startOfDay)
    }

    suspend fun pruneOlderThan(cutoff: Long) = withContext(Dispatchers.IO) {
        dao.deleteOlderThan(cutoff)
    }

    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        val arr = JSONArray()
        dao.all().forEach { event ->
            arr.put(
                JSONObject()
                    .put("id", event.id)
                    .put("timestamp", event.timestamp)
                    .put("appId", event.appId)
                    .put("surfaceId", event.surfaceId)
                    .put("action", event.action),
            )
        }
        arr.toString()
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) { dao.deleteAll() }
}
