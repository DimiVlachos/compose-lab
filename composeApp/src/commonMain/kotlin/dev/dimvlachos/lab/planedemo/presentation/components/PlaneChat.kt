package dev.dimvlachos.lab.planedemo.presentation.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextLayoutResult
import dev.dimvlachos.lab.core.presentation.components.paperplane.LetterStream
import dev.dimvlachos.lab.core.presentation.components.paperplane.PaperPlaneState
import dev.dimvlachos.lab.core.presentation.components.paperplane.planeTakeoff
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Immutable internal class ChatMessage(val id: Long, val text: String, val mine: Boolean)

/** A kept place's text, as laid out there, and where its top left lies in the root. */
@Immutable internal class Slot(val text: TextLayoutResult, val at: Offset)

// After a plane leaves, the button's next one is folded in this long.
private const val IconBackMs = 260L

/**
 * The conversation, newest last, and the messages on their way into it: each has its place kept
 * empty until its plane has dropped its letters there.
 */
@Stable
internal class PlaneChat(seed: List<Pair<String, Boolean>>) {
    val messages = mutableStateListOf<ChatMessage>()

    /** The messages in the air, by id: their places are kept, but empty. */
    val flying = mutableStateListOf<Long>()

    /** Each kept place's text and where it lies, once laid out. */
    val slots = mutableStateMapOf<Long, Slot>()

    /** The text being typed, as last laid out, and where its top left lies in the root. */
    var draftLayout: () -> TextLayoutResult? = { null }
    var draftAt: Offset by mutableStateOf(Offset.Zero)

    /** Where the button's paper plane lies, in the root. */
    var icon: Rect by mutableStateOf(Rect.Zero)

    /** Sends whose letters are still going into the button: it stays while there are any. */
    var pouring by mutableIntStateOf(0)
        private set

    /** Whether the button's plane has just left, and the next is not folded yet. */
    var iconAway by mutableIntStateOf(0)
        private set

    private var nextId = 0L
    private val seeded = seed.size

    /** Takes back every message sent since the conversation began. */
    fun clear() {
        messages.removeAll { it.id >= seeded && it.id !in flying }
    }

    init {
        seed.forEach { (text, mine) -> messages += ChatMessage(nextId++, text, mine) }
    }

    /**
     * Sends what is typed: its place is kept at the foot of the conversation, its letters go into
     * the button, [arrived] for each, and the button's plane flies them there and drops them into
     * it. Nothing for a blank message. Returns once the last letter lies in its place.
     */
    suspend fun send(
        text: String,
        letters: LetterStream,
        plane: PaperPlaneState,
        list: LazyListState,
        cleared: () -> Unit,
        arrived: () -> Unit,
    ) {
        val line = text.trim()
        val layout = draftLayout()
        if (line.isEmpty() || layout == null) return
        val message = ChatMessage(nextId++, line, mine = true)
        messages += message
        flying += message.id
        val from = draftAt
        pouring++
        cleared()
        try {
            coroutineScope {
                // Scrolled up the conversation, it comes back down to see the plane land.
                launch { list.animateScrollToItem(0) }
                try {
                    letters.pour(layout, from, icon.center, arrived)
                } finally {
                    pouring--
                }
                val takeoff = planeTakeoff(icon)
                iconAway++
                launch {
                    delay(IconBackMs)
                    iconAway--
                }
                val slot = snapshotFlow { slots[message.id] }.filterNotNull().first()
                plane.launch(message.id, takeoff, slot.text) {
                    slots[message.id]?.at ?: slot.at
                }
            }
        } finally {
            flying -= message.id
            slots -= message.id
        }
    }
}
