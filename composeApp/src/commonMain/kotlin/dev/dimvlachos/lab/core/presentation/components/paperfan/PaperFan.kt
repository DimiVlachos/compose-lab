package dev.dimvlachos.lab.core.presentation.components.paperfan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.dimvlachos.lab.core.platform.Sound
import dev.dimvlachos.lab.core.platform.SoundPlayer
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.core.presentation.ui.PaperFanScreenColors
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.gelasio_bold
import dev.dimvlachos.lab.resources.gelasio_regular
import kotlin.math.roundToInt
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.Font

/** The clips the fans play. */
internal val PaperFanSounds =
    setOf(Sound.PaperFanFwap, Sound.PaperFanClick, Sound.PaperFanSwish, Sound.PaperFanClack)

/** A question and its answer. */
internal data class FaqEntry(val question: String, val answer: String)

/**
 * A card of [entries] whose expand arrows are little folded paper fans, as the prototype draws
 * them: tap a question and its fan lifts out of the row and snaps open across the card with the
 * answer printed on its pleats; tap it again, or sweep the open fan shut with a finger, and it
 * folds back into the row. Only one answer is out at a time ([state]). The list scrolls in its
 * space with [scroll]; [expanded] and [collapsed] are what a screen reader says of a question.
 * [slots] is where the rows, the answer areas and the fans' rivets were last drawn, in the card's
 * own dp, for a script's fingertip to find them.
 */
@Composable
internal fun PaperFanFaq(
    entries: List<FaqEntry>,
    state: PaperFanState,
    sounds: SoundPlayer,
    colors: PaperFanScreenColors,
    expanded: String,
    collapsed: String,
    modifier: Modifier = Modifier,
    scroll: ScrollState = rememberScrollState(),
    slots: FanSlots = remember(entries.size) { FanSlots(entries.size) },
) {
    val haptics = LocalHapticFeedback.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(state, sounds, haptics) {
        val listener: (PaperFanEvent, Int) -> Unit = { event, _ ->
            when (event) {
                PaperFanEvent.Fwap -> sounds.play(Sound.PaperFanFwap)
                PaperFanEvent.Click -> {
                    sounds.play(Sound.PaperFanClick)
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                }
                PaperFanEvent.Swish -> sounds.play(Sound.PaperFanSwish)
                PaperFanEvent.Clack -> {
                    sounds.play(Sound.PaperFanClack)
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
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
                // At rest the loop sleeps, until a tap or a finger sets the springs moving again.
                if (state.settled) {
                    val seen = state.wake
                    snapshotFlow { state.wake }.first { it != seen }
                    last = 0L
                }
                withFrameNanos { now ->
                    // The first frame moves nothing; a stall can't blow the springs up.
                    val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(.15f)
                    last = now
                    state.step(dt)
                }
            }
        }
    }
    val dark = LabTheme.isDark
    val density = LocalDensity.current.density
    val measurer = rememberTextMeasurer()
    val serif =
        FontFamily(
            Font(Res.font.gelasio_regular, FontWeight.Normal),
            Font(Res.font.gelasio_bold, FontWeight.Bold),
        )
    val count = entries.size
    val painter = remember(count) { PaperFanPainter(count) }
    val rows = remember(count) { arrayOfNulls<LayoutCoordinates>(count) }
    val panels = remember(count) { arrayOfNulls<LayoutCoordinates>(count) }
    val here = remember { arrayOfNulls<LayoutCoordinates>(1) }
    val fiber = remember(density) { bakeFiber(density) }

    BoxWithConstraints(modifier.onGloballyPositioned { here[0] = it }) {
        // The card is the list's width less its 14 dp margins and its hairline border.
        val r = fanRadius(maxWidth.value - 30f)
        val openHeight = fanOpenHeight(r)
        // Each fan's print, baked the first time it is drawn, and again if the size or the
        // typeface (which may still be loading) changes.
        val prints = remember(entries, r, density, serif) { arrayOfNulls<ImageBitmap>(count) }
        val shape = RoundedCornerShape(18.dp)
        Column(
            Modifier.fillMaxSize()
                .verticalScroll(scroll)
                .padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
        ) {
            Column(
                Modifier.fillMaxWidth()
                    .clip(shape)
                    .background(colors.card)
                    .border(1.dp, colors.line, shape)
                    .padding(1.dp),
            ) {
                entries.forEachIndexed { i, entry ->
                    if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
                    val open = state.isOpen(i)
                    Box(
                        Modifier.fillMaxWidth()
                            .heightIn(min = 54.dp)
                            .onGloballyPositioned { rows[i] = it }
                            .testTag("question-$i")
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Button,
                            ) {
                                state.toggle(i)
                            }
                            .semantics {
                                stateDescription = if (open) expanded else collapsed
                                if (open) {
                                    collapse {
                                        state.toggle(i)
                                        true
                                    }
                                } else {
                                    expand {
                                        state.toggle(i)
                                        true
                                    }
                                }
                            }
                            .padding(start = 16.dp, end = 70.dp, top = 12.dp, bottom = 12.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            entry.question,
                            color = colors.ink,
                            fontSize = 15.sp,
                            lineHeight = 1.3.em,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    // The answer area: as tall as the fan's travel lets it be, set as it is laid
                    // out, so the fan's springs never recompose the list. Its answer is real text
                    // for a screen reader; the fan is only its picture.
                    Box(
                        Modifier.fillMaxWidth()
                            .layout { measurable, constraints ->
                                state.frame
                                val h =
                                    (state.heightFraction(i) * openHeight * density)
                                        .roundToInt()
                                        .coerceIn(constraints.minHeight, constraints.maxHeight)
                                val p =
                                    measurable.measure(
                                        constraints.copy(minHeight = h, maxHeight = h),
                                    )
                                layout(constraints.maxWidth, h) { p.place(0, 0) }
                            }
                            .onGloballyPositioned { panels[i] = it }
                            .testTag("answer-$i")
                            .clearAndSetSemantics { if (open) contentDescription = entry.answer }
                            .pointerInput(state, i) {
                                val slop = 6.dp.toPx()
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    if (!state.dragStart(i)) return@awaitEachGesture
                                    down.consume()
                                    var moved = false
                                    try {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change =
                                                event.changes.firstOrNull { it.id == down.id }
                                                    ?: break
                                            if (!change.pressed) break
                                            change.consume()
                                            if (
                                                (change.position - down.position).getDistance() >
                                                    slop
                                            ) {
                                                moved = true
                                            }
                                            if (moved) {
                                                val box = here[0]
                                                val panel = panels[i]
                                                if (
                                                    box != null && panel != null && panel.isAttached
                                                ) {
                                                    val at =
                                                        box.localPositionOf(
                                                            panel,
                                                            change.position,
                                                        ) / density
                                                    state.dragTo(
                                                        i,
                                                        PaperFanState.dragFraction(
                                                            at.x - slots.pivotX[i],
                                                            at.y - slots.pivotY[i],
                                                        ),
                                                    )
                                                }
                                            }
                                        }
                                    } finally {
                                        state.dragEnd(i, moved)
                                    }
                                }
                            },
                    )
                }
            }
        }
        // The fans, over the list and clipped to it, read where every row and answer area is as
        // they are drawn.
        Canvas(Modifier.matchParentSize().clearAndSetSemantics {}) {
            state.frame
            scroll.value
            val box = here[0] ?: return@Canvas
            if (!box.isAttached) return@Canvas
            for (i in 0 until count) {
                val row = rows[i]?.takeIf { it.isAttached } ?: return@Canvas
                val panel = panels[i]?.takeIf { it.isAttached } ?: return@Canvas
                val ro = box.localPositionOf(row, Offset.Zero)
                slots.rowX[i] = ro.x / density
                slots.rowY[i] = ro.y / density
                slots.rowW[i] = row.size.width / density
                slots.rowH[i] = row.size.height / density
                val po = box.localPositionOf(panel, Offset.Zero)
                slots.panelX[i] = po.x / density
                slots.panelY[i] = po.y / density
                slots.panelW[i] = panel.size.width / density
                slots.panelH[i] = panel.size.height / density
            }
            drawIntoCanvas { canvas ->
                val c = Canvas2D(canvas, size.width.toInt(), size.height.toInt())
                c.scale(density, density)
                painter.draw(
                    c,
                    state,
                    slots,
                    { i ->
                        prints[i]
                            ?: bakePrint(entries[i].answer, r, density, fiber, measurer, serif)
                                .image
                                .also { prints[i] = it }
                    },
                    r,
                    size.width / density,
                    size.height / density,
                    dark,
                    density,
                )
            }
        }
    }
}
