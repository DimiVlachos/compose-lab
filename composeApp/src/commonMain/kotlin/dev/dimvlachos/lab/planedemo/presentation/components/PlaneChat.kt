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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.text.TextLayoutResult
import dev.dimvlachos.lab.core.presentation.components.paperplane.LetterStream
import dev.dimvlachos.lab.core.presentation.components.paperplane.PaperPlaneState
import dev.dimvlachos.lab.core.presentation.components.paperplane.planeTakeoff
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@Immutable internal class ChatMessage(val id: Long, val text: String, val mine: Boolean)

/** A kept place's text, as laid out there, and where its top left lies in the root. */
@Immutable internal data class Slot(val text: TextLayoutResult, val at: Offset)

// After a plane leaves, the button's next one is folded in this long.
private const val IconBackMs = 260L

// How long a send waits for its place to be laid out before it scrolls straight to it, and how
// long after that before it gives up on the flight and lets the message simply appear.
private const val SlotWaitMs = 600L

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

    /**
     * The text being typed, as last laid out; where its top left lies in the root, scrolled as the
     * field has scrolled it; and the part of the field it shows, in the root.
     */
    var draftLayout: () -> TextLayoutResult? = { null }
    var draftAt: () -> Offset = { Offset.Zero }
    var draftShown: Rect = Rect.Zero

    /**
     * How tall the field is, and how tall it is held while a sent message's letters are still going
     * into the button: cleared, it would shrink under them and the conversation drop into where
     * they fly from.
     */
    var fieldHeight = 0
    var heldHeight by mutableIntStateOf(0)
        private set

    /** Where the send button lies in the root, as laid out, before it grows. */
    var button: Rect by mutableStateOf(Rect.Zero)

    /** The field the button sits in, its outline in the root: the button grows no further. */
    var field: RoundRect by mutableStateOf(RoundRect.Zero)

    /** Letters gone into the button since its last plane left: it grows with each. */
    var swallowed by mutableIntStateOf(0)
        private set

    /** Sends whose letters are still going into the button: it stays while there are any. */
    var pouring by mutableIntStateOf(0)
        private set

    /** Whether the button's plane has just left, and the next is not folded yet. */
    var iconAway by mutableIntStateOf(0)
        private set

    private var nextId = 0L

    /** How many messages the conversation began with: the ones after them were sent. */
    val seeded = seed.size

    init {
        seed.forEach { (text, mine) -> messages += ChatMessage(nextId++, text, mine) }
    }

    /**
     * Takes back every message sent since the conversation began, once those still on their way
     * have arrived: taken back in the air, a message would land in a conversation without it.
     */
    suspend fun clear() {
        snapshotFlow { flying.isEmpty() }.first { it }
        messages.removeAll { it.id >= seeded }
    }

    /**
     * Sends what is typed: its letters go into the button's dart, at [icon] within the button,
     * which [grown] says how much bigger it has grown with them; its place is kept at the foot of
     * the conversation, and the dart, as big as it has grown, lifts off as the plane that flies
     * them there and drops them into it. Nothing for a blank message. Returns once the last letter
     * lies in its place; a send stopped on its way takes its message back with it.
     */
    suspend fun send(
        text: String,
        icon: (Rect) -> Rect,
        letters: LetterStream,
        plane: PaperPlaneState,
        list: LazyListState,
        cleared: () -> Unit,
        grown: () -> Float,
    ) {
        val line = text.trim()
        val layout = draftLayout()
        if (line.isEmpty() || layout == null) return
        val message = ChatMessage(nextId++, line, mine = true)
        val from = draftAt()
        val shown = draftShown
        pouring++
        heldHeight = fieldHeight
        cleared()
        var arrivedThere = false
        try {
            coroutineScope {
                // Into the button wherever it is as each letter flies: the field shrinks as it is
                // cleared, and the button with it.
                val pour = launch {
                    try {
                        letters.pour(layout, from, { icon(button).center }, { swallowed++ }, shown)
                    } finally {
                        pouring--
                        if (pouring == 0) heldHeight = 0
                    }
                }
                // Its place is kept a frame after the tap, not in it: that frame has the field to
                // clear and the letters to lift, and the place opens no sooner than the plane
                // comes round to it.
                withFrameNanos {}
                messages += message
                flying += message.id
                // Scrolled up the conversation, it comes back down to see the plane land.
                launch { list.animateScrollToItem(0) }
                pour.join()
                val takeoff = planeTakeoff(icon(button).grownBy(grown()))
                swallowed = 0
                iconAway++
                launch {
                    try {
                        delay(IconBackMs)
                    } finally {
                        iconAway--
                    }
                }
                val slot = placeOf(message.id, list)
                if (slot != null) {
                    plane.launch(message.id, takeoff, slot.text) {
                        slots[message.id]?.at ?: slot.at
                    }
                }
                arrivedThere = true
            }
        } finally {
            if (!arrivedThere) messages -= message
            flying -= message.id
            slots -= message.id
        }
    }

    // The message's place once it is laid out. The way down to it can be cut short, by a hand on
    // the list: then straight there. Never laid out, the message goes without a plane.
    private suspend fun placeOf(id: Long, list: LazyListState): Slot? {
        suspend fun laidOut() =
            withTimeoutOrNull(SlotWaitMs) { snapshotFlow { slots[id] }.filterNotNull().first() }
        return laidOut()
            ?: run {
                list.requestScrollToItem(0)
                laidOut()
            }
    }
}

// The same box, [scale] times the size about its middle.
private fun Rect.grownBy(scale: Float) =
    Rect(
        center - Offset(width, height) * (scale / 2f),
        center + Offset(width, height) * (scale / 2f),
    )
