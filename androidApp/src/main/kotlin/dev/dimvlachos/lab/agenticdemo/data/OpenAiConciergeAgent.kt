package dev.dimvlachos.lab.agenticdemo.data

import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.models.Reasoning
import com.openai.models.ReasoningEffort
import com.openai.models.responses.EasyInputMessage
import com.openai.models.responses.ResponseCreateParams
import com.openai.models.responses.ResponseInputItem
import java.io.IOException
import kotlin.jvm.optionals.getOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    private val lock = Mutex()

    override fun send(turn: String): Flow<AgentChunk> =
        flow {
                lock.withLock {
                    history += message(EasyInputMessage.Role.USER, turn)
                    val splitter = JsonlLineSplitter()
                    val reply = StringBuilder()
                    var refused = false
                    try {
                        client.responses().createStreaming(params()).use { response ->
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
                        }
                    } catch (failure: Throwable) {
                        history.removeAt(history.lastIndex)
                        throw failure
                    }
                    splitter.flush().forEach { emit(it) }
                    history += message(EasyInputMessage.Role.ASSISTANT, reply.toString())
                    if (refused) emit(AgentChunk.Refused)
                }
            }
            .flowOn(Dispatchers.IO)

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
