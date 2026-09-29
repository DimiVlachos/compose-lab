package dev.dimvlachos.lab.agenticdemo.data

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.helpers.MessageAccumulator
import com.anthropic.models.messages.CacheControlEphemeral
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.MessageParam
import com.anthropic.models.messages.OutputConfig
import com.anthropic.models.messages.StopReason
import com.anthropic.models.messages.TextBlockParam
import kotlin.jvm.optionals.getOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Claude as the A2UI agent. Streams each reply and cuts it into lines as it arrives, and keeps the
 * whole conversation (full assistant messages, thinking included) so later turns can refer back to
 * the surfaces it already built.
 */
class ClaudeConciergeAgent(apiKey: String, systemPrompt: () -> String) : ConciergeAgent {
    private val client: AnthropicClient = AnthropicOkHttpClient.builder().apiKey(apiKey).build()
    // Built on first use, off the main thread: it reads the schema assets and exports the catalog.
    private val system by lazy {
        listOf(
            TextBlockParam.builder()
                .text(systemPrompt())
                .cacheControl(CacheControlEphemeral.builder().build())
                .build()
        )
    }
    private val history = mutableListOf<MessageParam>()
    private val lock = Mutex()

    override fun send(turn: String): Flow<AgentChunk> =
        flow {
                lock.withLock {
                    history +=
                        MessageParam.builder().role(MessageParam.Role.USER).content(turn).build()
                    val accumulator = MessageAccumulator.create()
                    val splitter = JsonlLineSplitter()
                    try {
                        client.messages().createStreaming(params()).use { response ->
                            for (event in response.stream().iterator()) {
                                accumulator.accumulate(event)
                                val delta =
                                    event
                                        .contentBlockDelta()
                                        .getOrNull()
                                        ?.delta()
                                        ?.text()
                                        ?.getOrNull()
                                if (delta != null) splitter.push(delta.text()).forEach { emit(it) }
                            }
                        }
                    } catch (failure: Throwable) {
                        history.removeAt(history.lastIndex)
                        throw failure
                    }
                    splitter.flush().forEach { emit(it) }
                    val message = accumulator.message()
                    history += message.toParam()
                    if (message.stopReason().getOrNull() == StopReason.REFUSAL) {
                        emit(AgentChunk.Refused)
                    }
                }
            }
            .flowOn(Dispatchers.IO)

    private fun params(): MessageCreateParams =
        MessageCreateParams.builder()
            .model(MODEL)
            .maxTokens(16_000L)
            .systemOfTextBlockParams(system)
            .messages(history.toList())
            .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.LOW).build())
            // Route a refused request to a fallback model server-side instead of just stopping.
            .putAdditionalHeader("anthropic-beta", "server-side-fallback-2026-07-01")
            .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
            .build()

    private companion object {
        const val MODEL = "claude-opus-5-5"
    }
}
