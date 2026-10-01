package dev.dimvlachos.lab.core.presentation.components.pageturn

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import kotlin.math.abs
import kotlin.math.max

/**
 * An open book of [spreads], each one image across both pages or blank paper (null), turned by
 * [state]. Tap the right page for the next spread and the left for the previous one, or drag a page
 * over: it follows the finger, bowing the way it moves, and finishes or falls back when let go.
 */
@Composable
fun PageTurnBook(
    spreads: List<ImageBitmap?>,
    state: PageTurnState,
    modifier: Modifier = Modifier,
) {
    require(spreads.size == state.spreadCount) {
        "${spreads.size} spreads for a book of ${state.spreadCount}"
    }
    val colors = LabTheme.colors
    val painter =
        remember(colors) {
            BookPainter(
                BookInk(
                    shade = colors.pageShade,
                    glare = colors.pageGlare,
                    gutter = colors.bookGutter,
                    staple = colors.staple,
                    stapleLit = colors.stapleLit,
                    crease = colors.bookCrease,
                    pageEdge = colors.pageEdge,
                    pageEdgeLine = colors.pageEdgeLine,
                )
            )
        }
    Spacer(
        modifier
            .aspectRatio(BookAspect)
            .onSizeChanged { state.bookWidthPx = it.width.toFloat() }
            .pageTurnGestures(state)
            .drawWithCache { onDrawBehind { with(painter) { drawBook(spreads, state) } } }
    )
}

/**
 * A tap turns a page; a horizontal drag past the slop takes the page and hands every move to
 * [state], then its speed on release. A drag that starts mostly vertical is left alone.
 */
private fun Modifier.pageTurnGestures(state: PageTurnState): Modifier =
    pointerInput(state) {
        awaitEachGesture {
            val down = awaitFirstDown()
            down.consume()
            val tracker = VelocityTracker()
            tracker.addPosition(down.uptimeMillis, down.position)
            val slop = PageTurnDimens.TapSlop.toPx()
            var travelled = 0f
            var dragging = false
            var vertical = false
            var lastX = down.position.x
            drag(down.id) { change ->
                tracker.addPosition(change.uptimeMillis, change.position)
                val offset = change.position - down.position
                travelled = max(travelled, offset.getDistance())
                if (!dragging && !vertical && travelled >= slop) {
                    if (abs(offset.x) < abs(offset.y)) {
                        vertical = true
                    } else {
                        dragging = state.dragStart(offset.x)
                        lastX = change.position.x
                    }
                }
                if (dragging) {
                    change.consume()
                    state.dragBy(change.position.x - lastX)
                    lastX = change.position.x
                }
            }
            when {
                dragging -> state.dragEnd(tracker.calculateVelocity().x)
                travelled < slop ->
                    if (down.position.x >= size.width / 2f) state.next() else state.previous()
            }
        }
    }
