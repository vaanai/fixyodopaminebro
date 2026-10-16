package quiet.core.classify

import quiet.core.model.NodeInfo

object ConversationHeuristics {
    private val sendMarkers = setOf("Send", "Post", "GIF", "Sticker", "Aa")
    private val messageHints = listOf("Message", "Say something", "Write a message")

    /**
     * True when the tree looks like an open DM thread but the messages tab is not selected.
     * Used to fail open instead of treating a stale feed tab selection as a feed surface.
     */
    fun looksLikeOpenThread(nodes: List<NodeInfo>, messagesTabLabels: List<String>): Boolean {
        if (SurfaceClassifier.isMessagesTabSelected(nodes, messagesTabLabels)) {
            return false
        }
        if (nodes.any { node ->
            node.labelTexts().any { label ->
                label.startsWith("quiet.synthetic.", ignoreCase = true) && label.contains("dm_thread", ignoreCase = true)
            }
        }) {
            return true
        }
        val hasSend = nodes.any { node ->
            node.labelTexts().any { label -> sendMarkers.any { it.equals(label, ignoreCase = true) } }
        }
        val hasComposer = nodes.any { node ->
            val cls = node.className?.contains("EditText", ignoreCase = true) == true
            val hint = node.labelTexts().any { text ->
                messageHints.any { hint -> text.contains(hint, ignoreCase = true) }
            }
            cls && hint
        }
        return hasSend || hasComposer
    }
}
