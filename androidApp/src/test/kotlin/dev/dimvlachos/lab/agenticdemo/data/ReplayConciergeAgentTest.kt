package dev.dimvlachos.lab.agenticdemo.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking

class ReplayConciergeAgentTest {
    private val scripts =
        mapOf(
            "turn1" to listOf("Options.", "{\"options\":1}"),
            "turn2" to listOf("Form."),
            "turn3" to listOf("Booked."),
            "again" to listOf("Only once."),
        )
    private val agent = ReplayConciergeAgent({ scripts.getValue(it) }, linePace = Duration.ZERO)

    @Test
    fun eachTurnPlaysItsScript() = runBlocking {
        assertEquals(
            listOf(AgentChunk.Prose("Options."), AgentChunk.A2uiLine("{\"options\":1}")),
            agent.send("Dinner").toList(),
        )
        assertEquals(listOf(AgentChunk.Prose("Form.")), agent.send(action("choose")).toList())
        assertEquals(listOf(AgentChunk.Prose("Booked.")), agent.send(action("book")).toList())
    }

    @Test
    fun aScriptPlaysOnceSoItsSurfacesAreNotCreatedTwice() = runBlocking {
        agent.send("Dinner").toList()

        assertEquals(listOf(AgentChunk.Prose("Only once.")), agent.send("Again").toList())
    }

    @Test
    fun errorReportsGetNoAnswer() = runBlocking {
        val error = """{"version":"v0.9","error":{"code":"VALIDATION_FAILED","message":"x"}}"""

        assertEquals(emptyList(), agent.send(error).toList())
    }

    private fun action(name: String) = """{"version":"v0.9","action":{"name":"$name"}}"""
}
