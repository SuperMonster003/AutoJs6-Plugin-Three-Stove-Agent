package io.github.supermonster003.autojs6.plugin.ai.agent.nodes

/** Stability describes the bounded accessibility sample, never the whole application. */
class ScreenStability(private val startedAt: Long) {
    enum class State { WAITING, STABLE, TIMED_OUT }
    private var fingerprint: List<Any>? = null
    private var changedAt = startedAt
    fun sample(snapshot: CompactNodeText.Snapshot, now: Long): State {
        val value = listOf(snapshot.window, snapshot.truncated, snapshot.nodes.map {
            listOf(it.fingerprint(snapshot.window), it.depth, it.flags.sorted(), it.bounds)
        })
        if (value != fingerprint) { fingerprint = value; changedAt = now }
        return when {
            now - changedAt >= QUIET_MS -> State.STABLE
            now - startedAt >= MAX_WAIT_MS -> State.TIMED_OUT
            else -> State.WAITING
        }
    }
    companion object { const val QUIET_MS = 500L; const val POLL_MS = 250L; const val MAX_WAIT_MS = 3000L }
}
