package dev.dimvlachos.lab.agenticdemo.presentation

import androidx.a2ui.compose.runtime.A2uiMessageParser
import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.A2uiMessageProcessor
import androidx.a2ui.model.processor.A2uiMessageParser as MessageParser
import androidx.a2ui.model.processor.A2uiSurfaceModel
import androidx.a2ui.model.processor.processInput
import androidx.a2ui.model.protocol.A2uiClientErrorMessage
import androidx.a2ui.model.protocol.A2uiClientEventMessage
import androidx.a2ui.model.protocol.A2uiClientToServerMessage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import dev.dimvlachos.lab.agenticdemo.data.A2uiWireMapper
import dev.dimvlachos.lab.agenticdemo.data.AgentChunk
import dev.dimvlachos.lab.agenticdemo.data.ComponentReferenceCheck
import dev.dimvlachos.lab.agenticdemo.data.ConciergeAgent
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * The whole A2UI loop in one place: agent lines go into the processor, which owns the surfaces;
 * taps come back out of it as events and go to the agent as its next turn; errors the renderer
 * reports are batched and sent back too, so the agent can correct itself (a few times at most).
 * Turns run one at a time, in order.
 */
class ConciergeViewModel(
    private val agent: ConciergeAgent?,
    catalog: A2uiCatalog,
    private val parser: MessageParser<String> = A2uiMessageParser(),
    backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val errorSettle: Duration = 800.milliseconds,
) : ViewModel() {
    private val processor = A2uiMessageProcessor(catalogs = listOf(catalog))
    private val log = Logger.withTag("Concierge")

    val surfaces: StateFlow<List<A2uiSurfaceModel>> = processor.activeSurfaces

    private val _state = MutableStateFlow(ConciergeState(hasApiKey = agent != null))
    val state: StateFlow<ConciergeState> = _state.asStateFlow()

    private val turns = Channel<Turn>(Channel.UNLIMITED)
    private val pendingErrors = mutableListOf<String>()
    private val references = ComponentReferenceCheck()
    private val reportedMissing = mutableSetOf<String>()
    private var errorFlush: Job? = null
    private var corrections = 0
    private var nextKey = 0

    init {
        viewModelScope.launch(backgroundDispatcher) { processor.collectMessages() }
        // The outbound flow has no replay, so collection has to be live before the first surface.
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            processor.outboundEvents.collect(::onOutbound)
        }
        viewModelScope.launch { for (turn in turns) run(turn) }
    }

    fun onSend(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || agent == null) return
        append(TranscriptItem.User(key(), trimmed))
        corrections = 0
        turns.trySend(Turn(trimmed))
    }

    private fun onOutbound(message: A2uiClientToServerMessage) {
        val wire = A2uiWireMapper.toWire(message)
        when (message) {
            is A2uiClientEventMessage -> {
                append(TranscriptItem.Action(key(), message.type))
                corrections = 0
                turns.trySend(Turn(wire))
            }
            is A2uiClientErrorMessage -> {
                log.w { "A2UI error: $wire" }
                pendingErrors += wire
                if (errorFlush?.isActive != true) {
                    errorFlush = viewModelScope.launch {
                        // Errors arrive one per bad component; let a burst settle into one turn.
                        delay(errorSettle)
                        val batch = pendingErrors.toList()
                        pendingErrors.clear()
                        if (corrections < MAX_CORRECTIONS) {
                            corrections++
                            append(TranscriptItem.Correction(key(), batch.size))
                            turns.trySend(Turn(batch.joinToString("\n")))
                        }
                    }
                }
            }
        }
    }

    private suspend fun run(turn: Turn) {
        val agent = agent ?: return
        _state.update { it.copy(thinking = true) }
        try {
            agent.send(turn.text).collect(::onChunk)
            reportMissingComponents()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            log.e(failure) { "Agent turn failed" }
            append(TranscriptItem.Failure(key(), FailureReason.Network))
        } finally {
            _state.update { it.copy(thinking = false) }
        }
    }

    private fun onChunk(chunk: AgentChunk) {
        when (chunk) {
            is AgentChunk.Prose -> append(TranscriptItem.Agent(key(), chunk.text))
            is AgentChunk.A2uiLine -> {
                log.d { "A2UI in: ${chunk.json}" }
                references.accept(chunk.json)
                createdSurfaceId(chunk.json)?.let { append(TranscriptItem.Surface(key(), it)) }
                processor.processInput(parser, chunk.json)
            }
            AgentChunk.Refused -> append(TranscriptItem.Failure(key(), FailureReason.Refused))
        }
    }

    // Once the turn is over, a child that never arrived is a mistake, not a pending update.
    private fun reportMissingComponents() {
        references.missing().forEach { (surfaceId, ids) ->
            val fresh = ids.filter { reportedMissing.add("$surfaceId/$it") }
            if (fresh.isEmpty()) return@forEach
            onOutbound(
                A2uiClientErrorMessage(
                    code = "VALIDATION_FAILED",
                    surfaceId = surfaceId,
                    message =
                        "Components referenced but never defined: ${fresh.joinToString()}. " +
                            "Send an updateComponents that defines them.",
                    context = mapOf("path" to "/components"),
                )
            )
        }
    }

    // A surface gets its place in the transcript where the agent created it.
    private fun createdSurfaceId(json: String): String? =
        runCatching { JSONObject(json).optJSONObject("createSurface")?.optString("surfaceId") }
            .getOrNull()
            ?.takeIf { it.isNotEmpty() }

    private fun append(item: TranscriptItem) {
        _state.update { it.copy(transcript = it.transcript + item) }
    }

    private fun key() = (nextKey++).toString()

    private class Turn(val text: String)

    private companion object {
        const val MAX_CORRECTIONS = 2
    }
}
