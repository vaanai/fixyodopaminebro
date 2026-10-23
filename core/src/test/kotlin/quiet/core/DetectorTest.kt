package quiet.core

import org.junit.Test
import quiet.core.Fixtures.IG
import quiet.core.Fixtures.TT
import quiet.core.detect.Detector
import quiet.core.rules.SurfaceKind
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DetectorTest {
    private val ig = Fixtures.rules("instagram")
    private val tt = Fixtures.rules("tiktok")

    @Test fun `instagram reels tab is the video feed`() {
        val r = Detector.read(ig, Fixtures.igReelsTab())
        assertEquals("reels", r.surface)
        assertEquals(SurfaceKind.VIDEO_FEED, r.kind)
        assertEquals(2250, r.tabBarTop)
    }

    @Test fun `instagram home feed is allowed surface`() {
        assertEquals("home", Detector.read(ig, Fixtures.igHome()).surface)
    }

    @Test fun `instagram thread is messages`() {
        assertEquals(SurfaceKind.MESSAGES, Detector.read(ig, Fixtures.igThread()).kind)
    }

    @Test fun `instagram reel without tab bar is a single viewer`() {
        val r = Detector.read(ig, Fixtures.igViewer("friend"))
        assertEquals("reel_viewer", r.surface)
        assertNull(r.tabBarTop)
    }

    @Test fun `signature changes with the author but not with like counts`() {
        val a = Detector.read(ig, Fixtures.igViewer("alice")).signature
        val b = Detector.read(ig, Fixtures.igViewer("bob")).signature
        assertNotEquals(a, b)
        assertTrue(a.isNotEmpty())
    }

    @Test fun `signature ignores ticking counters`() {
        fun viewer(likes: String) = Screen(IG).apply {
            node(id = "$IG:id/clips_viewer_view_pager", top = 0, bottom = 2400)
            node(id = "$IG:id/clips_author_username", text = "dj_2004", top = 1900, bottom = 1950)
            node(desc = "Like video. $likes likes", top = 1500, bottom = 1600)
        }.build()
        assertEquals(Detector.read(ig, viewer("1.2M")).signature, Detector.read(ig, viewer("1.3M")).signature)
    }

    @Test fun `tiktok for you with visible selection`() {
        val r = Detector.read(tt, Fixtures.ttForYou())
        assertEquals("for_you", r.surface)
        assertEquals(2250, r.tabBarTop)
    }

    @Test fun `tiktok home feed counts as for you even when selection is not exposed`() {
        assertEquals("for_you", Detector.read(tt, Fixtures.ttForYou(selectionVisible = false)).surface)
    }

    @Test fun `tiktok inbox is messages`() {
        assertEquals(SurfaceKind.MESSAGES, Detector.read(tt, Fixtures.ttInbox()).kind)
    }

    @Test fun `tiktok video without top tabs is a single viewer`() {
        val tree = Screen(TT).apply {
            node(desc = "Like video. 10 likes", top = 1200, bottom = 1300)
            node(text = "@friend", top = 1900, bottom = 1950)
        }.build()
        assertEquals("video_viewer", Detector.read(tt, tree).surface)
    }

    @Test fun `off-screen pages do not count`() {
        val tree = Screen(IG).apply {
            node(id = "$IG:id/clips_viewer_view_pager", top = 0, bottom = 2400, visible = false)
        }.build()
        assertTrue(Detector.read(ig, tree).isUnknown)
    }

    @Test fun `nav target resolves to clickable tab`() {
        val tree = Fixtures.ttForYou()
        val node = Detector.findNavNode(tt, tree, "inbox")!!
        assertTrue(node.clickable)
        assertEquals(2250, node.top)
    }

    @Test fun `empty tree is unknown`() {
        assertTrue(Detector.read(ig, Screen(IG).build()).isUnknown)
    }
}
