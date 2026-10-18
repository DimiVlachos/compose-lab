package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.unit.Velocity
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.ShelfLayout
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.TiltGesture
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.RectF
import dev.dimvlachos.lab.core.presentation.components.bookshelf.pageturn.PageTurnDimens
import dev.dimvlachos.lab.core.presentation.components.bookshelf.pageturn.PageTurnState
import kotlin.math.abs

private const val SPINE_SLOP_DP = 2f
private const val TOP_SLOP_DP = 6f
private const val PAGE_EDGE_SLOP_DP = 6f
private const val TURN_EDGE_FRACTION = 0.15f
private const val FLING_HOLD_MS = 100L

private data class SpineTouch(val bookId: Int, val grabHeight: Float, val canTip: Boolean)

internal suspend fun AwaitPointerEventScope.handleShelfGesture(
    layout: ShelfLayout,
    state: ShelfState,
    density: Float,
) {
    val down = awaitFirstDown(requireUnconsumed = false)
    if (state.mode != ShelfMode.Shelf && state.mode != ShelfMode.Settle) return
    val start = down.position / density
    val touch = spineAt(layout, start) ?: return
    while (true) {
        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: return
        if (change.isConsumed) return
        if (!change.pressed) {
            state.nudge(touch.bookId)
            return
        }
        val delta = change.position - down.position
        if (delta.getDistance() < viewConfiguration.touchSlop) continue
        if (!touch.canTip || delta.y <= abs(delta.x)) return
        change.consume()
        tipBook(down, touch.bookId, touch.grabHeight, change.position.y / density, state, density)
        return
    }
}

internal suspend fun AwaitPointerEventScope.handlePageGesture(
    page: RectF?,
    state: ShelfState,
    turns: PageTurnState?,
    density: Float,
    facing: Boolean,
) {
    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
    if (state.mode != ShelfMode.Open || page == null) {
        down.consume()
        return
    }
    if (state.listening) return
    val start = down.position / density
    if (start.y < page.top - PAGE_EDGE_SLOP_DP || start.y > page.bottom + PAGE_EDGE_SLOP_DP) return
    val zone = turns?.let { turnZone(start, page, facing) }
    val inner = if (facing) page.left - page.width() else page.left
    if (start.x < inner - PAGE_EDGE_SLOP_DP && zone == null) return
    when (val claim = awaitClaim(down)) {
        null -> Unit
        is PageClaim.Tap -> {
            if (zone != null && turns != null) {
                claim.up.consume()
                turns.tap(forward = zone)
            }
        }
        is PageClaim.Drag -> {
            val delta = claim.change.position - down.position
            val sideways = abs(delta.y) <= abs(delta.x) * PageTurnDimens.SteepestDrag
            if (
                turns != null && sideways && turns.dragStart(down.position, claim.change.position)
            ) {
                turnDrag(down, claim.change, turns)
                return
            }
            // Not a page turn. Down, the book goes down with the finger and, let go far or fast
            // enough, back to its shelf; right from the first page, its cover closes with the
            // finger, the same.
            val atFirstPage = (turns?.page ?: 0) == 0
            when {
                delta.y > 0f && abs(delta.y) > abs(delta.x) -> {
                    val velocity =
                        followDrag(down, claim.change) { at ->
                            state.dragPush((at.y - down.position.y) / density)
                        }
                    state.releasePush(velocity.y / density)
                }
                atFirstPage && delta.x > 0f && abs(delta.x) >= abs(delta.y) -> {
                    val across = page.width() * density
                    val velocity =
                        followDrag(down, claim.change) { at ->
                            state.dragCover((at.x - down.position.x) / across)
                        }
                    state.releaseCover(velocity.x / across)
                }
            }
        }
    }
}

// Follows a drag to its end, handing each position to [onMove]; returns how fast it was let go.
private suspend fun AwaitPointerEventScope.followDrag(
    down: PointerInputChange,
    claimed: PointerInputChange,
    onMove: (Offset) -> Unit,
): Offset {
    val tracker = VelocityTracker().apply { addPosition(down.uptimeMillis, down.position) }
    tracker.addPointerInputChange(claimed)
    onMove(claimed.position)
    while (true) {
        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
        if (!change.pressed) break
        change.consume()
        tracker.addPointerInputChange(change)
        onMove(change.position)
    }
    val v = tracker.calculateVelocity()
    return Offset(v.x, v.y)
}

private sealed interface PageClaim {
    class Drag(val change: PointerInputChange) : PageClaim

    class Tap(val up: PointerInputChange) : PageClaim
}

private suspend fun AwaitPointerEventScope.awaitClaim(down: PointerInputChange): PageClaim? {
    val longPressAt = down.uptimeMillis + viewConfiguration.longPressTimeoutMillis
    while (true) {
        val change =
            awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id }
                ?: return null
        if (change.isConsumed || change.uptimeMillis >= longPressAt) return null
        if (!change.pressed) return PageClaim.Tap(change)
        if ((change.position - down.position).getDistance() >= viewConfiguration.touchSlop) {
            change.consume()
            return PageClaim.Drag(change)
        }
    }
}

private suspend fun AwaitPointerEventScope.turnDrag(
    down: PointerInputChange,
    claim: PointerInputChange,
    turns: PageTurnState,
) {
    val tracker = VelocityTracker().apply { addPosition(down.uptimeMillis, down.position) }
    tracker.addPointerInputChange(claim)
    var moved = claim.uptimeMillis
    var velocity = tracker.calculateVelocity()
    try {
        var pressed = true
        while (pressed) {
            val change =
                awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull {
                    it.id == claim.id
                } ?: break
            change.consume()
            pressed = change.pressed
            if (pressed) {
                tracker.addPointerInputChange(change)
                velocity = tracker.calculateVelocity()
                moved = change.uptimeMillis
                turns.dragTo(change.position)
            } else if (change.uptimeMillis - moved > FLING_HOLD_MS) {
                velocity = Velocity.Zero
            }
        }
    } finally {
        val decay = splineBasedDecay<Float>(this)
        turns.dragEnd(
            Offset(velocity.x, velocity.y),
            Offset(decay.calculateTargetValue(0f, velocity.x), 0f),
        )
    }
}

private fun turnZone(point: Offset, page: RectF, facing: Boolean): Boolean? {
    if (point.y < page.top || point.y > page.bottom) return null
    val edge = page.width() * TURN_EDGE_FRACTION
    val outerLeft = if (facing) page.left - page.width() else page.left
    return when {
        point.x > page.right - edge -> true
        point.x < outerLeft + edge -> false
        else -> null
    }
}

internal suspend fun AwaitPointerEventScope.tipBook(
    down: PointerInputChange,
    bookId: Int,
    grabHeight: Float,
    startY: Float,
    state: ShelfState,
    density: Float,
) {
    state.beginTip(bookId)
    var velocity = 0f
    try {
        velocity =
            (track(down) { change ->
                state.dragTip(change.position.y / density - startY, grabHeight)
            } ?: 0f) / density
    } finally {
        state.releaseTip(velocity)
    }
}

private suspend fun AwaitPointerEventScope.track(
    down: PointerInputChange,
    onMove: (PointerInputChange) -> Unit,
): Float? {
    val tracker = VelocityTracker()
    tracker.addPointerInputChange(down)
    while (true) {
        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: return 0f
        tracker.addPointerInputChange(change)
        val handledBelow = change.isConsumed
        change.consume()
        if (!change.pressed) return if (handledBelow) null else tracker.calculateVelocity().y
        if (!handledBelow) onMove(change)
    }
}

private fun spineAt(layout: ShelfLayout, point: Offset): SpineTouch? {
    for (plank in layout.planks) {
        val slot =
            plank.slots.firstOrNull {
                point.x >= it.x - SPINE_SLOP_DP &&
                    point.x <= it.x + it.thickness + SPINE_SLOP_DP &&
                    point.y >= plank.plankY - it.height - TOP_SLOP_DP &&
                    point.y <= plank.plankY
            } ?: continue
        val grab = plank.plankY - point.y
        return SpineTouch(slot.bookId, grab, TiltGesture.canTip(grab, slot.height))
    }
    return null
}
