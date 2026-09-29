package dev.dimvlachos.lab.agenticdemo.data

import kotlinx.coroutines.flow.Flow

/**
 * The agent side of the loop: one turn in (user text, or an A2UI action/error), chunks out. Turns
 * must not overlap; the caller runs them one at a time. A failed turn throws [AgentFailure].
 */
interface ConciergeAgent {
    fun send(turn: String): Flow<AgentChunk>

    /** Releases the HTTP client; the agent is not used again. */
    fun close() {}
}
