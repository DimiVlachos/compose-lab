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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.toSize
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import kotlin.math.abs
import kotlin.math.max

/**
 * An open book of [spreads], each one image across both pages or blank paper (null), turned by
 * [state]. Tap the right page for the next spread and the left for the previous one, or take a page
 * anywhere and drag it over: the paper stays under the finger, a corner peels first, and the leaf
 * bows and sways with how fast it moves, finishing or falling back when let go.
 */
@Composable
public fun PageTurnBook(
    spreads: List<ImageBitmap?>,
    state: PageTurnState,
    modifier: Modifier = Modifier,
) {
    require(spreads.size == state.spreadCount) {
        "${spreads.size} spreads for a book of ${state.spreadCount}"
    }
    val colors = LabTheme.colors
    val density = LocalDensity.current
    val painter =
        remember(colors) {
            BookPainter(
                BookInk(
                    shade = colors.pageShade,
                    glare = colors.pageGlare,
                    gutter = colors.bookGutter,
                    thread = colors.thread,
                    threadTwist = colors.threadTwist,
                    crease = colors.bookCrease,
                    pageEdge = colors.pageEdge,
                    pageEdgeLine = colors.pageEdgeLine,
                )
            )
        }
    Spacer(
        modifier
            .aspectRatio(BookAspect)
            .onSizeChanged { state.layout = bookLayout(it.toSize(), density) }
            .pageTurnGestures(state)
            .drawWithCache { onDrawBehind { with(painter) { drawBook(spreads, state) } } }
    )
}

/**
 * A tap turns a page; a drag past the slop that sets off sideways, straight or on a slant, takes
 * the paper under the finger and hands every move to [state], up and down too, then its speed on
 * release. A drag that starts nearly straight up or down is left alone.
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
            drag(down.id) { change ->
                tracker.addPosition(change.uptimeMillis, change.position)
                val offset = change.position - down.position
                travelled = max(travelled, offset.getDistance())
                if (!dragging && !vertical && travelled >= slop) {
                    if (abs(offset.y) > abs(offset.x) * PageTurnDimens.SteepestDrag) {
                        vertical = true
                    } else {
                        dragging = state.dragStart(down.position, change.position)
                    }
                }
                if (dragging) {
                    change.consume()
                    state.dragTo(change.position)
                }
            }
            when {
                dragging -> tracker.calculateVelocity().let { state.dragEnd(Offset(it.x, it.y)) }
                travelled < slop ->
                    if (down.position.x >= size.width / 2f) state.next() else state.previous()
            }
        }
    }
