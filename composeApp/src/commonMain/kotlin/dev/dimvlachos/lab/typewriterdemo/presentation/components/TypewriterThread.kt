package dev.dimvlachos.lab.typewriterdemo.presentation.components

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.dimvlachos.lab.core.presentation.components.typewriter.layOutSheet
import dev.dimvlachos.lab.core.presentation.components.typewriter.slipTilt
import kotlinx.coroutines.delay

/** A message in the thread: a paper slip of [lines] sent from the typewriter, or a reply. */
@Immutable
internal sealed interface ThreadMessage {
    val id: Long
    val text: String

    /** A sheet sent from the typewriter, trimmed to its [lines] and lying [tilt] degrees turned. */
    @Immutable
    class Slip(
        override val id: Long,
        override val text: String,
        val lines: List<String>,
        val tilt: Float,
    ) : ThreadMessage

    /** A message from the other side, in a plain bubble. */
    @Immutable class Reply(override val id: Long, override val text: String) : ThreadMessage
}

// The other side starts typing this long after a slip lands, and its reply comes this long after.
private const val ReplyWaitMs = 700L
private const val ReplyTypingMs = 1_500L

/**
 * A chat with one person, whose messages from this side are typewritten slips: it starts with
 * [seed] (each a reply, or a slip if it's ours), and each slip sent gets the next of [replies], in
 * turn, after a moment of the other side typing.
 */
@Stable
internal class TypewriterThread(
    seed: List<Pair<String, Boolean>>,
    private val replies: List<String>,
) {
    val messages = mutableStateListOf<ThreadMessage>()

    /** Whether the other side is typing a reply. */
    var typing by mutableStateOf(false)
        private set

    /**
     * The id the reply being typed will have: the bubble that shows the typing becomes the reply,
     * in the same place.
     */
    var typingId by mutableStateOf(-1L)
        private set

    /**
     * Hears that something is about to come in at the foot of the thread, just before it does, in
     * the same frame: a list showing the foot can make room for it there.
     */
    var onArrival: (() -> Unit)? = null

    private var nextId = 0L
    private var replied = 0

    /** How many messages the thread began with: the ones after them came since. */
    private val seeded = seed.size

    init {
        for ((text, ours) in seed) messages += if (ours) slip(text) else reply(text)
    }

    /** Whether message [id] came since the thread began, rather than being there from the start. */
    fun isNew(id: Long): Boolean = id >= seeded

    /** Keeps the next message's id, so its sheet can take off for it before it is in the thread. */
    fun nextId(): Long = nextId

    /** Adds a slip of [lines], [text] sent from the typewriter, to the foot of the thread. */
    fun send(text: String, lines: List<String>): ThreadMessage.Slip {
        val id = nextId++
        onArrival?.invoke()
        return ThreadMessage.Slip(id, text, lines, slipTilt(id)).also { messages += it }
    }

    /** The other side types for a moment, then replies with the next of its replies. */
    suspend fun answer() {
        if (replies.isEmpty()) return
        try {
            delay(ReplyWaitMs)
            typingId = nextId++
            onArrival?.invoke()
            typing = true
            delay(ReplyTypingMs)
            messages += ThreadMessage.Reply(typingId, replies[replied++ % replies.size])
        } finally {
            typing = false
        }
    }

    /** Takes everything since the thread began back out, so it is as it started. */
    fun clear() {
        messages.removeAll { it.id >= seeded }
        typing = false
        replied = 0
        nextId = seeded.toLong()
    }

    private fun slip(text: String): ThreadMessage.Slip {
        val id = nextId++
        return ThreadMessage.Slip(id, text, layOutSheet(text).sent, slipTilt(id))
    }

    private fun reply(text: String) = ThreadMessage.Reply(nextId++, text)
}
