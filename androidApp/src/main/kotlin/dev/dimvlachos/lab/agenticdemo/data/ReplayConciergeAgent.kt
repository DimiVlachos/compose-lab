package dev.dimvlachos.lab.agenticdemo.data

import android.content.res.AssetManager
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * A scripted agent for working on the UI and recording clips without an API key or a bill: a user
 * message gets the options, "choose" the booking form, "book" the confirmation, each played line by
 * line from `assets/a2ui/replay/` at a model-like pace.
 */
class ReplayConciergeAgent(private val assets: AssetManager) : ConciergeAgent {
    override fun send(turn: String): Flow<AgentChunk> =
        flow {
                val splitter = JsonlLineSplitter()
                val script = assets.open("a2ui/replay/turn${scriptFor(turn)}.jsonl")
                script.bufferedReader().useLines { lines ->
                    for (line in lines) {
                        delay(LinePace)
                        splitter.push(line + "\n").forEach { emit(it) }
                    }
                }
            }
            .flowOn(Dispatchers.IO)

    private fun scriptFor(turn: String) =
        when {
            turn.contains("\"name\":\"choose\"") -> 2
            turn.contains("\"name\":\"book\"") -> 3
            else -> 1
        }

    private companion object {
        val LinePace = 450.milliseconds
    }
}
