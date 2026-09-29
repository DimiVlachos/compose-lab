package dev.dimvlachos.lab.agenticdemo.data

import android.content.res.AssetManager
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow

/**
 * A scripted agent for working on the UI and recording clips without an API key or a bill: a user
 * message gets the options, "choose" the booking form, "book" the confirmation, each played line by
 * line at a model-like pace. Each script plays once, since replaying one would re-create surfaces
 * that already exist; after that the agent says so. Error reports get no answer: a script cannot
 * correct itself.
 */
class ReplayConciergeAgent(
    private val readScript: (name: String) -> List<String>,
    private val linePace: Duration = 450.milliseconds,
) : ConciergeAgent {
    constructor(
        assets: AssetManager
    ) : this({ name ->
        assets.open("a2ui/replay/$name.jsonl").bufferedReader().use { it.readLines() }
    })

    private val played = mutableSetOf<String>()

    override fun send(turn: String): Flow<AgentChunk> {
        if (turn.contains("\"error\"")) return emptyFlow()
        val script =
            when {
                turn.contains("\"name\":\"choose\"") -> "turn2"
                turn.contains("\"name\":\"book\"") -> "turn3"
                else -> "turn1"
            }
        val name = if (played.add(script)) script else "again"
        return flow {
            val splitter = JsonlLineSplitter()
            for (line in readScript(name)) {
                delay(linePace)
                splitter.push(line + "\n").forEach { emit(it) }
            }
        }
    }
}
