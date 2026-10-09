package dev.dimvlachos.lab.core.demo

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.time.Duration
import org.jetbrains.compose.resources.StringResource

internal interface DemoController {
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

    /**
     * Takes the lamp's bead with a fingertip, pulls it [down] and [across] (negative is up and
     * left) over [duration], and lets go. Pulled far enough down, the cord clicks and the lamp
     * switches. Returns once the finger has let go; the cord sways on by itself.
     */
    suspend fun pullCord(down: Dp, duration: Duration, across: Dp = 0.dp)

    /**
     * Takes the [tag] magnet with a fingertip wherever it is, the strip or the table, carries it
     * through [path] over [duration] (points as fractions of the stage, at even time steps) and
     * puts it down where the path ends. Returns once the finger has lifted.
     */
    suspend fun dragMagnet(tag: String, path: List<Offset>, duration: Duration)

    /**
     * Flicks the [tag] magnet back into its slot in the strip over [duration]: what is on it falls
     * off and slides home.
     */
    suspend fun releaseMagnet(tag: String, duration: Duration)

    /** Taps the [tag] magnet: its photos fan out into a grid, or, fanned out, fold back. */
    suspend fun tapMagnet(tag: String)

    /**
     * Taps the photo [id] in the fanned-out grid, which opens it, or, open, taps it again, which
     * closes it back into its cell.
     */
    suspend fun tapPhoto(id: String)

    /**
     * Pulls the list of a pull-to-refresh down with a fingertip near its top, [distance] over
     * [duration], holds it a moment and lets go, through the same nested scroll a finger's drag
     * sends from the top of a list. Pulled far enough, it asks for a refresh. Returns once the
     * finger has lifted; the refresh plays out by itself.
     */
    suspend fun pullToRefresh(distance: Dp, duration: Duration)
}
