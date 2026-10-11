package app.quiet.data.repo

import app.quiet.data.db.LibraryItemDao
import app.quiet.data.db.LibraryItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class LibraryRepository(private val dao: LibraryItemDao) {
    suspend fun all() = withContext(Dispatchers.IO) { dao.all() }

    suspend fun insert(item: LibraryItemEntity) = withContext(Dispatchers.IO) { dao.insert(item) }

    suspend fun update(item: LibraryItemEntity) = withContext(Dispatchers.IO) { dao.update(item) }

    suspend fun delete(item: LibraryItemEntity) = withContext(Dispatchers.IO) { dao.delete(item) }

    suspend fun deleteAll() = withContext(Dispatchers.IO) { dao.deleteAll() }

    fun filterAndSort(
        items: List<LibraryItemEntity>,
        typeFilter: String?,
        statusFilter: String?,
    ): List<LibraryItemEntity> {
        return items.filter { item ->
            val typeOk = typeFilter == null || typeFilter == "All" || item.type == typeFilter
            val statusOk = statusFilter == null || item.status == statusFilter
            typeOk && statusOk
        }.sortedByDescending { it.updatedAt }
    }

    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        val arr = JSONArray()
        dao.all().forEach { item ->
            arr.put(
                JSONObject()
                    .put("title", item.title)
                    .put("type", item.type)
                    .put("status", item.status)
                    .put("progress", item.progress)
                    .put("rating", item.rating)
                    .put("notes", item.notes)
                    .put("updatedAt", item.updatedAt),
            )
        }
        arr.toString()
    }
}
