package quiet.core.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import quiet.core.classify.SurfaceClassifier
import quiet.core.fixtures.SyntheticTrees
import quiet.core.walk.FakeRawNode
import quiet.core.walk.TreeWalker

class ClassifierTest {
    private lateinit var instagram: AppRules
    private lateinit var tiktok: AppRules

    @Before
    fun loadRules() {
        val igJson = readResource("rules/instagram.json")
        val ttJson = readResource("rules/tiktok.json")
        instagram = RuleParser.parseResource(igJson, "instagram.json").first!!
        tiktok = RuleParser.parseResource(ttJson, "tiktok.json").first!!
    }

    @Test
    fun syntheticFixturesClassify() {
        assertSurface(instagram, SyntheticTrees.instagramDmInbox(), "instagram.dm_inbox")
        assertSurface(instagram, SyntheticTrees.instagramDmThread(), "instagram.dm_thread")
        assertSurface(instagram, SyntheticTrees.instagramHomeFeed(), "instagram.home_feed")
        assertSurface(instagram, SyntheticTrees.instagramReels(), "instagram.reels")
        assertSurface(instagram, SyntheticTrees.instagramExplore(), "instagram.explore")
        assertSurface(instagram, SyntheticTrees.instagramStories(), "instagram.stories")
        assertSurface(instagram, SyntheticTrees.instagramSharedReel(), "instagram.dm_shared_reel")
        assertSurface(instagram, SyntheticTrees.instagramProfile(), "instagram.profile")

        assertSurface(tiktok, SyntheticTrees.tiktokDmInbox(), "tiktok.dm_inbox")
        assertSurface(tiktok, SyntheticTrees.tiktokDmThread(), "tiktok.dm_thread")
        assertSurface(tiktok, SyntheticTrees.tiktokForYou(), "tiktok.for_you")
        assertSurface(tiktok, SyntheticTrees.tiktokFollowing(), "tiktok.following")
        assertSurface(tiktok, SyntheticTrees.tiktokDiscover(), "tiktok.discover")
        assertSurface(tiktok, SyntheticTrees.tiktokSharedVideo(), "tiktok.dm_shared_video")
        assertSurface(tiktok, SyntheticTrees.tiktokProfile(), "tiktok.profile")
    }

    @Test
    fun protectedFirst() {
        val result = SurfaceClassifier.classify(SyntheticTrees.protectedAndFeed(), instagram, truncated = false)
        assertEquals("instagram.dm_inbox", result.surfaceId)
    }

    @Test
    fun messagesTabWinsOverFeedLabels() {
        val nodes = listOf(
            SyntheticTrees.node("Home"),
            SyntheticTrees.node("Reels"),
            SyntheticTrees.node("Messages", selected = true),
        )
        val result = SurfaceClassifier.classify(nodes, instagram, truncated = false)
        assertEquals("instagram.dm_inbox", result.surfaceId)
    }

    @Test
    fun homeWithStoriesTray() {
        val result = SurfaceClassifier.classify(SyntheticTrees.instagramHomeWithStoriesTray(), instagram, truncated = false)
        assertEquals("instagram.home_feed", result.surfaceId)
    }

    @Test
    fun threadNoTabBarNeverFeed() {
        val result = SurfaceClassifier.classify(SyntheticTrees.threadNoTabBar(), instagram, truncated = false)
        assertTrue(result.surfaceId == "instagram.dm_thread" || result.isUnknown)
        assertTrue(result.surfaceId != "instagram.home_feed")
        assertTrue(result.surfaceId != "instagram.reels")
    }

    @Test
    fun tiktokForYouWithUnselectedInbox() {
        val result = SurfaceClassifier.classify(SyntheticTrees.tiktokForYouWithUnselectedInbox(), tiktok, truncated = false)
        assertEquals("tiktok.for_you", result.surfaceId)
    }

    @Test
    fun inboxSelectedBeatsForYouText() {
        val result = SurfaceClassifier.classify(SyntheticTrees.tiktokInboxSelectedForYouText(), tiktok, truncated = false)
        assertEquals("tiktok.dm_inbox", result.surfaceId)
    }

    @Test
    fun emptyIsUnknown() {
        val result = SurfaceClassifier.classify(emptyList(), instagram, truncated = false)
        assertTrue(result.isUnknown)
    }

    @Test
    fun caseInsensitiveExactMatch() {
        val nodes = listOf(SyntheticTrees.node("home", selected = true))
        val result = SurfaceClassifier.classify(nodes, instagram, truncated = false)
        assertEquals("instagram.home_feed", result.surfaceId)
    }

    @Test
    fun containsIsNotUsed() {
        val nodes = listOf(SyntheticTrees.node("Homework", selected = true))
        val result = SurfaceClassifier.classify(nodes, instagram, truncated = false)
        assertTrue(result.isUnknown)
    }

    @Test
    fun selectedTabAfterNoiseStillFound() {
        val children = (1..600).map { FakeRawNode(text = "noise$it") } +
            FakeRawNode(text = "Home", isSelected = true)
        val root = FakeRawNode(children = children)
        val walk = TreeWalker.walk(root)
        val result = SurfaceClassifier.classify(walk.nodes, instagram, walk.truncated)
        assertEquals("instagram.home_feed", result.surfaceId)
    }

    @Test
    fun timeCapYieldsUnknownForWeakMatch() {
        var calls = 0
        val clock = {
            calls++
            if (calls <= 1) 0L else 31_000_000L
        }
        val root = FakeRawNode(children = listOf(FakeRawNode(text = "quiet.synthetic.instagram.explore")))
        val walk = TreeWalker.walk(root, clock)
        val result = SurfaceClassifier.classify(walk.nodes, instagram, walk.truncated)
        assertTrue(result.isUnknown)
    }

    @Test
    fun shippedFilesValidateAndEnforceIds() {
        val ig = RuleParser.parseResource(readResource("rules/instagram.json"), "instagram.json").first!!
        val tt = RuleParser.parseResource(readResource("rules/tiktok.json"), "tiktok.json").first!!
        assertEquals(false, ig.verified)
        assertEquals(false, tt.verified)
        val enforced = (ig.surfaces + tt.surfaces).filter { it.enforce }.map { it.fullId }.toSet()
        assertEquals(
            setOf(
                "instagram.dm_inbox",
                "instagram.dm_thread",
                "instagram.home_feed",
                "instagram.reels",
                "tiktok.dm_inbox",
                "tiktok.dm_thread",
                "tiktok.for_you",
                "tiktok.following",
            ),
            enforced,
        )
    }

    @Test
    fun invalidJsonAllowOtherApp() {
        val good = readResource("rules/tiktok.json")
        val parsed = RuleParser.loadAll("instagram.json" to "{bad", "tiktok.json" to good)
        assertTrue(parsed.errors.containsKey("instagram"))
        assertEquals(1, parsed.apps.size)
        assertEquals("tiktok", parsed.apps.first().app)
    }

    private fun assertSurface(rules: AppRules, nodes: List<quiet.core.model.NodeInfo>, expected: String) {
        val result = SurfaceClassifier.classify(nodes, rules, truncated = false)
        assertEquals(expected, result.surfaceId)
    }

    private fun readResource(path: String): String =
        ClassLoader.getSystemResource(path)?.readText()
            ?: error("Missing resource $path")
}
