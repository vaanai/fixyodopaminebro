package app.quiet.data.library

import androidx.test.core.app.ApplicationProvider
import app.quiet.QuietApplication
import app.quiet.data.db.LibraryItemEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LibraryTitleFormatterTest {
    @Test
    fun decodesPercentEncodedSpaces() {
        assertEquals("Slow Horses", LibraryTitleFormatter.normalize("Slow%20Horses"))
    }

    @Test
    fun titleWithSpaceRoundTrips() {
        val app = ApplicationProvider.getApplicationContext<QuietApplication>()
        runBlocking {
            app.libraryRepository.deleteAll()
            val item = LibraryItemEntity(
                id = 0,
                title = LibraryTitleFormatter.normalize("Slow Horses"),
                type = "Shows",
                status = "Want",
                progress = "",
                rating = 0,
                notes = "",
                updatedAt = System.currentTimeMillis(),
            )
            app.libraryRepository.insert(item)
            val loaded = app.libraryRepository.all().single()
            assertEquals("Slow Horses", loaded.title)
            assertFalse(loaded.title.contains("%"))
        }
    }
}
