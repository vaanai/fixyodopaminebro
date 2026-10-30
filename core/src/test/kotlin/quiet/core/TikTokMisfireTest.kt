package quiet.core

import org.junit.Test
import quiet.core.detect.Detector
import quiet.core.engine.CoverReason
import quiet.core.engine.Decision
import quiet.core.engine.Engine
import quiet.core.engine.EngineInput
import quiet.core.engine.ScrollInfo
import quiet.core.model.UiNode
import quiet.core.model.UiTree
import quiet.core.rules.AppRules
import quiet.core.settings.Mode
import quiet.core.settings.Settings
import quiet.core.stats.DayStats
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A tester set TikTok's For You to Allow and still got blocked now and then, full screen, with a "Go back"
 * that left TikTok. These pin down each way that happened.
 */
class TikTokMisfireTest {
    private val tt = Fixtures.rules("tiktok")
    private val zone = ZoneOffset.UTC
    private var now = LocalDateTime.of(2026, 10, 22, 12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
    private var settings = Settings()
    private var today = DayStats("2026-10-22")
    private var scroll: ScrollInfo? = null
    private var fromApp = false
    private var pagerIndex = 0
    private val engine = Engine()

    private val forYouAllowed: Settings
        get() = Settings().withApp("tiktok") { it.copy(modes = it.modes + ("for_you" to Mode.ALLOW)) }

    private fun see(rules: AppRules, tree: UiTree, advanceMs: Long = 200): Decision {
        now += advanceMs
        return engine.evaluate(EngineInput(now, zone, rules, Detector.read(rules, tree), settings, today, scroll, fromApp))
    }

    private fun swipe() {
        pagerIndex++
        scroll = ScrollInfo(now + 50, fromPager = true, index = pagerIndex)
    }

    /**
     * A swipe, then a read once the last one's hold is over: cut reads within [Engine.UNKNOWN_HOLD_MS] of
     * each other keep the decision before them.
     */
    private fun swipeAndSee(tree: UiTree): Decision {
        now += Engine.UNKNOWN_HOLD_MS
        swipe()
        return see(tt, tree, advanceMs = 100)
    }

    /** For You read in part: the walk ran out inside the video, before any tab. Looks like a single video. */
    private fun forYouCut(author: String): UiTree = Screen(Fixtures.TT).apply {
        node(text = author, top = 1900, bottom = 1950)
        node(desc = "Like video. 1.2M likes", clickable = true, top = 1200, bottom = 1300)
    }.build().cut()

    private fun UiTree.cut() = UiTree(packageName, nodes, screenWidth, screenHeight, truncated = true)

    @Test fun `for you read as a single video isn't covered when for you is allowed`() {
        settings = forYouAllowed
        see(tt, Fixtures.ttInbox())
        assertTrue(see(tt, forYouCut("@one"), advanceMs = 2000).isViewer)
        for (i in 1..5) {
            assertNull(swipeAndSee(forYouCut("@next$i")).cover, "swipe $i")
        }
    }

    @Test fun `the same read is still stopped when for you is blocked`() {
        see(tt, Fixtures.ttInbox())
        see(tt, forYouCut("@one"), advanceMs = 2000)
        assertEquals(CoverReason.SWIPE_LIMIT, swipeAndSee(forYouCut("@two")).cover?.reason)
    }

    @Test fun `a shared video swipes freely when for you is allowed`() {
        settings = forYouAllowed
        see(tt, Fixtures.ttInbox())
        see(tt, Fixtures.ttSharedVideo())
        assertNull(swipeAndSee(forYouCut("@stranger")).cover)
    }

    @Test fun `limited for you with allowance left counts swiping on against the allowance`() {
        settings = Settings().withApp("tiktok") { it.copy(modes = it.modes + ("for_you" to Mode.LIMIT)) }
        see(tt, Fixtures.ttInbox())
        see(tt, Fixtures.ttSharedVideo())
        val d = swipeAndSee(forYouCut("@stranger"))
        assertNull(d.cover)
        assertTrue(d.chargesBudget)
        today = today.copy(budgetSeconds = settings.budgetMinutes * 60)
        assertEquals(CoverReason.SWIPE_LIMIT, swipeAndSee(forYouCut("@another")).cover?.reason)
    }

    @Test fun `focus still stops swiping on even when for you is allowed`() {
        settings = forYouAllowed.copy(focusUntil = now + 3_600_000)
        see(tt, Fixtures.ttInbox())
        see(tt, Fixtures.ttSharedVideo())
        assertEquals(CoverReason.SWIPE_LIMIT, swipeAndSee(forYouCut("@stranger")).cover?.reason)
    }

    @Test fun `switching to tiktok from another app doesn't limit an allowed for you`() {
        settings = forYouAllowed
        fromApp = true
        see(tt, Fixtures.ttForYou(author = "@one"))
        see(tt, Fixtures.ttForYou(author = "@one"), advanceMs = Engine.LINK_SETTLE_MS)
        for (i in 1..4) {
            swipe()
            val d = see(tt, Fixtures.ttForYou(author = "@next$i"), advanceMs = 1000)
            assertNull(d.cover, "swipe $i")
            assertTrue(d.isFeed)
        }
    }

    @Test fun `a link's cover over the feed offers the inbox, not Back`() {
        fromApp = true
        see(tt, Fixtures.ttForYou(author = "@friend"))
        see(tt, Fixtures.ttForYou(author = "@friend"), advanceMs = Engine.LINK_SETTLE_MS)
        swipe()
        val cover = assertNotNull(see(tt, Fixtures.ttForYou(author = "@stranger"), advanceMs = 900).cover)
        assertEquals(CoverReason.SWIPE_LIMIT, cover.reason)
        assertTrue(cover.overFeed)
        assertTrue(cover.fromLink)
        assertEquals("inbox", cover.safeTarget)
    }

    @Test fun `a video opened from a chat is not over a feed`() {
        see(tt, Fixtures.ttInbox())
        see(tt, Fixtures.ttSharedVideo())
        val cover = assertNotNull(swipeAndSee(forYouCut("@stranger")).cover)
        assertFalse(cover.overFeed)
        assertEquals("inbox", cover.safeTarget)
    }

    @Test fun `a LIVE preview in For You is still For You`() {
        settings = forYouAllowed
        val preview = Screen(Fixtures.TT).apply {
            topTab("Following")
            topTab("For You", selected = true)
            node(id = "${Fixtures.TT}:id/ttlive_preview_container", top = 0, bottom = 2250)
            for (label in listOf("Home", "Friends", "Inbox", "Profile")) bottomTab(label = label, selected = label == "Home", asText = true)
        }.build()
        val r = Detector.read(tt, preview)
        assertEquals("for_you", r.surface)
        assertNull(see(tt, preview).cover)
    }

    @Test fun `a LIVE room on its own is still LIVE`() {
        val room = Screen(Fixtures.TT).apply {
            node(id = "${Fixtures.TT}:id/ttlive_link_layout_layer_root", top = 0, bottom = 2400)
        }.build()
        assertEquals("live", Detector.read(tt, room).surface)
    }

    @Test fun `tiktok looks its tabs up when a read is cut short`() {
        val words = tt.tabWords
        assertTrue("For You" in words)
        assertTrue("Inbox" in words)
        assertTrue("Home" in words)
        // Longest first, so a short word can't stand in for a longer label.
        assertEquals(words.sortedByDescending { it.length }, words)
        assertTrue(Fixtures.rules("instagram").tabWords.isEmpty())
    }

    @Test fun `tabs found after a cut read make it For You again`() {
        val cut = forYouCut("@one")
        assertEquals("video_viewer", Detector.read(tt, cut).surface)
        val withTabs = cut.withExtra(
            UiNode(0, -1, 0, text = "For You", selected = true, top = 120, bottom = 200, right = 400, extra = true),
            UiNode(0, -1, 0, text = "Home", selected = true, top = 2300, bottom = 2400, right = 200, extra = true),
        )
        val r = Detector.read(tt, withTabs)
        assertEquals("for_you", r.surface)
        // What was looked up doesn't change which video this is.
        assertEquals(Detector.read(tt, cut).signature, r.signature)
    }

    @Test fun `safe target is the auto-skip target or a chat tab`() {
        assertEquals("inbox", tt.safeTarget)
        assertEquals("messages", Fixtures.rules("instagram").safeTarget)
    }

    @Test fun `main feed is the launch surface or the first video feed`() {
        assertEquals("for_you", tt.mainFeed)
        assertEquals("reels", Fixtures.rules("instagram").mainFeed)
        assertEquals("shorts", Fixtures.rules("youtube").mainFeed)
    }

    private fun UiTree.withExtra(vararg extra: UiNode): UiTree {
        val all = nodes + extra.mapIndexed { i, n -> n.copy(index = nodes.size + i) }
        return UiTree(packageName, all, screenWidth, screenHeight, truncated)
    }
}
