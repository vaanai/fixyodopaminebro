package app.quiet.data.library

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object LibraryTitleFormatter {
    fun normalize(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return trimmed
        if (!trimmed.contains('%')) return trimmed
        return try {
            URLDecoder.decode(trimmed, StandardCharsets.UTF_8.name())
        } catch (_: IllegalArgumentException) {
            trimmed.replace("%20", " ")
        }
    }
}
