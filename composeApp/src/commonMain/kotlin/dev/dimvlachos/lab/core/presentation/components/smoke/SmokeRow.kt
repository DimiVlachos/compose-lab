package dev.dimvlachos.lab.core.presentation.components.smoke

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * A row of a [SmokeOverlay]'s list, known by [key], that can be swiped away: to the left it goes as
 * [left] says, burning from its right edge, and to the right as [right] says, from its left edge; a
 * side with no action doesn't swipe. The row is cut at the front as it burns, along a ragged,
 * flickering edge, and once it is all smoke its height springs to nothing. With less than a third
 * burnt, let go, the smoke is drawn back in and the row is whole again.
 *
 * The row stands while it is [present] and closes up when it isn't, and opens again when it comes
 * back: as its [left] or [right] action runs, the caller takes it out, and an undo puts it back
 * (and calls [SmokeState.reform] to breathe its smoke back in).
 *
 * With a keyboard, Delete or Backspace on the focused row runs its [left] action, once however long
 * the key is held, focus moving on to the next row (or the one before, at the end); a screen reader
 * has both actions as custom actions, by their labels. A row that isn't [present] is hidden from
 * both.
 */
@Composable
public fun SmokeRow(
    state: SmokeState,
    key: Any,
    modifier: Modifier = Modifier,
    present: Boolean = true,
    left: SmokeAction? = null,
    right: SmokeAction? = null,
    content: @Composable () -> Unit,
) {
    ReportComposition()
    val row = remember(state, key) { SmokeRowState(key, present) }
    DisposableEffect(state, key, row) {
        state.rows[key] = row
        onDispose { if (state.rows[key] === row) state.rows.remove(key) }
    }
    val layer = rememberGraphicsLayer()
    SideEffect {
        row.left = left
        row.right = right
        row.layer = layer
        if (row.present != present) {
            row.present = present
            state.awake = true
        }
    }
    val currentLeft by rememberUpdatedState(left)
    val focusManager = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    val colors = LabTheme.colors
    val edge = remember { EdgePainter() }
    val ring = colors.accent
    val smokeEdge =
        remember(colors) {
            EdgeColors(colors.ember, colors.emberCore, colors.emberChar, colors.mist)
        }
    val actions =
        listOfNotNull(
            left?.let {
                CustomAccessibilityAction(it.label) { state.swipeAway(key, SmokeSide.Left) }
            },
            right?.let {
                CustomAccessibilityAction(it.label) { state.swipeAway(key, SmokeSide.Right) }
            },
        )
    Box(
        modifier
            // Clipped to the height it stands at, outside the layout that sets it, so a closing
            // row's content never spills over the rows under it.
            .clipToBounds()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val height = (placeable.height * row.height).roundToInt()
                layout(placeable.width, height) { placeable.place(0, 0) }
            }
            .onPlaced { row.coordinates = it }
            .then(
                if (present) {
                    Modifier.semantics(mergeDescendants = true) { customActions = actions }
                } else Modifier.clearAndSetSemantics {}
            )
            .onFocusChanged { focused = it.isFocused }
            .onKeyEvent { event ->
                if (!isDelete(event.key) || currentLeft == null) return@onKeyEvent false
                // A held key repeats its downs, on whichever row focus has moved on to: only the
                // first, until the key is let up anywhere in the overlay, burns.
                if (event.type != KeyEventType.KeyDown || state.deleteHeld) return@onKeyEvent true
                state.deleteHeld = true
                if (state.swipeAway(key, SmokeSide.Left)) {
                    // On to the next row, or the one before at the end of the list.
                    if (!focusManager.moveFocus(FocusDirection.Down)) {
                        focusManager.moveFocus(FocusDirection.Up)
                    }
                }
                true
            }
            .focusable(enabled = present)
            .pointerInput(state, key) { swipes(state, key) }
            .drawWithContent {
                row.tick
                layer.record { this@drawWithContent.drawContent() }
                val plume = state.plumeOf(row)
                if (plume == null) drawLayer(layer)
                else with(edge) { drawCut(plume, layer, state.now, smokeEdge) }
                if (focused) {
                    val width = SmokeDimens.FocusRing.toPx()
                    drawRect(
                        ring,
                        topLeft = Offset(width / 2f, width / 2f),
                        size = size.copy(size.width - width, size.height - width),
                        style = Stroke(width),
                    )
                }
            }
    ) {
        content()
    }
}

/** Whether [key] deletes: Delete, or Backspace. */
internal fun isDelete(key: Key): Boolean = key == Key.Delete || key == Key.Backspace

// The swipe: once a finger has gone far enough across, and more across than up, it holds the row;
// before that the list's scroll may take it, and a row that can't go that way lets it.
private suspend fun PointerInputScope.swipes(
    state: SmokeState,
    key: Any,
) {
    val slop = SmokeDimens.Slop.toPx()
    val giveUp = SmokeDimens.VerticalGiveUp.toPx()
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val tracker = VelocityTracker()
        tracker.addPointerInputChange(down)
        var holding = false
        var startX = down.position.x
        try {
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                tracker.addPointerInputChange(change)
                if (!change.pressed) {
                    if (holding) {
                        holding = false
                        state.release(key, tracker.calculateVelocity().x)
                    }
                    break
                }
                val dx = change.position.x - down.position.x
                val dy = change.position.y - down.position.y
                if (!holding) {
                    if (change.isConsumed) break
                    if (abs(dx) > slop && abs(dx) > abs(dy) * 1.2f) {
                        val side = if (dx < 0f) SmokeSide.Left else SmokeSide.Right
                        if (!state.dragStart(key, side)) break
                        holding = true
                        startX = change.position.x
                        change.consume()
                    } else if (abs(dy) > giveUp) break
                } else {
                    change.consume()
                    state.dragTo(key, change.position.x - startX, tracker.calculateVelocity().x)
                }
            }
        } finally {
            if (holding) state.release(key, 0f)
        }
    }
}

/** The burning edge's colours: the ember, its core, the char behind it, and the mist's edge. */
internal class EdgeColors(val ember: Color, val core: Color, val char: Color, val mist: Color)

/**
 * Draws a row cut at its plume's front, along a ragged edge that flickers as it burns: a char band,
 * a glow, a hot rim and a white-hot core for smoke, a soft cool haze for mist. Its paths are made
 * once and reused.
 */
internal class EdgePainter {
    private val clip = Path()
    private val line = Path()
    private val xs = FloatArray(SmokeDimens.EdgePoints + 1)

    fun ContentDrawScope.drawCut(
        plume: SmokePlume,
        layer: GraphicsLayer,
        now: Float,
        colors: EdgeColors,
    ) {
        val width = size.width
        val height = size.height
        val front = plume.front
        when (plume.phase) {
            PlumePhase.Burnt,
            PlumePhase.Waiting -> return
            PlumePhase.Reforming -> {
                // The row comes back in the last of the reforming, as its smoke arrives, from the
                // side that never burnt.
                val shown = smoothstep(0.7f, 1f, plume.reform)
                if (front.fromRight) {
                    val right = (front.edge + (1f - front.edge) * shown) * width
                    clipRect(right = right) { drawLayer(layer) }
                } else {
                    val left = front.edge * (1f - shown) * width
                    clipRect(left = left) { drawLayer(layer) }
                }
                return
            }
            PlumePhase.Held,
            PlumePhase.Burning -> Unit
        }
        if (front.burnt <= 0f) {
            drawLayer(layer)
            return
        }
        val at = front.edge * width
        val ragged = SmokeDimens.Ragged.toPx()
        val seed = (plume.hashCode() and 1023) / 1023f
        val t = now * (1f + seed * 0.3f)
        val n = SmokeDimens.EdgePoints
        for (i in 0..n) {
            xs[i] =
                at +
                    ragged *
                        (sin(i * 1.73f + t * 6.1f + seed * 9f) +
                            0.6f * sin(i * 3.37f - t * 10.7f + seed * 4f) +
                            0.35f * sin(i * 7.1f + t * 17.3f))
        }
        // What's left of the row lies on the side that hasn't burnt.
        val outside = if (front.fromRight) -2f else width + 2f
        clip.rewind()
        clip.moveTo(outside, 0f)
        for (i in 0..n) clip.lineTo(xs[i], height * i / n)
        clip.lineTo(outside, height)
        clip.close()
        clipPath(clip) { drawLayer(layer) }
        line.rewind()
        line.moveTo(xs[0], 0f)
        for (i in 1..n) line.lineTo(xs[i], height * i / n)
        // Behind the front, on the row's side: the way the front runs is away from it.
        val back = -plume.travel
        if (plume.material == SmokeMaterial.Mist) {
            translate(left = back * SmokeDimens.MistBack.toPx()) {
                drawPath(
                    line,
                    colors.mist.copy(alpha = 0.35f),
                    style = stroke(SmokeDimens.MistEdgeWidth),
                )
                drawPath(
                    line,
                    colors.mist.copy(alpha = 0.75f),
                    style = stroke(SmokeDimens.MistLine),
                )
            }
            return
        }
        translate(left = back * SmokeDimens.CharBack.toPx()) {
            drawPath(line, colors.char.copy(alpha = 0.3f), style = stroke(SmokeDimens.CharWidth))
        }
        translate(left = back * SmokeDimens.RimBack.toPx()) {
            drawPath(line, colors.ember.copy(alpha = 0.22f), style = stroke(SmokeDimens.GlowWidth))
            drawPath(line, colors.ember.copy(alpha = 0.4f), style = stroke(SmokeDimens.GlowInner))
            drawPath(line, colors.ember.copy(alpha = 0.8f), style = stroke(SmokeDimens.RimWidth))
        }
        translate(left = back * SmokeDimens.CoreBack.toPx()) {
            drawPath(line, colors.core, style = stroke(SmokeDimens.CoreWidth))
        }
    }

    private fun DrawScope.stroke(width: Dp) =
        Stroke(width.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
}
