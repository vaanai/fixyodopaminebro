package quiet.core.walk

class FakeRawNode(
    override val text: CharSequence? = null,
    override val contentDescription: CharSequence? = null,
    override val viewIdResourceName: String? = null,
    override val isSelected: Boolean = false,
    override val isClickable: Boolean = false,
    override val className: CharSequence? = null,
    override val boundsLeft: Int = 0,
    override val boundsTop: Int = 0,
    override val boundsRight: Int = 100,
    override val boundsBottom: Int = 100,
    private val children: List<FakeRawNode> = emptyList(),
) : RawNode {
    override val childCount: Int = children.size
    override fun childAt(index: Int): RawNode? = children.getOrNull(index)
}
