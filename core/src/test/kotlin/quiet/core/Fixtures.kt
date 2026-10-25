package quiet.core

import quiet.core.model.UiNode
import quiet.core.model.UiTree
import quiet.core.model.UiTreeBuilder
import quiet.core.rules.AppRules
import quiet.core.rules.RuleParser

/** Tiny DSL for synthetic screens. Coordinates assume a 1080x2400 phone. */
class Screen(pkg: String) {
    private val b = UiTreeBuilder(pkg)
    private val root = b.add(-1) { i, d -> UiNode(i, -1, d, cls = "FrameLayout", right = 1080, bottom = 2400) }

    fun node(
        parent: Int = root,
        id: String? = null,
        text: String? = null,
        desc: String? = null,
        hint: String? = null,
        cls: String? = null,
        selected: Boolean = false,
        clickable: Boolean = false,
        editable: Boolean = false,
        visible: Boolean = true,
        top: Int = 100,
        bottom: Int = 200,
        left: Int = 0,
        right: Int = 1080,
    ): Int = b.add(parent) { i, d ->
        UiNode(
            i, parent, d, id = id, text = text, desc = desc, hint = hint, cls = cls, selected = selected,
            clickable = clickable, editable = editable, visible = visible, left = left, top = top, right = right, bottom = bottom,
        )
    }

    /** A bottom tab: clickable container with a label; `selected` lives on the container. */
    fun bottomTab(id: String? = null, label: String, selected: Boolean = false, asText: Boolean = false): Int {
        val tab = node(id = id, desc = if (asText) null else label, clickable = true, selected = selected, top = 2250, bottom = 2400)
        if (asText) node(parent = tab, text = label, top = 2340, bottom = 2390)
        return tab
    }

    fun topTab(label: String, selected: Boolean = false): Int =
        node(text = label, selected = selected, clickable = true, top = 120, bottom = 200)

    fun build(): UiTree = b.build()
}

object Fixtures {
    fun rules(name: String): AppRules =
        RuleParser.parse(javaClass.classLoader.getResource("rules/$name.json")!!.readText())

    const val IG = "com.instagram.android"
    const val TT = "com.zhiliaoapp.musically"

    fun igTabs(s: Screen, selected: String) {
        s.node(id = "$IG:id/tab_bar", top = 2250, bottom = 2400)
        for ((id, label) in listOf("feed_tab" to "Home", "search_tab" to "Search and explore", "clips_tab" to "Reels", "direct_tab" to "Messages", "profile_tab" to "Profile")) {
            s.bottomTab(id = "$IG:id/$id", label = label, selected = id == selected)
        }
    }

    fun igReelsTab(author: String = "creator_one"): UiTree = Screen(IG).apply {
        node(id = "$IG:id/clips_viewer_view_pager", top = 0, bottom = 2250)
        node(id = "$IG:id/clips_author_username", text = author, top = 1900, bottom = 1950)
        igTabs(this, "clips_tab")
    }.build()

    fun igHome(): UiTree = Screen(IG).apply {
        node(text = "friend posted a photo", top = 400, bottom = 500)
        igTabs(this, "feed_tab")
    }.build()

    /** Home with the Stories row: a tray container holding story bubbles, each with a name under it. */
    fun igHomeWithStories(trayTop: Int = 230, trayBottom: Int = 560, byIdOnly: Boolean = false): UiTree = Screen(IG).apply {
        val tray = node(id = if (byIdOnly) "$IG:id/reels_tray_container" else null, top = trayTop, bottom = trayBottom)
        if (!byIdOnly) {
            for ((i, name) in listOf("Your story", "sam's story at column 2. Unseen.").withIndex()) {
                val item = node(parent = tray, top = trayTop + 20, bottom = trayBottom - 10, left = i * 260, right = i * 260 + 240)
                node(parent = item, desc = name, clickable = true, top = trayTop + 20, bottom = trayBottom - 80, left = i * 260, right = i * 260 + 240)
                node(parent = item, text = name.substringBefore("'s"), top = trayBottom - 70, bottom = trayBottom - 10, left = i * 260, right = i * 260 + 240)
            }
        }
        node(text = "friend posted a photo", top = trayBottom + 40, bottom = trayBottom + 140)
        igTabs(this, "feed_tab")
    }.build()

    fun igThread(): UiTree = Screen(IG).apply {
        node(text = "lol watch this", top = 400, bottom = 500)
        node(id = "$IG:id/row_thread_composer_edittext", hint = "Message...", cls = "android.widget.EditText", editable = true, top = 2200, bottom = 2300)
    }.build()

    /** A reel opened full screen, no tab bar (how Instagram shows a reel from a chat). */
    fun igViewer(author: String, caption: String = "caption of $author"): UiTree = Screen(IG).apply {
        node(id = "$IG:id/clips_viewer_view_pager", top = 0, bottom = 2400)
        node(id = "$IG:id/clips_author_username", text = author, top = 1900, bottom = 1950)
        node(id = "$IG:id/clips_caption", text = caption, top = 1960, bottom = 2050)
        node(desc = "Like", clickable = true, top = 1500, bottom = 1600)
        node(text = "12.3K", top = 1600, bottom = 1650)
    }.build()

    fun ttForYou(selectionVisible: Boolean = true, author: String = "@dancer"): UiTree = Screen(TT).apply {
        topTab("Following")
        topTab("For You", selected = selectionVisible)
        node(text = author, top = 1900, bottom = 1950)
        node(desc = "Like video. 1.2M likes", clickable = true, top = 1200, bottom = 1300)
        node(desc = "Read or add comments. 3,456 comments", clickable = true, top = 1350, bottom = 1450)
        for (label in listOf("Home", "Friends", "Inbox", "Profile")) {
            bottomTab(label = label, selected = selectionVisible && label == "Home", asText = true)
        }
    }.build()

    /** TikTok mid cold start: the read ran out of time inside the video pager, before reaching any tabs. */
    fun ttStartingUp(): UiTree = Screen(TT).apply {
        node(desc = "Like video. 1,442 likes", clickable = true, left = 904, top = 1273, bottom = 1438)
    }.build().let { UiTree(it.packageName, it.nodes, it.screenWidth, it.screenHeight, truncated = true) }

    /** A single video opened from a link: read in full, and there are no tabs. */
    fun ttSharedVideo(): UiTree = Screen(TT).apply {
        node(text = "@friend", top = 1900, bottom = 1950)
        node(desc = "Like video. 88 likes", clickable = true, top = 1200, bottom = 1300)
    }.build()

    /** TikTok's splash screen: nothing to read yet. */
    fun ttSplash(): UiTree = Screen(TT).apply { node(cls = "ImageView", top = 1000, bottom = 1400) }.build()

    fun ttInbox(): UiTree = Screen(TT).apply {
        node(text = "Messages", top = 300, bottom = 400)
        for (label in listOf("Home", "Friends", "Inbox", "Profile")) {
            bottomTab(label = label, selected = label == "Inbox", asText = true)
        }
    }.build()
}
