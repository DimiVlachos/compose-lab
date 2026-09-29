package dev.dimvlachos.lab.agenticdemo.data

import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.errors.BadRequestException
import com.openai.errors.NotFoundException
import com.openai.errors.PermissionDeniedException
import com.openai.errors.RateLimitException
import com.openai.errors.UnauthorizedException
import com.openai.models.Reasoning
import com.openai.models.ReasoningEffort
import com.openai.models.responses.EasyInputMessage
import com.openai.models.responses.ResponseCreateParams
import com.openai.models.responses.ResponseInputItem
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.jvm.optionals.getOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * An OpenAI model as the A2UI agent, over the Responses API. The same contract as
 * [ClaudeConciergeAgent]: stream the reply, cut it into lines as it arrives, keep the conversation
 * client-side (`store = false`) so each turn resends the history.
 */
class OpenAiConciergeAgent(apiKey: String, private val model: String, systemPrompt: () -> String) :
    ConciergeAgent {
    private val client: OpenAIClient = OpenAIOkHttpClient.builder().apiKey(apiKey).build()
    // Built on first use, off the main thread: it reads the schema assets and exports the catalog.
    private val instructions by lazy(systemPrompt)
    private val history = mutableListOf<ResponseInputItem>()

    override fun send(turn: String): Flow<AgentChunk> =
        flow {
                history += message(EasyInputMessage.Role.USER, turn)
                val splitter = JsonlLineSplitter()
                val reply = StringBuilder()
                var refused = false
                try {
                    client.responses().createStreaming(params()).use { response ->
                        // Leaving the demo cancels the turn; closing the stream stops the request
                        // (and the bill) instead of waiting for the next event to notice.
                        val stop =
                            currentCoroutineContext()[Job]?.invokeOnCompletion {
                                response.close()
                            }
                        try {
                            for (event in response.stream().iterator()) {
                                event.outputTextDelta().getOrNull()?.let { delta ->
                                    reply.append(delta.delta())
                                    splitter.push(delta.delta()).forEach { emit(it) }
                                }
                                if (event.refusalDelta().isPresent) refused = true
                                event.error().getOrNull()?.let { throw IOException(it.message()) }
                                event.failed().getOrNull()?.let { failed ->
                                    throw IOException(
                                        failed.response().error().getOrNull()?.message()
                                            ?: "Response failed"
                                    )
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
                keepPartialReply(reply)
                if (refused) emit(AgentChunk.Refused)
            }
            .flowOn(Dispatchers.IO)

    override fun close() = client.close()

    // Whatever streamed is on screen, so the conversation has to say so too; with nothing streamed
    // (a refusal, a failure before the first token) the user turn is dropped instead of being
    // answered by an empty assistant message.
    private fun keepPartialReply(reply: StringBuilder) {
        if (reply.isEmpty()) {
            history.removeAt(history.lastIndex)
        } else {
            history += message(EasyInputMessage.Role.ASSISTANT, reply.toString())
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

    private fun params(): ResponseCreateParams =
        ResponseCreateParams.builder()
            .model(model)
            .instructions(instructions)
            .inputOfResponse(history.toList())
            .reasoning(Reasoning.builder().effort(ReasoningEffort.LOW).build())
            // A reply is a few surfaces; the cap stops a runaway turn from billing thousands more.
            .maxOutputTokens(6_000L)
            .store(false)
            .build()

    private fun message(role: EasyInputMessage.Role, text: String) =
        ResponseInputItem.ofEasyInputMessage(
            EasyInputMessage.builder().role(role).content(text).build()
        )
}
