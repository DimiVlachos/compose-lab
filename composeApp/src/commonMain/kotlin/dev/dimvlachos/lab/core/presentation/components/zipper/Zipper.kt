package dev.dimvlachos.lab.core.presentation.components.zipper

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.dimvlachos.lab.core.platform.Sound
import dev.dimvlachos.lab.core.platform.SoundPlayer
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.gelasio_bold
import kotlin.random.Random
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.Font

/** The clips the zipper plays. */
internal val ZipperSounds = setOf(Sound.ZipClack, Sound.ZipTick, Sound.ZipTeeth, Sound.ZipLift)

/** How wide the strip down the right that takes the zipper's touches is. */
internal val ZipperStripWidth = 52.dp

/**
 * A brass zipper sewn down the right of a list, as the prototype draws it: the fast-scroll thumb of
 * [track]'s list. Lay it over the list area, the same size. The list starts zipped shut and opens
 * behind the pull as it goes down; holding the pull stands its tab up with a cotton tag showing the
 * letter at the top of the list, and dragging it scrolls the list. A tap on the tape glides the
 * pull there. [label] names it for screen readers, which can set it, step it a letter at a time
 * with its actions ([nextLetter], [previousLetter]), and keys do the same.
 */
@Composable
internal fun Zipper(
    state: ZipperState,
    track: ZipperTrack,
    label: String,
    nextLetter: String,
    previousLetter: String,
    sounds: SoundPlayer,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val density = LocalDensity.current.density
    LaunchedEffect(state, sounds, haptics) {
        val listener: (ZipperEvent, Float) -> Unit = { event, k ->
            when (event) {
                ZipperEvent.Clack -> {
                    sounds.play(Sound.ZipClack, .4f + .6f * k)
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                }
                ZipperEvent.Tick -> sounds.play(Sound.ZipTick, (.3f + 2f * k).coerceAtMost(1f))
                ZipperEvent.Letter -> {
                    sounds.play(Sound.ZipTick, .6f)
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                }
                ZipperEvent.Teeth ->
                    sounds.play(
                        Sound.ZipTeeth,
                        .35f + .15f * k,
                        .85f + Random.nextFloat() * .4f,
                    )
                ZipperEvent.Lift -> {
                    sounds.play(Sound.ZipLift, .8f)
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
    LaunchedEffect(state, track) {
        // The list scrolled by a finger or a fling: the pull follows, so the loop wakes.
        launch {
            snapshotFlow {
                track.list.firstVisibleItemIndex * 100_000 + track.list.firstVisibleItemScrollOffset
            }
                .collect { state.poke() }
        }
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var last = 0L
            while (true) {
                if (state.settled) {
                    val seen = state.wake
                    snapshotFlow { state.wake }.first { it != seen }
                    last = 0L
                }
                withFrameNanos { now ->
                    // The first frame moves nothing; a stall can't blow the springs up.
                    val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(.15f)
                    last = now
                    track.measure()
                    state.follow(track.position())
                    state.step(dt)
                    state.takeScrollRequest()?.let { track.scrollTo(it) }
                    state.showLetter(
                        track.letterAt(state.pos * track.maxScroll() + LetterProbe * density),
                    )
                }
            }
        }
    }
    val dark = LabTheme.isDark
    val weave = remember { zipperWeave() }
    val measurer = rememberTextMeasurer()
    val serif = Font(Res.font.gelasio_bold)
    val fonts = remember(serif) { mapOf("serif" to FontFamily(serif)) }
    Box(modifier.onSizeChanged { state.layout(it.height / density) }) {
        Canvas(Modifier.fillMaxSize()) {
            state.frame
            drawIntoCanvas { canvas ->
                val c = Canvas2D(canvas, size.width.toInt(), size.height.toInt())
                c.textMeasurer = measurer
                c.fonts = fonts
                c.scale(density, density)
                c.drawZipper(state, size.width / density, dark, weave, density / PixelRatio)
            }
        }
        val percent = state.percent
        Box(
            Modifier.align(Alignment.TopEnd)
                .width(ZipperStripWidth)
                .fillMaxHeight()
                .testTag("zipper")
                .semantics {
                    contentDescription = label
                    stateDescription = state.letter
                    progressBarRangeInfo = ProgressBarRangeInfo(percent / 100f, 0f..1f, 99)
                    setProgress { p ->
                        state.glideTo(p)
                        true
                    }
                    customActions =
                        listOf(
                            CustomAccessibilityAction(nextLetter) {
                                state.glideTo(track.letterStep(down = true))
                                true
                            },
                            CustomAccessibilityAction(previousLetter) {
                                state.glideTo(track.letterStep(down = false))
                                true
                            },
                        )
                }
                .onKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                    val to =
                        when (e.key) {
                            Key.DirectionDown,
                            Key.PageDown -> track.letterStep(down = true)
                            Key.DirectionUp,
                            Key.PageUp -> track.letterStep(down = false)
                            Key.MoveHome -> 0f
                            Key.MoveEnd -> 1f
                            else -> return@onKeyEvent false
                        }
                    state.glideTo(to)
                    true
                }
                .focusable()
                .pointerInput(state) {
                    awaitEachGesture {
                        val first = awaitFirstDown()
                        first.consume()
                        state.down(first.position.y / density)
                        val id = first.id
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == id }
                            if (change == null) {
                                state.cancel()
                                break
                            }
                            if (!change.pressed) {
                                state.up(change.position.y / density)
                                break
                            }
                            change.consume()
                            state.move(change.position.y / density)
                        }
                    }
                },
        )
    }
}

/** How far below the list's top the tag reads its letter, as the prototype's `scrollTop + 30`. */
private const val LetterProbe = 30f

/** The browser's pixel ratio the prototype's shadow sizes are given at. */
private const val PixelRatio = 2f
