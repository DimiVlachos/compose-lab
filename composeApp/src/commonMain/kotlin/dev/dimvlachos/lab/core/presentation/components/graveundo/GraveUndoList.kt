package dev.dimvlachos.lab.core.presentation.components.graveundo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.dimvlachos.lab.core.platform.Sound
import dev.dimvlachos.lab.core.platform.SoundPlayer
import dev.dimvlachos.lab.core.platform.playVaried
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.ui.GraveUndoScreenColors
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.fraunces_bold
import dev.dimvlachos.lab.resources.fraunces_semibold
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.Font

/** The clips the grave plays. */
internal val GraveUndoSounds =
    setOf(
        Sound.GraveTick,
        Sound.GraveSwipe,
        Sound.GraveThud,
        Sound.GraveCrumble,
        Sound.GraveOoo,
        Sound.GraveClick,
        Sound.GraveLift,
    )

/** Where a task's title goes in the strings below. */
internal const val GraveTitleSlot = "{title}"

/**
 * What the list says: the canvas's [words], the list's [label] for screen readers, a row's [delete]
 * action, its check's [markDone] (with [GraveTitleSlot]) and [done] state, the grave's [undo]
 * button (with the slot), and what is announced when a task is [deleted] or [restored].
 */
internal class GraveUndoText(
    val words: GraveWords,
    val label: String,
    val delete: String,
    val markDone: String,
    val done: String,
    val notDone: String,
    val undo: String,
    val deleted: String,
    val restored: String,
)

/**
 * A to-do list where a task swiped away is buried: its row arcs down into a little moonlit
 * graveyard where the snackbar goes and sinks into the soil, its name carved on the headstone, and
 * a skeleton hand pushes up holding it. Tap the hand (or the grave) and it hands the task back to
 * its place; leave it and it sinks and the grave slides away.
 *
 * [header] is the screen's heading over the list, which starts 4 dp under it. [hint] shows over the
 * foot of the list as [GraveUndoState.hint] fades it in; [emptyState] fills the list when nothing
 * is left. [bottomInset] is how far the screen's foot is from the grave's 12 dp gap.
 */
@Composable
internal fun GraveUndoList(
    state: GraveUndoState,
    text: GraveUndoText,
    colors: GraveUndoScreenColors,
    sounds: SoundPlayer,
    ring: Color,
    modifier: Modifier = Modifier,
    bottomInset: Float = 0f,
    header: @Composable () -> Unit,
    hint: @Composable () -> Unit,
    emptyState: @Composable () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var said by remember { mutableStateOf("") }
    // What to say next and how many times something was said: the region is cleared first, as the
    // prototype's say() does, so the same words twice are announced twice.
    var pending by remember { mutableStateOf("") }
    var sayings by remember { mutableIntStateOf(0) }
    LaunchedEffect(sayings) {
        if (sayings == 0) return@LaunchedEffect
        said = ""
        delay(30)
        said = pending
    }
    LaunchedEffect(state, sounds, haptics, text) {
        val listener: (GraveEvent, GraveItem?) -> Unit = { event, item ->
            when (event) {
                GraveEvent.Arm -> {
                    sounds.playVaried(Sound.GraveTick)
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                }
                GraveEvent.Buried -> {
                    sounds.playVaried(Sound.GraveSwipe)
                    pending = text.deleted.replace(GraveTitleSlot, item!!.task.title)
                    sayings++
                }
                GraveEvent.Thud -> {
                    sounds.playVaried(Sound.GraveThud)
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                }
                GraveEvent.Rise -> {
                    sounds.playVaried(Sound.GraveCrumble)
                    sounds.play(Sound.GraveOoo)
                }
                GraveEvent.Restored -> {
                    sounds.playVaried(Sound.GraveClick)
                    sounds.playVaried(Sound.GraveLift)
                    pending = text.restored.replace(GraveTitleSlot, item!!.task.title)
                    sayings++
                }
            }
        }
        state.onEvent = listener
        try {
            awaitCancellation()
        } finally {
            if (state.onEvent === listener) state.onEvent = null
        }
    }
    LaunchedEffect(state, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var last = 0L
            while (true) {
                // At rest the loop sleeps, until a touch, a key or the script moves something.
                if (state.settled) {
                    val seen = state.frame
                    snapshotFlow { state.frame }.first { it != seen }
                    last = 0L
                }
                withFrameNanos { now ->
                    // The first frame moves nothing; a stall can't throw the cards about.
                    val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(.15f)
                    last = now
                    state.step(dt)
                }
                state.syncUndoItem()
            }
        }
    }

    val density = LocalDensity.current
    val serif =
        FontFamily(
            Font(Res.font.fraunces_semibold, FontWeight.SemiBold),
            Font(Res.font.fraunces_bold, FontWeight.Bold),
        )
    val fonts = remember(serif) { mapOf(GraveSans to FontFamily.Default, GraveSerif to serif) }
    // Seven rows of three lines, the stone's letters and the grave's two lines: more than the
    // default cache holds.
    val measurer = rememberTextMeasurer(cacheSize = 96)
    val painter =
        remember(text.words, density.density) { GraveUndoPainter(text.words, density.density) }
    var boxW by remember { mutableFloatStateOf(0f) }
    var boxH by remember { mutableFloatStateOf(0f) }
    var headH by remember { mutableFloatStateOf(0f) }
    fun relayout() {
        if (boxW > 0f && headH > 0f)
            state.layout(
                boxW / density.density,
                boxH / density.density,
                headH / density.density + 4f,
                bottomInset,
                density.fontScale,
            )
    }

    fun DrawScope.paint(block: (Canvas2D) -> Unit) {
        state.frame
        if (state.width <= 0f) return
        drawIntoCanvas { canvas ->
            val c = Canvas2D(canvas, size.width.toInt(), size.height.toInt())
            c.textMeasurer = measurer
            c.fonts = fonts
            c.textResolution = this.density
            c.scale(this.density, this.density)
            block(c)
        }
    }

    Box(
        modifier
            .onSizeChanged {
                boxW = it.width.toFloat()
                boxH = it.height.toFloat()
                relayout()
            }
            .pointerInput(state) { graveGesture(state) },
    ) {
        Canvas(Modifier.fillMaxSize()) { paint { painter.drawList(it, state, colors) } }
        Box(
            Modifier.fillMaxWidth().onSizeChanged {
                headH = it.height.toFloat()
                relayout()
            },
        ) {
            header()
        }
        if (state.listVersion >= 0 && state.empty) {
            Box(
                Modifier.fillMaxWidth().layout { m, c ->
                    val p = m.measure(c)
                    layout(p.width, p.height) {
                        p.place(0, (state.listTop * density.density).roundToInt())
                    }
                },
            ) {
                emptyState()
            }
        }
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = state.hint }) { hint() }
        Canvas(Modifier.fillMaxSize()) { paint { painter.drawGrave(it, state, colors) } }
        Nodes(state, text, ring)
        Box(
            Modifier.size(1.dp).semantics {
                liveRegion = LiveRegionMode.Polite
                if (said.isNotEmpty()) contentDescription = said
            },
        )
    }
}

// The finger on the list, as the prototype's pointer handlers: down, moves, up. A press on the
// grave or the hand that stays a tap undoes; one on a row's check that stays a tap ticks it; a
// touch cancelled lets go as a pointerup does. A real touch takes over from the script first.
private suspend fun PointerInputScope.graveGesture(state: GraveUndoState) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        state.scripted = false
        val x = down.position.x / density
        val y = down.position.y / density
        val onHand =
            state.handHitOn &&
                x >= state.handHitX &&
                x <= state.handHitX + state.handHitW &&
                y >= state.handHitY &&
                y <= state.handHitY + state.handHitH
        val onSlot =
            state.slotOn &&
                x >= state.slotX &&
                x <= state.slotX + state.slotW &&
                y >= state.slotY &&
                y <= state.slotY + state.slotH
        if (onHand || onSlot) {
            var moved = false
            var lifted = false
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (
                    abs(change.position.x - down.position.x) > viewConfiguration.touchSlop ||
                        abs(change.position.y - down.position.y) > viewConfiguration.touchSlop
                )
                    moved = true
                if (!change.pressed) {
                    lifted = !change.isConsumed
                    break
                }
            }
            // Only a finger lifted where it went down undoes; a cancelled touch does nothing.
            if (lifted && !moved) state.undo()
            return@awaitEachGesture
        }
        state.down(x, y, down.uptimeMillis.toDouble())
        var ended = false
        var last = down.position
        try {
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id }
                if (change == null) break
                // A cancelled touch comes as every pointer up, already consumed: it lets go as
                // the prototype's pointercancel does, and the row springs back.
                if (!change.pressed && change.isConsumed) break
                if (change.positionChanged()) {
                    last = change.position
                    state.move(
                        change.position.x / density,
                        change.position.y / density,
                        change.uptimeMillis.toDouble(),
                    )
                }
                // The lift-off's own position counts, as the prototype's pointerup moves first.
                if (!change.pressed) {
                    ended = true
                    val tap = state.up()
                    // A tap on the check, with a finger's slop round its 24 dp ring, ticks it.
                    if (tap && last.x / density in 6f..54f)
                        state.tapped(x, y)?.let { state.toggleDone(it) }
                    break
                }
            }
        } finally {
            if (!ended) state.up(cancel = true)
        }
    }
}

/** The rows, their checks and the grave as screen readers and keys find them, over the canvas. */
@Composable
private fun Nodes(state: GraveUndoState, text: GraveUndoText, ring: Color) {
    state.listVersion
    val density = LocalDensity.current.density
    // Each row's focus, so a Delete can hand focus to its neighbour, as the prototype keeps it.
    val requesters = remember { mutableMapOf<GraveItem, FocusRequester>() }
    Box(
        Modifier.fillMaxSize().semantics {
            isTraversalGroup = true
            contentDescription = text.label
        },
    ) {
        val live = state.liveItems
        for (item in live) {
            key(item) {
                var focused by remember { mutableStateOf(false) }
                val requester = remember { FocusRequester() }
                DisposableEffect(item) {
                    requesters[item] = requester
                    onDispose { requesters.remove(item) }
                }
                fun delete(): Boolean {
                    val i = live.indexOf(item)
                    val next = live.getOrNull(i + 1) ?: live.getOrNull(i - 1)
                    if (!state.bury(item, -1f)) return false
                    if (focused) next?.let { requesters[it] }?.requestFocus()
                    return true
                }
                Box(
                    Modifier.layout { m, _ ->
                            state.frame
                            val w = (state.width * density).roundToInt()
                            val h = (state.rowH * density).roundToInt()
                            val p = m.measure(Constraints.fixed(w.coerceAtLeast(0), h))
                            layout(p.width, p.height) {
                                p.place(0, (state.rowTop(item) * density).roundToInt())
                            }
                        }
                        .testTag("task:${item.task.title}")
                        .semantics {
                            contentDescription =
                                "${item.task.title}, ${item.task.sub}, ${item.task.time}"
                            customActions =
                                listOf(CustomAccessibilityAction(text.delete) { delete() })
                        }
                        .onFocusChanged { focused = it.isFocused }
                        .onKeyEvent { e ->
                            if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                            when (e.key) {
                                Key.Delete,
                                Key.Backspace -> delete()
                                else -> false
                            }
                        }
                        .focusRequester(requester)
                        .focusable()
                        .drawBehindRing(focused, ring),
                ) {
                    // The check, a 44 dp target round its 24 dp ring.
                    Box(
                        Modifier.layout { m, c ->
                                val s = (44 * density).roundToInt()
                                val p = m.measure(Constraints.fixed(s, s))
                                layout(p.width, p.height) {
                                    p.place((8 * density).roundToInt(), (c.maxHeight - s) / 2)
                                }
                            }
                            .testTag("check:${item.task.title}")
                            .semantics {
                                role = Role.Checkbox
                                contentDescription =
                                    text.markDone.replace(GraveTitleSlot, item.task.title)
                                stateDescription = if (item.done) text.done else text.notDone
                                onClick {
                                    state.toggleDone(item)
                                    true
                                }
                            },
                    )
                }
            }
        }
        val g = state.undoItem
        var focused by remember { mutableStateOf(false) }
        Box(
            Modifier.layout { m, _ ->
                    state.frame
                    val p =
                        m.measure(
                            Constraints.fixed(
                                (state.slotW * density).roundToInt().coerceAtLeast(0),
                                (state.slotH * density).roundToInt(),
                            ),
                        )
                    layout(p.width, p.height) {
                        p.place(
                            (state.slotX * density).roundToInt(),
                            (state.slotY * density).roundToInt(),
                        )
                    }
                }
                .testTag("grave")
                .then(
                    if (g == null) Modifier.clearAndSetSemantics {}
                    else
                        Modifier.semantics {
                                role = Role.Button
                                contentDescription = text.undo.replace(GraveTitleSlot, g.task.title)
                                onClick { state.undo() }
                            }
                            .onFocusChanged { focused = it.isFocused }
                            .onKeyEvent { e ->
                                val press =
                                    e.key == Key.Enter ||
                                        e.key == Key.Spacebar ||
                                        e.key == Key.NumPadEnter
                                if (!press || e.type != KeyEventType.KeyDown) false
                                else state.undo()
                            }
                            .focusable()
                            .drawBehindRing(focused, ring, radius = 20f),
                ),
        )
    }
}

// The prototype's focus outline: 2 wide, just inside the box, round its corners.
private fun Modifier.drawBehindRing(on: Boolean, ring: Color, radius: Float = 0f) = drawBehind {
    if (on) focusRing(ring, radius.dp.toPx())
}

private fun DrawScope.focusRing(color: Color, radius: Float) {
    val w = 2.dp.toPx()
    val out = 2.dp.toPx() + w / 2
    drawRoundRect(
        color,
        topLeft = Offset(out, out),
        size = Size(size.width - 2 * out, size.height - 2 * out),
        cornerRadius = CornerRadius(radius),
        style = Stroke(w),
    )
}
