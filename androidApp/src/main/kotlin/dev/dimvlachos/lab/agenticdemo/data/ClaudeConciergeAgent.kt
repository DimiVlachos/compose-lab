package dev.dimvlachos.lab.agenticdemo.data

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.errors.BadRequestException
import com.anthropic.errors.NotFoundException
import com.anthropic.errors.PermissionDeniedException
import com.anthropic.errors.RateLimitException
import com.anthropic.errors.UnauthorizedException
import com.anthropic.helpers.MessageAccumulator
import com.anthropic.models.messages.CacheControlEphemeral
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.MessageParam
import com.anthropic.models.messages.OutputConfig
import com.anthropic.models.messages.StopReason
import com.anthropic.models.messages.TextBlockParam
import kotlin.coroutines.cancellation.CancellationException
import kotlin.jvm.optionals.getOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

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

    override fun send(turn: String): Flow<AgentChunk> =
        flow {
                history += message(MessageParam.Role.USER, turn)
                val accumulator = MessageAccumulator.create()
                val splitter = JsonlLineSplitter()
                val reply = StringBuilder()
                try {
                    client.messages().createStreaming(params()).use { response ->
                        // Leaving the demo cancels the turn; closing the stream stops the request
                        // (and the bill) instead of waiting for the next event to notice.
                        val stop =
                            currentCoroutineContext()[Job]?.invokeOnCompletion {
                                response.close()
                            }
                        try {
                            for (event in response.stream().iterator()) {
                                accumulator.accumulate(event)
                                val delta =
                                    event
                                        .contentBlockDelta()
                                        .getOrNull()
                                        ?.delta()
                                        ?.text()
                                        ?.getOrNull()
                                if (delta != null) {
                                    reply.append(delta.text())
                                    splitter.push(delta.text()).forEach { emit(it) }
                                }
                            }
                        } finally {
                            stop?.dispose()
                        }
                    }
                } catch (failure: Throwable) {
                    keepPartialReply(reply)
                    throw if (failure is CancellationException) failure else classify(failure)
                }
                splitter.flush().forEach { emit(it) }
                val message = accumulator.message()
                val refused = message.stopReason().getOrNull() == StopReason.REFUSAL
                // A refused or empty message must not go into the history as it is: an empty
                // assistant turn makes every later request invalid.
                if (refused || reply.isEmpty()) keepPartialReply(reply)
                else history += message.toParam()
                if (refused) emit(AgentChunk.Refused)
            }
            .flowOn(Dispatchers.IO)

    override fun close() = client.close()

    // Whatever already streamed is on screen, so the conversation has to say so too; with nothing
    // streamed the user turn is dropped, keeping the history a valid user/assistant alternation.
    private fun keepPartialReply(reply: StringBuilder) {
        if (reply.isEmpty()) {
            history.removeAt(history.lastIndex)
        } else {
            history += message(MessageParam.Role.ASSISTANT, reply.toString())
        }
    }

    private fun classify(failure: Throwable): AgentFailure =
        AgentFailure(
            when (failure) {
                is UnauthorizedException,
                is PermissionDeniedException -> AgentFailure.Reason.Auth
                is RateLimitException -> AgentFailure.Reason.RateLimited
                is BadRequestException,
                is NotFoundException -> AgentFailure.Reason.Unavailable
                else -> AgentFailure.Reason.Network
            },
            failure,
        )

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

    private fun message(role: MessageParam.Role, text: String) =
        MessageParam.builder().role(role).content(text).build()

    private companion object {
        const val MODEL = "claude-opus-5-5"
    }
}
