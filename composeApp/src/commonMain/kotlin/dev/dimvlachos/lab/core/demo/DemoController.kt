package dev.dimvlachos.lab.core.demo

import androidx.compose.ui.geometry.Offset
import kotlin.time.Duration
import org.jetbrains.compose.resources.StringResource

interface DemoController {
    val selectedIndex: Int

    fun select(index: Int)

    suspend fun scrollBy(px: Float)

    suspend fun dragPage(drag: PageDrag)

    /**
     * Drags a finger through [path] over [duration]: points as fractions of the stage, sampled at
     * even time steps, so their spacing sets the finger's speed.
     */
    suspend fun wipe(path: List<Offset>, duration: Duration)

    /**
     * Starts a drop of condensation at [at], a fraction of the stage, to run [length] of its
     * height. Returns at once; the drop runs on its own.
     */
    suspend fun drip(at: Offset, length: Float)

    /** Mists the stage evenly back over in [duration], as a steamy room does on its own. */
    suspend fun mist(duration: Duration)

    /**
     * Types [text] into the message field over [typing], a character at a time, and taps send.
     * Returns once the message has landed in the conversation.
     */
    suspend fun sendMessage(text: StringResource, typing: Duration)

    /** Takes the messages sent so far back out of the conversation, leaving it as it began. */
    suspend fun clearMessages()
}
