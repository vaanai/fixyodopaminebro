package quiet.core.fixtures

import quiet.core.model.NodeInfo

object SyntheticTrees {
    fun node(
        text: String? = null,
        contentDescription: String? = null,
        selected: Boolean = false,
        viewId: String? = null,
    ) = NodeInfo(
        text = text,
        contentDescription = contentDescription,
        isSelected = selected,
        viewId = viewId,
    )

    fun instagramDmInbox() = listOf(
        node("Home"),
        node("Reels"),
        node("Messages", selected = true),
    )

    fun instagramDmThread() = listOf(
        node("quiet.synthetic.instagram.dm_thread"),
    )

    fun instagramHomeFeed() = listOf(
        node("Home", selected = true),
        node("Reels"),
        node("Messages"),
    )

    fun instagramReels() = listOf(
        node("Home"),
        node("Reels", selected = true),
    )

    fun instagramExplore() = listOf(node("quiet.synthetic.instagram.explore"))
    fun instagramStories() = listOf(node("quiet.synthetic.instagram.stories"))
    fun instagramSharedReel() = listOf(node("quiet.synthetic.instagram.dm_shared_reel"))
    fun instagramProfile() = listOf(node("quiet.synthetic.instagram.profile"))

    fun instagramHomeWithStoriesTray() = listOf(
        node("Stories"),
        node("Home", selected = true),
        node("Reels"),
    )

    fun threadNoTabBar() = listOf(
        node("Home"),
        node("Reels"),
        node("Stories"),
        node("quiet.synthetic.instagram.dm_thread"),
    )

    fun tiktokDmInbox() = listOf(node("Inbox", selected = true))
    fun tiktokDmThread() = listOf(node("quiet.synthetic.tiktok.dm_thread"))
    fun tiktokForYou() = listOf(
        node("For You", selected = true),
        node("Inbox"),
    )

    fun tiktokFollowing() = listOf(node("Following", selected = true))
    fun tiktokDiscover() = listOf(node("quiet.synthetic.tiktok.discover"))
    fun tiktokSharedVideo() = listOf(node("quiet.synthetic.tiktok.dm_shared_video"))
    fun tiktokProfile() = listOf(node("quiet.synthetic.tiktok.profile"))

    fun tiktokForYouWithUnselectedInbox() = listOf(
        node("For You", selected = true),
        node("Inbox"),
    )

    fun tiktokInboxSelectedForYouText() = listOf(
        node("For You"),
        node("Inbox", selected = true),
    )

    fun protectedAndFeed() = listOf(
        node("Home", selected = true),
        node("Messages", selected = true),
    )
}
