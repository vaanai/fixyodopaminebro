package quiet.core

import org.junit.Test
import quiet.core.detect.Detector
import quiet.core.engine.Command
import quiet.core.engine.CoverReason
import quiet.core.engine.Decision
import quiet.core.engine.Engine
import quiet.core.engine.EngineInput
import quiet.core.engine.EventType
import quiet.core.engine.ScrollInfo
import quiet.core.model.UiTree
import quiet.core.rules.AppRules
import quiet.core.settings.FocusWindow
import quiet.core.settings.Mode
import quiet.core.settings.Origin
import quiet.core.settings.Settings
import quiet.core.settings.UnlockKind
import quiet.core.stats.DayStats
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EngineTest {
    private val ig = Fixtures.rules("instagram")
    private val tt = Fixtures.rules("tiktok")
    private val zone: ZoneId = ZoneOffset.UTC
    // Thursday 2026-10-22 12:00 UTC
    private var now = LocalDateTime.of(2026, 10, 22, 12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
    private var settings = Settings()
    private var today = DayStats("2026-10-22")
    private var scroll: ScrollInfo? = null
    private val engine = Engine()

    private fun see(rules: AppRules, tree: UiTree, advanceMs: Long = 200): Decision {
        now += advanceMs
        return engine.evaluate(EngineInput(now, zone, rules, Detector.read(rules, tree), settings, today, scroll))
    }

    private var pagerIndex = 0

    private fun swipe() {
        pagerIndex++
        scroll = ScrollInfo(now + 50, fromPager = true, index = pagerIndex)
    }

    @Test fun `reels tab is covered above the tab bar`() {
        settings = settings.withApp("instagram") { it.copy(autoSkipTarget = null) }
        val d = see(ig, Fixtures.igReelsTab())
        val cover = assertNotNull(d.cover)
        assertEquals(CoverReason.BLOCKED, cover.reason)
        assertEquals(2250, cover.bottomPx)
        assertTrue(cover.canUnlock)
        assertTrue(d.events.any { it.type == EventType.COVER_SHOWN })
    }

    @Test fun `cover shown event fires once per landing`() {
        see(ig, Fixtures.igReelsTab())
        val again = see(ig, Fixtures.igReelsTab())
        assertNotNull(again.cover)
        assertFalse(again.events.any { it.type == EventType.COVER_SHOWN })
    }

    @Test fun `home feed is allowed by default`() {
        assertNull(see(ig, Fixtures.igHome()).cover)
    }

    @Test fun `friend's reel from a chat plays, swiping on is stopped`() {
        see(ig, Fixtures.igThread())
        val first = see(ig, Fixtures.igViewer("friend"))
        assertNull(first.cover)
        assertTrue(first.isViewer)

        // Caption expands without a scroll: not a swipe.
        assertNull(see(ig, Fixtures.igViewer("friend", caption = "caption expanded")).cover)

        swipe()
        val next = see(ig, Fixtures.igViewer("stranger"), advanceMs = 900)
        val cover = assertNotNull(next.cover)
        assertEquals(CoverReason.SWIPE_LIMIT, cover.reason)
        assertEquals(Origin.MESSAGES, cover.origin)
        assertTrue(cover.offerBack)
        assertNull(cover.bottomPx)
    }

    @Test fun `profile origin allows browsing a few reels`() {
        val profile = Screen(Fixtures.IG).apply {
            node(id = "${Fixtures.IG}:id/profile_header_container", top = 200, bottom = 800)
        }.build()
        see(ig, profile)
        see(ig, Fixtures.igViewer("friend"))
        for (i in 1..10) {
            swipe()
            assertNull(see(ig, Fixtures.igViewer("friend" + ('a' + i)), advanceMs = 900).cover, "swipe $i")
        }
        swipe()
        assertEquals(CoverReason.SWIPE_LIMIT, see(ig, Fixtures.igViewer("stranger"), advanceMs = 900).cover?.reason)
    }

    @Test fun `opening a reel doesn't count as a swipe`() {
        see(ig, Fixtures.igThread())
        see(ig, Fixtures.igViewer("friend", caption = "loading"))
        // The open animation scrolls the pager and the full caption arrives a moment later.
        scroll = ScrollInfo(now + 50, fromPager = true, index = 3)
        assertNull(see(ig, Fixtures.igViewer("friend"), advanceMs = 400).cover)
        assertNull(see(ig, Fixtures.igViewer("friend"), advanceMs = 700).cover)
        // A real swipe afterwards is still caught.
        scroll = ScrollInfo(now + 50, fromPager = true, index = 4)
        assertEquals(CoverReason.SWIPE_LIMIT, see(ig, Fixtures.igViewer("stranger"), advanceMs = 900).cover?.reason)
    }

    @Test fun `unlimited swipes setting never covers`() {
        settings = settings.withApp("instagram") { it.copy(viewerSwipes = it.viewerSwipes + (Origin.MESSAGES to -1)) }
        see(ig, Fixtures.igThread())
        see(ig, Fixtures.igViewer("a"))
        repeat(5) { i ->
            swipe()
            assertNull(see(ig, Fixtures.igViewer("b" + ('a' + i)), advanceMs = 900).cover)
        }
    }

    @Test fun `rapid scroll noise inside one swipe counts once`() {
        settings = settings.withApp("instagram") { it.copy(viewerSwipes = it.viewerSwipes + (Origin.MESSAGES to 1)) }
        see(ig, Fixtures.igThread())
        see(ig, Fixtures.igViewer("a"))
        swipe()
        assertNull(see(ig, Fixtures.igViewer("mid"), advanceMs = 900).cover)
        assertNull(see(ig, Fixtures.igViewer("b"), advanceMs = 100).cover)
        assertEquals(1, engine.snapshot().swipes)
    }

    @Test fun `unknown screens hold the cover briefly then fail open`() {
        see(ig, Fixtures.igReelsTab())
        val blank = Screen(Fixtures.IG).build()
        assertNotNull(see(ig, blank, advanceMs = 500).cover)
        assertNull(see(ig, blank, advanceMs = 2000).cover)
    }

    @Test fun `a truncated read doesn't override the last full one`() {
        see(ig, Fixtures.igReelsTab())
        val partial = Fixtures.igHome().let { UiTree(it.packageName, it.nodes, it.screenWidth, it.screenHeight, truncated = true) }
        assertNotNull(see(ig, partial, advanceMs = 300).cover)
        // Still only partial reads after the hold: go with them.
        assertNull(see(ig, partial, advanceMs = 2000).cover)
    }

    @Test fun `unlock opens the app for its minutes`() {
        see(ig, Fixtures.igReelsTab())
        engine.unlock("instagram", 5, now)
        val d = see(ig, Fixtures.igReelsTab())
        assertNull(d.cover)
        assertNotNull(d.pill)
        now += 5 * 60_000
        assertNotNull(see(ig, Fixtures.igReelsTab()).cover)
    }

    @Test fun `no unlock offered when out of unlocks or unlocks are off`() {
        today = today.copy(unlocks = 3)
        assertFalse(see(ig, Fixtures.igReelsTab()).cover!!.canUnlock)
        today = today.copy(unlocks = 0)
        settings = settings.copy(unlocksEnabled = false)
        engine.onLeftApp()
        assertFalse(see(ig, Fixtures.igReelsTab()).cover!!.canUnlock)
    }

    @Test fun `the cover offers the unlock you chose`() {
        settings = settings.withApp("instagram") { it.copy(autoSkipTarget = null) }
        assertEquals(listOf(UnlockKind.VIDEOS), see(ig, Fixtures.igReelsTab()).cover!!.unlockOffers)
        settings = settings.copy(unlockKind = UnlockKind.MINUTES)
        assertEquals(listOf(UnlockKind.MINUTES), see(ig, Fixtures.igReelsTab()).cover!!.unlockOffers)
        settings = settings.copy(unlockKind = UnlockKind.CHOOSE)
        assertEquals(listOf(UnlockKind.VIDEOS, UnlockKind.MINUTES), see(ig, Fixtures.igReelsTab()).cover!!.unlockOffers)
    }

    @Test fun `a whole-app limit only unlocks by time`() {
        settings = settings.withApp("instagram") { it.copy(dailyLimitMinutes = 30) }
        today = today.copy(appSeconds = mapOf(Fixtures.IG to 30 * 60))
        val cover = see(ig, Fixtures.igHome()).cover!!
        assertEquals(CoverReason.APP_LIMIT, cover.reason)
        assertEquals(listOf(UnlockKind.MINUTES), cover.unlockOffers)
    }

    @Test fun `blocked home keeps the stories row uncovered`() {
        settings = settings.withApp("instagram") { it.copy(modes = it.modes + ("home" to Mode.BLOCK)) }
        val cover = assertNotNull(see(ig, Fixtures.igHomeWithStories(trayBottom = 560)).cover)
        assertEquals(CoverReason.BLOCKED, cover.reason)
        assertEquals(550, cover.topPx)
        assertEquals(2250, cover.bottomPx)
        assertEquals("Stories", cover.keptName)
    }

    @Test fun `blocked home covers everything when keeping stories is off or the row is gone`() {
        settings = settings.withApp("instagram") { it.copy(modes = it.modes + ("home" to Mode.BLOCK)) }
        assertNull(see(ig, Fixtures.igHome()).cover!!.topPx)
        settings = settings.withApp("instagram") { it.copy(keepVisible = emptySet()) }
        val cover = see(ig, Fixtures.igHomeWithStories()).cover!!
        assertNull(cover.topPx)
        assertNull(cover.keptName)
    }

    @Test fun `allowed home with a stories row is not covered`() {
        assertNull(see(ig, Fixtures.igHomeWithStories()).cover)
    }

    @Test fun `limit mode spends the shared allowance then blocks`() {
        settings = settings.withApp("instagram") { it.copy(modes = it.modes + ("reels" to Mode.LIMIT)) }
        val ok = see(ig, Fixtures.igReelsTab())
        assertNull(ok.cover)
        assertTrue(ok.chargesBudget)
        assertEquals("20:00 left today", ok.pill?.text)
        today = today.copy(budgetSeconds = 20 * 60)
        assertEquals(CoverReason.BUDGET_DONE, see(ig, Fixtures.igReelsTab()).cover?.reason)
    }

    @Test fun `pause mode asks first and continue opens the surface`() {
        settings = settings.withApp("instagram") { it.copy(modes = it.modes + ("reels" to Mode.PAUSE)) }
        val d = see(ig, Fixtures.igReelsTab())
        assertEquals(CoverReason.PAUSE, d.cover?.reason)
        assertFalse(d.cover!!.canUnlock)
        engine.continueAfterPause("instagram", "reels", 5, now)
        assertNull(see(ig, Fixtures.igReelsTab()).cover)
    }

    @Test fun `focus window blocks even allowed video feeds and disables unlocks`() {
        settings = settings.copy(
            focusWindows = listOf(FocusWindow("w", "Work", setOf(DayOfWeek.THURSDAY), 9 * 60, 17 * 60)),
        ).withApp("tiktok") { it.copy(autoSkipTarget = null) }
        val friendsFeed = Screen(Fixtures.TT).apply {
            bottomTab(label = "Friends", selected = true, asText = true)
        }.build()
        val d = see(tt, friendsFeed)
        assertEquals(CoverReason.FOCUS, d.cover?.reason)
        assertFalse(d.cover!!.canUnlock)
    }

    @Test fun `focus window crossing midnight`() {
        val w = FocusWindow("n", "Night", setOf(DayOfWeek.THURSDAY), 23 * 60, 7 * 60)
        assertTrue(w.contains(DayOfWeek.THURSDAY, 23 * 60 + 30))
        assertTrue(w.contains(DayOfWeek.FRIDAY, 6 * 60))
        assertFalse(w.contains(DayOfWeek.FRIDAY, 23 * 60 + 30))
        assertFalse(w.contains(DayOfWeek.THURSDAY, 12 * 60))
    }

    @Test fun `tiktok lands on for you and gets skipped to inbox`() {
        val d = see(tt, Fixtures.ttForYou())
        assertNotNull(d.cover)
        assertEquals(Command.Navigate("tiktok", "inbox"), d.command)
        assertTrue(d.events.any { it.type == EventType.AUTO_SKIP })
        // Still on the feed shortly after: no hammering.
        assertNull(see(tt, Fixtures.ttForYou(), advanceMs = 300).command)
        // One retry, then give up and just keep it covered.
        assertNotNull(see(tt, Fixtures.ttForYou(), advanceMs = 1200).command)
        assertNull(see(tt, Fixtures.ttForYou(), advanceMs = 1500).command)
        assertNotNull(see(tt, Fixtures.ttForYou(), advanceMs = 100).cover)
        // Reaching the inbox clears everything.
        assertNull(see(tt, Fixtures.ttInbox()).cover)
    }

    @Test fun `instagram reels jumps to messages, coming straight back shows the options`() {
        val d = see(ig, Fixtures.igReelsTab())
        assertEquals(Command.Navigate("instagram", "messages"), d.command)
        see(ig, Fixtures.igThread())
        val back = see(ig, Fixtures.igReelsTab(), advanceMs = 3000)
        assertNull(back.command)
        assertNotNull(back.cover)
    }

    @Test fun `coming back after a skip shows the options even if the chats screen never read`() {
        assertEquals(Command.Navigate("instagram", "messages"), see(ig, Fixtures.igReelsTab()).command)
        val blank = Screen(Fixtures.IG).build()
        see(ig, blank, advanceMs = 500)
        see(ig, blank, advanceMs = 2000)
        val back = see(ig, Fixtures.igReelsTab(), advanceMs = 3000)
        assertNull(back.command)
        assertNotNull(back.cover)
    }

    @Test fun `a partial read of a chat is never held under a cover`() {
        see(ig, Fixtures.igReelsTab())
        val thread = Fixtures.igThread().let { UiTree(it.packageName, it.nodes, it.screenWidth, it.screenHeight, truncated = true) }
        assertNull(see(ig, thread, advanceMs = 300).cover)
    }

    @Test fun `watch N more unlock counts swipes`() {
        settings = settings.withApp("instagram") { it.copy(autoSkipTarget = null) }
        see(ig, Fixtures.igReelsTab("anna"))
        engine.unlockVideos("instagram", 2, now)
        assertEquals("2 more", see(ig, Fixtures.igReelsTab("anna")).pill?.text)
        swipe()
        assertNull(see(ig, Fixtures.igReelsTab("bert"), advanceMs = 900).cover)
        swipe()
        val last = see(ig, Fixtures.igReelsTab("cleo"), advanceMs = 900)
        assertNull(last.cover)
        assertEquals("Last one", last.pill?.text)
        swipe()
        assertNotNull(see(ig, Fixtures.igReelsTab("dave"), advanceMs = 900).cover)
    }

    @Test fun `a video settling after a swipe isn't counted again`() {
        settings = settings.withApp("instagram") { it.copy(autoSkipTarget = null) }
        see(ig, Fixtures.igReelsTab("anna"))
        engine.unlockVideos("instagram", 2, now)
        see(ig, Fixtures.igReelsTab("anna"), advanceMs = 900)
        swipe()
        assertEquals("1 more", see(ig, Fixtures.igReelsTab("bert"), advanceMs = 900).pill?.text)
        // The pager settles on the same item while the caption loads.
        scroll = ScrollInfo(now + 50, fromPager = true, index = pagerIndex)
        assertEquals("1 more", see(ig, Fixtures.igReelsTab("bert"), advanceMs = 900).pill?.text)
        scroll = ScrollInfo(now + 50, fromPager = true)
        assertEquals("1 more", see(ig, Fixtures.igReelsTab("bert_loaded"), advanceMs = 900).pill?.text)
    }

    @Test fun `an unlock running out mid-feed shows the cover instead of skipping`() {
        see(ig, Fixtures.igReelsTab("anna"))
        see(ig, Fixtures.igThread())
        engine.unlockVideos("instagram", 0, now)
        assertNull(see(ig, Fixtures.igReelsTab("anna"), advanceMs = 30_000).cover)
        swipe()
        val ended = see(ig, Fixtures.igReelsTab("bert"), advanceMs = 900)
        assertNotNull(ended.cover)
        assertNull(ended.command)
        assertNull(see(ig, Fixtures.igReelsTab("bert"), advanceMs = 5_000).command)
    }

    @Test fun `the options cover stays put while you wait to unlock`() {
        see(ig, Fixtures.igReelsTab())
        see(ig, Fixtures.igThread())
        assertNull(see(ig, Fixtures.igReelsTab(), advanceMs = 3000).command)
        // Past the 20 s return window, still sitting on the cover: no skip from under it.
        for (i in 1..10) assertNull(see(ig, Fixtures.igReelsTab(), advanceMs = 3000).command, "tick $i")
    }

    @Test fun `app daily limit covers the app but keeps chats`() {
        settings = settings.withApp("instagram") { it.copy(dailyLimitMinutes = 30) }
        today = today.copy(appSeconds = mapOf(Fixtures.IG to 30 * 60))
        assertEquals(CoverReason.APP_LIMIT, see(ig, Fixtures.igHome()).cover?.reason)
        assertNull(see(ig, Fixtures.igThread()).cover)
    }

    @Test fun `protection off or app disabled does nothing`() {
        settings = settings.copy(protectionOn = false)
        assertNull(see(ig, Fixtures.igReelsTab()).cover)
        settings = Settings().withApp("instagram") { it.copy(enabled = false) }
        assertNull(see(ig, Fixtures.igReelsTab()).cover)
    }

    @Test fun `full screen style ignores the tab bar`() {
        settings = settings.copy(coverStyle = quiet.core.settings.CoverStyle.FULL_SCREEN)
        assertNull(see(ig, Fixtures.igReelsTab()).cover?.bottomPx)
    }

    @Test fun `clock formatting`() {
        assertEquals("0:05", Engine.formatClock(4_200))
        assertEquals("5:00", Engine.formatClock(300_000))
        assertEquals("1h 30m", Engine.formatClock(90 * 60_000L))
    }
}
