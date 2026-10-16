package quiet.core.classify

import quiet.core.model.NodeInfo

object ConversationHeuristics {
    private val messageHints = listOf("Message", "Say something", "Write a message")

    /**
     * True when the tree looks like an open DM thread but the messages tab is not selected.
     * Send / Post / GIF / Sticker / Aa alone are not used — those appear on reels and feeds too.
     */
    fun looksLikeOpenThread(nodes: List<NodeInfo>, messagesTabLabels: List<String>): Boolean {
        if (SurfaceClassifier.isMessagesTabSelected(nodes, messagesTabLabels)) {
            return false
        }
        if (nodes.any { node ->
            node.labelTexts().any { label ->
                label.startsWith("quiet.synthetic.", ignoreCase = true) &&
                    label.contains("dm_thread", ignoreCase = true)
            }
        }) {
            return true
        }
        return hasMessageComposer(nodes)
    }

    fun hasMessageComposer(nodes: List<NodeInfo>): Boolean {
        return nodes.any { node ->
            val isEditText = node.className?.contains("EditText", ignoreCase = true) == true
            val hint = node.labelTexts().any { text ->
                messageHints.any { hint -> text.contains(hint, ignoreCase = true) }
            }
            isEditText && hint
        }
    }

}
