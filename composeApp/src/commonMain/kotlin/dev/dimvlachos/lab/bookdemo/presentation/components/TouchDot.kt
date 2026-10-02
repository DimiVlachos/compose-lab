package dev.dimvlachos.lab.bookdemo.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.util.lerp
import dev.dimvlachos.lab.core.demo.PageDrag
import dev.dimvlachos.lab.core.presentation.components.pageturn.PageTurnState
import dev.dimvlachos.lab.core.presentation.components.touch.ScriptedTouch
import dev.dimvlachos.lab.core.presentation.components.touch.drawTouch
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

// Where the scripted finger lands, as fractions of the book: on the outer part of a page, a little
// below the middle, where a thumb would take it.
private const val TouchY = 0.62f
private const val RightPageX = 0.86f
private const val LeftPageX = 0.14f

/**
 * A fingertip drawn over the book while the script touches it, so a clip shows what a hand did: a
 * tap, a slow drag, a flick. Nothing but the script moves it.
 */
@Stable
internal class TouchDot {
    private var x by mutableFloatStateOf(RightPageX)
    private var y by mutableFloatStateOf(TouchY)
    private val alpha = Animatable(0f)

    // Counts the touches: a finger lifting after the next one has come down leaves the dot alone.
    private var touches = 0

    suspend fun tap(forward: Boolean) {
        val touch = ++touches
        x = if (forward) RightPageX else LeftPageX
        y = TouchY
        try {
            alpha.animateTo(1f, tween(ScriptedTouch.DownMs))
            delay(ScriptedTouch.TapHoldMs)
            alpha.animateTo(0f, tween(ScriptedTouch.UpMs))
        } finally {
            lift(touch)
        }
    }

    /**
     * Plays [drag] on [book] as a finger would: down on the page it pulls, through each move, and
     * off at the drag's release speed. The book sees the same calls a real finger makes.
     */
    suspend fun drag(book: PageTurnState, drag: PageDrag) {
        val touch = ++touches
        val moves = drag.moves
        val box = book.layout?.size ?: return
        if (moves.isEmpty()) return
        val leftwards = moves.first().toFraction < 0f
        val startX = drag.x ?: if (leftwards) RightPageX else LeftPageX
        val startY = drag.y ?: TouchY
        x = startX
        y = startY
        val down = Offset(startX * box.width, startY * box.height)
        var dragging = false
        // A script stopped mid-drag still lets go of the page, or the book stays held.
        try {
            alpha.animateTo(1f, tween(ScriptedTouch.DownMs))
            dragging = book.dragStart(down, down + Offset(if (leftwards) -1f else 1f, 0f))
            if (!dragging) return
            var atX = 0f
            var atY = 0f
            moves.forEachIndexed { index, move ->
                val fromX = atX
                val fromY = atY
                // A move that ends in a flick keeps its speed to the end; any other one eases.
                val flicked = index == moves.lastIndex && drag.releaseSpeed != 0f
                animate(
                    0f,
                    1f,
                    animationSpec =
                        tween(
                            move.duration.inWholeMilliseconds.toInt(),
                            easing = if (flicked) LinearEasing else FastOutSlowInEasing,
                        ),
                ) { fraction, _ ->
                    atX = lerp(fromX, move.toFraction, fraction)
                    atY = lerp(fromY, move.toFractionY, fraction)
                    x = startX + atX
                    y = startY + atY
                    book.dragTo(Offset(x * box.width, y * box.height))
                }
            }
            book.dragEnd(Offset(drag.releaseSpeed * box.width, 0f))
            dragging = false
            alpha.animateTo(0f, tween(ScriptedTouch.UpMs))
        } finally {
            if (dragging) book.dragEnd(Offset.Zero)
            lift(touch)
        }
    }

    // Gone at once when a script is stopped; a no-op after a finished fade. Only the latest touch
    // takes the dot away: an earlier one's fade, cut short by the next touch, leaves it be.
    private suspend fun lift(touch: Int) {
        if (touch != touches) return
        withContext(NonCancellable) { alpha.snapTo(0f) }
    }

    fun Modifier.drawTouch(color: Color): Modifier = drawWithContent {
        drawContent()
        drawTouch(color, Offset(size.width * x, size.height * y), alpha.value)
    }
}
