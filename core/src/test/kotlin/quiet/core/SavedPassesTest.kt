package quiet.core

import org.junit.Test
import quiet.core.engine.Engine
import quiet.core.engine.SavedPasses
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SavedPassesTest {
    private val now = 1_800_000_000_000L

    @Test fun `unlocks come back after a restart`() {
        val before = Engine()
        before.unlock("instagram", 5, now)
        before.unlockVideos("tiktok", 3, now)
        before.continueAfterPause("instagram", "explore", 5, now)
        val text = SavedPasses.encode(before.savedPasses(now).copy(generic = mapOf("com.reddit.frontpage" to now + 60_000)))

        val saved = SavedPasses.decode(text)
        assertEquals(now + 60_000, saved.generic["com.reddit.frontpage"])
        val after = Engine()
        after.restorePasses(saved, now + 1_000)
        assertEquals(now + 5 * 60_000, after.unlockedUntil("instagram", now + 1_000))
        assertEquals(3, after.videosLeft("tiktok", now + 1_000))
        assertEquals(before.savedPasses(now + 1_000), after.savedPasses(now + 1_000))
    }

    @Test fun `what ran out while Quiet was stopped stays gone`() {
        val before = Engine()
        before.unlock("instagram", 5, now)
        val after = Engine()
        after.restorePasses(before.savedPasses(now), now + 6 * 60_000)
        assertNull(after.unlockedUntil("instagram", now + 6 * 60_000))
    }

    @Test fun `a broken file means no unlocks, not a crash`() {
        assertEquals(SavedPasses(), SavedPasses.decode("{not json"))
        assertEquals(SavedPasses(), SavedPasses.decode(null))
    }
}
