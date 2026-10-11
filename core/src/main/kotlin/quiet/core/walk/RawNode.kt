package quiet.core.walk

interface RawNode {
    val text: CharSequence?
    val contentDescription: CharSequence?
    val viewIdResourceName: String?
    val isSelected: Boolean
    val isClickable: Boolean
    val className: CharSequence?
    val boundsLeft: Int
    val boundsTop: Int
    val boundsRight: Int
    val boundsBottom: Int
    val childCount: Int
    fun childAt(index: Int): RawNode?
}
