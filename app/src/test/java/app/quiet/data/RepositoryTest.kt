package app.quiet.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.quiet.data.db.EnforcementEventEntity
import app.quiet.data.db.QuietDatabase
import app.quiet.data.policy.PolicyRepository
import app.quiet.data.prefs.PresetIds
import app.quiet.data.prefs.QuietPreferences
import app.quiet.data.repo.EventRepository
import app.quiet.data.repo.LibraryRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RepositoryTest {
    private lateinit var db: QuietDatabase
    private lateinit var events: EventRepository
    private lateinit var library: LibraryRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        db = Room.inMemoryDatabaseBuilder(context, QuietDatabase::class.java).build()
        events = EventRepository(db.enforcementEventDao())
        library = LibraryRepository(db.libraryItemDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun eventInsertAndPrune() = runBlocking {
        val old = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(91)
        val recent = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
        events.log("SHEET_SHOWN", "instagram", "instagram.reels", old)
        events.log("SHEET_SHOWN", "instagram", "instagram.reels", recent)
        events.pruneOlderThan(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(90))
        val all = db.enforcementEventDao().all()
        assertEquals(1, all.size)
    }

    @Test
    fun exportLogHasNoTextColumns() = runBlocking {
        events.log("REDIRECTED", "instagram", "instagram.home_feed")
        val json = events.exportJson()
        assertFalse(json.contains("text"))
        assertFalse(json.contains("contentDescription"))
        assertFalse(json.contains("label"))
    }

    @Test
    fun presetSetsCustomOnEdit() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val prefs = QuietPreferences(context)
        val repo = PolicyRepository(prefs)
        repo.applyPreset(PresetIds.BALANCED)
        repo.setSurfacePolicy("instagram.home_feed", quiet.core.rules.Policy.GATE)
        val snap = prefs.readSnapshot()
        assertEquals(PresetIds.CUSTOM, snap.presetId)
    }

    @Test
    fun libraryCrud() = runBlocking {
        val id = library.insert(
            app.quiet.data.db.LibraryItemEntity(
                title = "Show",
                type = "Shows",
                status = "Want",
                progress = "",
                rating = 0,
                notes = "",
                updatedAt = System.currentTimeMillis(),
            ),
        )
        assertTrue(id > 0)
        val items = library.all()
        assertEquals(1, items.size)
        library.delete(items.first())
        assertTrue(library.all().isEmpty())
    }
}
