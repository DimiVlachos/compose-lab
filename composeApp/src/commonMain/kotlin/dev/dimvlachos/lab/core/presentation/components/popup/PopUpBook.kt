package dev.dimvlachos.lab.core.presentation.components.popup

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import kotlinx.coroutines.flow.first

/**
 * A pop-up book lying open on the table: open a spread and its [PopUpPiece]s stand up out of the
 * gutter in perspective, folding flat again as it shuts. Drag a leaf over, or tap the near half to
 * turn forward and the far half to turn back; pull a spread's tab to move its pieces.
 *
 * The [cover] is the book's outside; there is one [PopUpSpread] per opening, as many as [state] was
 * made for. The book is decoration to a screen reader, named by [description]; give the tour its
 * own buttons and text alongside.
 */
@Composable
public fun PopUpBook(
    cover: Painter,
    spreads: List<PopUpSpread>,
    state: PopUpBookState,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    require(spreads.size == state.spreadCount) {
        "PopUpBook has ${spreads.size} spreads but its state was made for ${state.spreadCount}"
    }
    val density = LocalDensity.current
    val colors = LabTheme.colors
    val painter = remember { PopUpPainter() }

    LaunchedEffect(state, spreads) {
        state.restless =
            BooleanArray(spreads.size) { s ->
                spreads[s].pieces.any {
                    (it.motion as? PieceMotion.RidesTab)?.bob?.let { b -> b != 0f } == true
                }
            }
    }
    LaunchedEffect(state) {
        while (true) {
            snapshotFlow { state.awake }.first { it }
            var last = -1L
            while (state.awake) {
                withFrameNanos { now ->
                    if (last >= 0L) {
                        val seconds = (now - last) / 1e9f
                        state.advance(seconds.coerceIn(0f, PopUpDimens.MaxFrameSeconds))
                    }
                    last = now
                }
            }
        }
    }

    Spacer(
        // As wide as it may be, or as tall, whichever fits: on a short screen it never spills
        // over its neighbours.
        modifier
            .aspectRatio(1f / PopUpDimens.Aspect)
            .onSizeChanged { size ->
                val camera = BookCamera(size.width.toFloat(), size.height.toFloat())
                state.layout = layoutFor(camera, spreads, state, density.density)
            }
            .pointerInput(state) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val tab = state.tabStart(down.position)
                    if (!tab && !state.dragStart(down.position)) return@awaitEachGesture
                    down.consume()
                    val tracker = VelocityTracker()
                    tracker.addPosition(down.uptimeMillis, down.position)
                    var lifted = false
                    var lastUptime = down.uptimeMillis
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            tracker.addPosition(change.uptimeMillis, change.position)
                            lastUptime = change.uptimeMillis
                            if (!change.pressed) {
                                lifted = true
                                break
                            }
                            if (tab) state.tabTo(change.position) else state.dragTo(change.position)
                            change.consume()
                        }
                    } finally {
                        // A touch the system took away, or one held still past a long press,
                        // taps nothing: the leaf is only let go.
                        val heldFor = lastUptime - down.uptimeMillis
                        val tap = heldFor < viewConfiguration.longPressTimeoutMillis
                        when {
                            tab -> state.tabEnd()
                            lifted && (tap || state.isDragMoved) ->
                                state.dragEnd(tracker.calculateVelocity())
                            else -> state.dragCancel()
                        }
                    }
                }
            }
            .drawWithCache {
                val camera = BookCamera(size.width, size.height)
                val art = painter.prepare(cover, spreads, camera, this, colors.paperBack)
                onDrawBehind {
                    // Read so a moved frame redraws; the moving values themselves are read below.
                    state.frame
                    painter.draw(
                        this,
                        state,
                        spreads,
                        camera,
                        art,
                        colors.pageEdge,
                        colors.pageEdgeLine,
                    )
                }
            }
            .clearAndSetSemantics { description?.let { contentDescription = it } }
    )
}

// Where the book's gutter, its turning span and its tabs are on the screen.
private fun layoutFor(
    camera: BookCamera,
    spreads: List<PopUpSpread>,
    state: PopUpBookState,
    density: Float,
): BookLayout {
    val gutter = camera.project(Vec3(0f, 0f, 0f))
    val near = camera.project(Vec3(0f, -PopUpDimens.PageDepth, 0f))
    val far = camera.project(Vec3(0f, PopUpDimens.PageDepth, 0f))
    return BookLayout(
        camera = camera,
        gutterY = gutter.y,
        span = near.y - far.y,
        touchSlop = PopUpDimens.TapSlopDp * density,
        tabReach = PopUpDimens.TabReachDp * density,
        tabPoint = { s ->
            spreads.getOrNull(s)?.tab?.let {
                val angles = state.angles
                val a = PopUpMath.across(if (s + 1 < angles.size) angles[s + 1] else 0f)
                val d = PopUpDimens.TabFromGutter + PopUpDimens.TabWidth / 2f
                val x = PopUpDimens.TabX + state.tabTravel[s]
                camera.project(Vec3(x, a.y * d, a.z * d))
            }
        },
        tabTravel = { s -> spreads.getOrNull(s)?.tab?.travel ?: 0f },
    )
}
