package dev.dimvlachos.lab.agenticdemo.data

import kotlinx.coroutines.flow.Flow

/** The agent side of the loop: one turn in (user text, or an A2UI action/error), chunks out. */
interface ConciergeAgent {
    fun send(turn: String): Flow<AgentChunk>
}
