package dev.dimvlachos.lab.core.presentation.components.stickynote

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.dimvlachos.lab.core.platform.Sound
import dev.dimvlachos.lab.core.platform.SoundPlayer
import dev.dimvlachos.lab.core.platform.playVaried
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.caveat_semibold
import kotlin.math.max
import kotlin.random.Random
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.Font

/** The clips the notes play. */
internal val StickyNoteSounds =
    setOf(
        Sound.NoteGrain,
        Sound.NoteTick,
        Sound.NoteRip,
        Sound.NoteStick,
        Sound.NotePat,
        Sound.NoteLift,
    )

/** Stands in for a note's title in [StickyNoteText]'s announcements. */
internal const val TitleSlot = "\u0001"

/**
 * What the list says: the canvas's [words], the list's own [label] for screen readers, what is
 * announced when a note is [archived], [snoozed] or [putBack], each with [TitleSlot] for its title,
 * and what the [empty] list says, after the last note's announcement too.
 */
internal class StickyNoteText(
    val words: NoteWords,
    val label: String,
    val archived: String,
    val snoozed: String,
    val putBack: String,
    val empty: String,
)

/**
 * A reminders list where every row is a sticky note, as the prototype draws it. Swipe one and it
 * peels from the corner over Archive (left) or Snooze (right); pull it past 42 % of its width, or
 * flick it, and it comes off and flies onto the Done or Later pad under the list; let go early and
 * it smooths back down. Tapping a pad puts its top note back; tapping a note lifts its corner.
 *
 * [topBar] is the screen's bar over the list: the notes that peel or fly are drawn over it, so the
 * paper covers its text, and the list starts 10 under it. [padLabel] names a pad and how many notes
 * it holds for screen readers; [ring] is the keyboard focus ring. [emptyState] is centred between
 * the list's top and the pads' labels while no note is left, fading in as the last one leaves and
 * out as a pad puts one back; screen readers read it as [StickyNoteText.empty].
 */
@Composable
internal fun StickyNoteList(
    state: StickyNoteState,
    text: StickyNoteText,
    padLabel: @Composable (NoteAction, Int) -> String,
    sounds: SoundPlayer,
    ring: Color,
    modifier: Modifier = Modifier,
    emptyState: @Composable () -> Unit,
    topBar: @Composable () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var said by remember { mutableStateOf("") }
    LaunchedEffect(state, sounds, haptics, text) {
        val listener: (StickyNoteEvent, Reminder?, Float) -> Unit = { event, item, amount ->
            when (event) {
                StickyNoteEvent.Grain ->
                    sounds.playVaried(Sound.NoteGrain, amount, .7f + Random.nextFloat() * .6f)
                StickyNoteEvent.Tick -> {
                    sounds.playVaried(Sound.NoteTick)
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                }
                StickyNoteEvent.Rip -> sounds.playVaried(Sound.NoteRip)
                StickyNoteEvent.Stick -> {
                    sounds.playVaried(Sound.NoteStick)
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                }
                StickyNoteEvent.Pat -> sounds.playVaried(Sound.NotePat)
                StickyNoteEvent.Lift -> sounds.playVaried(Sound.NoteLift)
                // The prototype's peek is the same flick, a little higher and softer.
                StickyNoteEvent.Peek -> sounds.playVaried(Sound.NoteLift, .8f, 1.23f)
                StickyNoteEvent.Archived ->
                    said = text.archived.replace(TitleSlot, item!!.title) + emptied(state, text)
                StickyNoteEvent.Snoozed ->
                    said = text.snoozed.replace(TitleSlot, item!!.title) + emptied(state, text)
                StickyNoteEvent.PutBack -> said = text.putBack.replace(TitleSlot, item!!.title)
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
                    // The first frame moves nothing; a stall can't throw the notes about.
                    val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(.15f)
                    last = now
                    state.step(dt)
                }
            }
        }
    }

    val density = LocalDensity.current.density
    val inks = if (LabTheme.isDark) NoteInks.Dusk else NoteInks.Day
    val hand = FontFamily(Font(Res.font.caveat_semibold, FontWeight.SemiBold))
    val fonts = remember(hand) { mapOf(HandFont to hand, SansFont to FontFamily.Default) }
    // Every note's two lines, the panels' and the pads': more than the default cache holds.
    val measurer = rememberTextMeasurer(cacheSize = 64)
    val paper = remember { notePaper() }
    val painter =
        remember(paper, text.words, density) { StickyNotePainter(paper, text.words, density) }
    var boxW by remember { mutableFloatStateOf(0f) }
    var boxH by remember { mutableFloatStateOf(0f) }
    var barH by remember { mutableFloatStateOf(0f) }
    fun relayout() {
        if (boxW > 0f && barH > 0f)
            state.layout(boxW / density, boxH / density, barH / density + 10)
    }

    fun DrawScope.paint(lift: Boolean) {
        state.frame
        if (state.width <= 0f) return
        drawIntoCanvas { canvas ->
            val c = Canvas2D(canvas, size.width.toInt(), size.height.toInt())
            c.textMeasurer = measurer
            c.fonts = fonts
            c.scale(density, density)
            if (lift) painter.drawLift(c, state, inks) else painter.drawBase(c, state, inks)
        }
    }

    Box(
        modifier
            .onSizeChanged {
                boxW = it.width.toFloat()
                boxH = it.height.toFloat()
                relayout()
            }
            .pointerInput(state) { notesGesture(state) },
    ) {
        Canvas(Modifier.fillMaxSize()) { paint(lift = false) }
        Box(
            Modifier.fillMaxWidth().onSizeChanged {
                barH = it.height.toFloat()
                relayout()
            },
        ) {
            topBar()
        }
        EmptyNotes(state, text.empty, emptyState)
        // Peeling and flying notes go on this layer, above the app bar.
        Canvas(Modifier.fillMaxSize()) { paint(lift = true) }
        Nodes(state, text, padLabel, ring)
        Box(
            Modifier.size(1.dp).semantics {
                liveRegion = LiveRegionMode.Polite
                if (said.isNotEmpty()) contentDescription = said
            },
        )
    }
}

// What the last note's announcement adds: that the list is now empty.
private fun emptied(state: StickyNoteState, text: StickyNoteText) =
    if (state.rows.isEmpty()) " ${text.empty}" else ""

/**
 * The prototype's `.empty`: over the list, under the lift layer so a note flying back passes over
 * it. It fades in over 420 ms after 180 as the last note leaves, and out over 200 ms when one comes
 * back, at once with reduced motion; it is read only while the list is empty.
 */
@Composable
private fun EmptyNotes(state: StickyNoteState, said: String, content: @Composable () -> Unit) {
    state.listVersion
    val empty = state.rows.isEmpty()
    val shown by
        animateFloatAsState(
            if (empty) 1f else 0f,
            when {
                state.reducedMotion -> snap()
                empty -> tween(420, delayMillis = 180, easing = CssEase)
                else -> tween(200, easing = CssEase)
            },
            label = "empty",
        )
    if (!empty && shown <= 0f) return
    val top = state.top0
    val height = max(0f, state.padY() - 20 - top)
    Box(
        Modifier.fillMaxWidth()
            .offset(y = top.dp)
            .height(height.dp)
            .graphicsLayer { alpha = shown }
            .then(
                if (empty)
                    Modifier.testTag("empty").clearAndSetSemantics { contentDescription = said }
                else Modifier.clearAndSetSemantics {},
            )
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

// CSS's `ease`.
private val CssEase = CubicBezierEasing(.25f, .1f, .25f, 1f)

// The finger on the list, as the prototype's pointer handlers: down, moves, up; a touch cancelled
// lets go as a pointerup does. A real touch takes over from the script's finger first.
private suspend fun PointerInputScope.notesGesture(
    state: StickyNoteState,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        state.releaseScripted()
        state.down(
            down.position.x / density,
            down.position.y / density,
            down.uptimeMillis.toDouble(),
        )
        var ended = false
        try {
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id }
                if (change == null || !change.pressed) break
                if (change.positionChanged()) {
                    state.move(
                        change.position.x / density,
                        change.position.y / density,
                        change.uptimeMillis.toDouble(),
                    )
                }
            }
            ended = true
            state.up()
        } finally {
            if (!ended && !state.scriptedTouch) state.up()
        }
    }
}

/** The notes and pads as screen readers and keys find them, over where they are drawn. */
@Composable
private fun Nodes(
    state: StickyNoteState,
    text: StickyNoteText,
    padLabel: @Composable (NoteAction, Int) -> String,
    ring: Color,
) {
    state.listVersion
    // Each note's focus, so an action can hand focus to its neighbour in the list, as the
    // prototype keeps it: the next note, or the one before from the last.
    val requesters = remember { mutableMapOf<NoteRow, FocusRequester>() }
    Box(
        Modifier.fillMaxSize().semantics {
            isTraversalGroup = true
            contentDescription = text.label
        },
    ) {
        for (row in state.rows.toList()) {
            key(row) {
                var focused by remember { mutableStateOf(false) }
                val requester = remember { FocusRequester() }
                DisposableEffect(row) {
                    requesters[row] = requester
                    onDispose { requesters.remove(row) }
                }
                // An action moves focus on to the next note, as the list closes up under it.
                fun act(kind: NoteAction): Boolean {
                    if (row.state == RowState.Tween || row.state == RowState.Gone) return false
                    val i = state.rows.indexOf(row)
                    val next = state.rows.getOrNull(i + 1) ?: state.rows.getOrNull(i - 1)
                    if (focused) next?.let { requesters[it] }?.requestFocus()
                    state.commit(row, kind, fromKey = true)
                    return true
                }
                val actions =
                    listOf(
                        CustomAccessibilityAction(text.words.archive) { act(NoteAction.Archive) },
                        CustomAccessibilityAction(text.words.snooze) { act(NoteAction.Snooze) },
                    )
                Box(
                    Modifier.offset(state.mx.dp, row.ty.dp)
                        .size(state.noteW.dp, state.rh.dp)
                        .testTag("note:${row.item.title}")
                        .then(
                            if (row.hidden) Modifier.clearAndSetSemantics {}
                            else
                                Modifier.semantics {
                                    contentDescription = "${row.item.title}, ${row.item.meta}"
                                    customActions = actions
                                },
                        )
                        .onFocusChanged { focused = it.isFocused }
                        .onKeyEvent { e ->
                            if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                            when (e.key) {
                                Key.DirectionLeft,
                                Key.Delete,
                                Key.Backspace -> act(NoteAction.Archive)
                                Key.DirectionRight -> act(NoteAction.Snooze)
                                else -> false
                            }
                        }
                        .focusRequester(requester)
                        .focusable(enabled = !row.hidden)
                        .drawBehind { if (focused) focusRing(ring, 4.dp.toPx()) },
                )
            }
        }
        for (kind in NoteAction.entries) {
            val label = padLabel(kind, state.pad(kind).stack.size)
            var focused by remember(kind) { mutableStateOf(false) }
            Box(
                Modifier.offset(state.padX(kind).dp, (state.padY() - 22).dp)
                    .size(state.pw.dp, (state.ph + 22).dp)
                    .testTag("pad:${kind.name.lowercase()}")
                    .semantics {
                        role = Role.Button
                        contentDescription = label
                        onClick {
                            state.undo(kind)
                            true
                        }
                    }
                    .onFocusChanged { focused = it.isFocused }
                    .onKeyEvent { e ->
                        val press =
                            e.key == Key.Enter || e.key == Key.Spacebar || e.key == Key.NumPadEnter
                        if (!press || e.type != KeyEventType.KeyDown) return@onKeyEvent false
                        state.undo(kind)
                        true
                    }
                    .focusable()
                    .drawBehind { if (focused) focusRing(ring, 8.dp.toPx()) },
            )
        }
    }
}

// The prototype's focus outline: 2 wide, 3 out from the box, its corners rounded by [radius] + 3.
private fun DrawScope.focusRing(color: Color, radius: Float) {
    val w = 2.dp.toPx()
    val out = 3.dp.toPx() + w / 2
    drawRoundRect(
        color,
        topLeft = Offset(-out, -out),
        size = Size(size.width + 2 * out, size.height + 2 * out),
        cornerRadius = CornerRadius(radius + out),
        style = Stroke(w),
    )
}
