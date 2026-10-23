package app.quiet.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import quiet.core.detect.Reading
import quiet.core.engine.Decision
import quiet.core.engine.EngineSnapshot

/** What the service is seeing right now. Feeds the Inspector and the "service running" checks. */
data class Live(
    val connected: Boolean = false,
    val lastHeartbeat: Long = 0L,
    val foreground: String? = null,
    val reading: Reading? = null,
    val decision: Decision? = null,
    val engine: EngineSnapshot? = null,
    val nodeCount: Int = 0,
    val walkMs: Long = 0,
    val lastScrollAt: Long = 0L,
    val lastDump: String? = null,
    val unlockedUntil: Map<String, Long> = emptyMap(),
)

object LiveState {
    private val _state = MutableStateFlow(Live())
    val state: StateFlow<Live> = _state.asStateFlow()

    fun update(f: (Live) -> Live) {
        _state.value = f(_state.value)
    }
}
