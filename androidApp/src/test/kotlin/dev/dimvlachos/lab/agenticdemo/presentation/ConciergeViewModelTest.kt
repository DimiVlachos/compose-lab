package dev.dimvlachos.lab.agenticdemo.presentation

import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.dimvlachos.lab.agenticdemo.catalog.RatingBarComponent
import dev.dimvlachos.lab.agenticdemo.catalog.StatTileComponent
import dev.dimvlachos.lab.agenticdemo.data.AgentChunk
import dev.dimvlachos.lab.agenticdemo.data.AgentFailure
import dev.dimvlachos.lab.agenticdemo.data.ConciergeAgent
import dev.dimvlachos.lab.agenticdemo.data.JvmA2uiParser
import java.io.IOException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout

@OptIn(ExperimentalCoroutinesApi::class)
class ConciergeViewModelTest {
    private val catalog =
        A2uiCatalog(catalogId = CATALOG, components = listOf(RatingBarComponent, StatTileComponent))

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun aReplyLandsAsProseAndASurfaceInTranscriptOrder() = runBlocking {
        val agent =
            FakeAgent(
                listOf(AgentChunk.Prose("Here are three."), create("options_1"), stat("options_1"))
            )
        val viewModel = viewModel(agent)

        viewModel.onSend("  Dinner for 4  ")

        val transcript = viewModel.awaitTranscript { it.size == 3 }
        assertEquals(TranscriptItem.User("0", "Dinner for 4"), transcript[0])
        assertEquals(TranscriptItem.Agent("1", "Here are three."), transcript[1])
        assertEquals("options_1", (transcript[2] as TranscriptItem.Surface).surfaceId)
        withTimeout(5.seconds) { viewModel.state.first { "options_1" in it.seenSurfaces } }
        assertEquals(listOf("Dinner for 4"), agent.turns)
        assertTrue(viewModel.state.value.transcript.none { it is TranscriptItem.Correction })
    }

    @Test
    fun aBadLineIsSentBackToTheAgentOnce() = runBlocking {
        val agent =
            FakeAgent(listOf(create("options_1"), AgentChunk.A2uiLine("{\"version\":\"v0.9\"")))
        val viewModel = viewModel(agent)

        viewModel.onSend("Dinner")

        val transcript = viewModel.awaitTranscript { t ->
            t.any { it is TranscriptItem.Correction }
        }
        withTimeout(5.seconds) { agent.turnCount.first { it >= 2 } }
        assertEquals(1, transcript.count { it is TranscriptItem.Correction })
        assertTrue(agent.turns[1].contains("\"error\""), agent.turns[1])
    }

    @Test
    fun aChildThatNeverArrivesIsSentBackAsAValidationError() = runBlocking {
        val dangling =
            AgentChunk.A2uiLine(
                """{"version":"v0.9","updateComponents":{"surfaceId":"options_1","components":[""" +
                    """{"id":"root","component":"Card","child":"missing_label"}]}}"""
            )
        val agent = FakeAgent(listOf(create("options_1"), dangling))
        val viewModel = viewModel(agent)

        viewModel.onSend("Dinner")

        withTimeout(5.seconds) { agent.turnCount.first { it >= 2 } }
        assertTrue(agent.turns[1].contains("missing_label"), agent.turns[1])
        assertTrue(agent.turns[1].contains("VALIDATION_FAILED"), agent.turns[1])
    }

    @Test
    fun aSurfaceSentAgainMovesDownInsteadOfShowingTwice() = runBlocking {
        val agent =
            FakeAgent(
                listOf(create("options_1"), stat("options_1")),
                listOf(AgentChunk.Prose("Fixed."), create("options_1"), stat("options_1")),
            )
        val viewModel = viewModel(agent)

        viewModel.onSend("Dinner")
        viewModel.awaitTranscript { t -> t.any { it is TranscriptItem.Surface } }
        viewModel.onSend("Again")

        val transcript = viewModel.awaitTranscript { t ->
            t.any { it == TranscriptItem.Agent(it.key, "Fixed.") }
        }
        val surfaces = transcript.filterIsInstance<TranscriptItem.Surface>()
        assertEquals(1, surfaces.size)
        assertEquals(transcript.last(), surfaces.single())
    }

    @Test
    fun aSurfaceTheRendererNeverCreatesIsMarkedFailed() = runBlocking {
        val unknownCatalog =
            AgentChunk.A2uiLine(
                """{"version":"v0.9","createSurface":{"surfaceId":"ghost","catalogId":"https://nope"}}"""
            )
        val viewModel = viewModel(FakeAgent(listOf(unknownCatalog)))

        viewModel.onSend("Dinner")

        withTimeout(5.seconds) { viewModel.state.first { "ghost" in it.failedSurfaces } }
        assertTrue("ghost" !in viewModel.state.value.seenSurfaces)
    }

    @Test
    fun aFailedTurnSaysWhyItFailed() = runBlocking {
        val agent = FakeAgent(failure = AgentFailure(AgentFailure.Reason.Auth, IOException("401")))
        val viewModel = viewModel(agent)

        viewModel.onSend("Dinner")

        val transcript = viewModel.awaitTranscript { t -> t.any { it is TranscriptItem.Failure } }
        assertEquals(
            FailureReason.Auth,
            transcript.filterIsInstance<TranscriptItem.Failure>().single().reason,
        )
        assertEquals(false, viewModel.state.value.thinking)
    }

    @Test
    fun anUnexpectedErrorCountsAsANetworkFailure() = runBlocking {
        val viewModel = viewModel(FakeAgent(failure = IllegalStateException("boom")))

        viewModel.onSend("Dinner")

        val transcript = viewModel.awaitTranscript { t -> t.any { it is TranscriptItem.Failure } }
        assertEquals(
            FailureReason.Network,
            transcript.filterIsInstance<TranscriptItem.Failure>().single().reason,
        )
    }

    @Test
    fun leavingTheDemoClosesTheAgent() {
        val agent = FakeAgent()
        val store = ViewModelStore()
        ViewModelProvider.create(
                store,
                viewModelFactory {
                    initializer { ConciergeViewModel(agent, catalog, JvmA2uiParser) }
                },
            )[ConciergeViewModel::class]

        store.clear()

        assertTrue(agent.closed)
    }

    @Test
    fun withoutAnAgentNothingIsSent() {
        val viewModel = ConciergeViewModel(agent = null, catalog = catalog)

        viewModel.onSend("Dinner")

        assertEquals(false, viewModel.state.value.hasApiKey)
        assertEquals(emptyList(), viewModel.state.value.transcript)
    }

    private fun viewModel(agent: ConciergeAgent) =
        ConciergeViewModel(agent, catalog, JvmA2uiParser, errorSettle = 10.milliseconds)

    private suspend fun ConciergeViewModel.awaitTranscript(
        until: (List<TranscriptItem>) -> Boolean
    ): List<TranscriptItem> =
        withTimeout(5.seconds) { state.first { until(it.transcript) } }.transcript

    private fun create(surfaceId: String) =
        AgentChunk.A2uiLine(
            """{"version":"v0.9","createSurface":{"surfaceId":"$surfaceId","catalogId":"$CATALOG"}}"""
        )

    private fun stat(surfaceId: String) =
        AgentChunk.A2uiLine(
            """{"version":"v0.9","updateComponents":{"surfaceId":"$surfaceId","components":[""" +
                """{"id":"root","component":"StatTile","label":"Price","value":"€30"}]}}"""
        )

    /** Plays one reply per turn, in order, then nothing; or fails every turn with [failure]. */
    private class FakeAgent(
        private vararg val replies: List<AgentChunk>,
        private val failure: Exception? = null,
    ) : ConciergeAgent {
        val turns = mutableListOf<String>()
        val turnCount = MutableStateFlow(0)
        var closed = false

        override fun send(turn: String): Flow<AgentChunk> {
            turns += turn
            turnCount.value = turns.size
            failure?.let { error ->
                return flow { throw error }
            }
            return flowOf(*replies.getOrElse(turns.lastIndex) { emptyList() }.toTypedArray())
        }

        override fun close() {
            closed = true
        }
    }

    private companion object {
        const val CATALOG = "https://example.test/catalog.json"
    }
}
