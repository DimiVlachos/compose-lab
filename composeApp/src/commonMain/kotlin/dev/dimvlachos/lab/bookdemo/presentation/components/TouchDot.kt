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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import dev.dimvlachos.lab.core.demo.PageDrag
import dev.dimvlachos.lab.core.presentation.components.pageturn.PageTurnState
import kotlin.math.sign
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

// Where the scripted finger lands, as fractions of the book: on the outer part of a page, a little
// below the middle, where a thumb would take it.
private const val TouchY = 0.62f
private const val RightPageX = 0.86f
private const val LeftPageX = 0.14f
private const val FillAlpha = 0.28f
private const val RingAlpha = 0.7f
private const val DownMs = 90
private const val UpMs = 260
private const val TapHoldMs = 110L
private val TouchRadius = 18.dp
private val RingWidth = 1.5.dp

/**
 * A fingertip drawn over the book while the script touches it, so a clip shows what a hand did: a
 * tap, a slow drag, a flick. Nothing but the script moves it.
 */
@Stable
internal class TouchDot {
    private var x by mutableFloatStateOf(RightPageX)
    private val alpha = Animatable(0f)

    suspend fun tap(forward: Boolean) {
        x = if (forward) RightPageX else LeftPageX
        try {
            alpha.animateTo(1f, tween(DownMs))
            delay(TapHoldMs)
            alpha.animateTo(0f, tween(UpMs))
        } finally {
            lift()
        }
    }

    /**
     * Plays [drag] on [book] as a finger would: down on the page it pulls, through each move, and
     * off at the drag's release speed. The book sees the same calls a real finger makes.
     */
    suspend fun drag(book: PageTurnState, drag: PageDrag) {
        val moves = drag.moves
        if (moves.isEmpty() || book.bookWidthPx <= 0f) return
        val leftwards = moves.first().toFraction < 0f
        val startX = if (leftwards) RightPageX else LeftPageX
        x = startX
        var dragging = false
        // A script stopped mid-drag still lets go of the page, or the book stays held.
        try {
            alpha.animateTo(1f, tween(DownMs))
            dragging = book.dragStart(dx = sign(moves.first().toFraction))
            if (!dragging) return
            var at = 0f
            moves.forEachIndexed { index, move ->
                val from = at
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
                    val next = lerp(from, move.toFraction, fraction)
                    book.dragBy((next - at) * book.bookWidthPx)
                    at = next
                    x = startX + at
                }
            }
            book.dragEnd(drag.releaseSpeed * book.bookWidthPx)
            dragging = false
            alpha.animateTo(0f, tween(UpMs))
        } finally {
            if (dragging) book.dragEnd(velocityPxPerSecond = 0f)
            lift()
        }
    }

    // Gone at once when a script is stopped; a no-op after a finished fade.
    private suspend fun lift() {
        withContext(NonCancellable) { alpha.snapTo(0f) }
    }

    fun Modifier.drawTouch(color: Color): Modifier = drawWithContent {
        drawContent()
        val a = alpha.value
        if (a <= 0f) return@drawWithContent
        val center = Offset(size.width * x, size.height * TouchY)
        drawCircle(color.copy(alpha = FillAlpha * a), TouchRadius.toPx(), center)
        drawCircle(
            color.copy(alpha = RingAlpha * a),
            TouchRadius.toPx(),
            center,
            style = Stroke(RingWidth.toPx()),
        )
    }
}
