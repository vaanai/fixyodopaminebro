package quiet.core.model

data class NodeInfo(
    val text: String? = null,
    val contentDescription: String? = null,
    val viewId: String? = null,
    val isSelected: Boolean = false,
    val isClickable: Boolean = false,
    val className: String? = null,
    val boundsLeft: Int = 0,
    val boundsTop: Int = 0,
    val boundsRight: Int = 0,
    val boundsBottom: Int = 0,
) {
    val boundsArea: Int
        get() = (boundsRight - boundsLeft).coerceAtLeast(0) * (boundsBottom - boundsTop).coerceAtLeast(0)

    fun labelTexts(): List<String> = listOfNotNull(text, contentDescription).filter { it.isNotBlank() }
}
