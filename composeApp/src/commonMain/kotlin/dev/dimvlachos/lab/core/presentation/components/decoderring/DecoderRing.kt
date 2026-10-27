package dev.dimvlachos.lab.core.presentation.components.decoderring

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
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
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.jetbrains_mono_semibold
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.Font

/** The clips the decoder ring plays. */
internal val DecoderRingSounds =
    setOf(Sound.DecoderTick, Sound.DecoderSettle, Sound.DecoderDone, Sound.DecoderBack)

/**
 * Plays what [state] does and steps it each frame while anything moves, while the screen is in
 * front. Call once for a ring, wherever its message and its button are.
 */
@Composable
internal fun DecoderRingEffects(state: DecoderRingState, sounds: SoundPlayer) {
    val haptics = LocalHapticFeedback.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(state, sounds, haptics) {
        val listener: (DecoderEvent) -> Unit = { event ->
            when (event) {
                DecoderEvent.Tick -> {
                    sounds.play(Sound.DecoderTick)
                    if (state.held)
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                }
                // The prototype's click is pitched anywhere from 2.6 to 3.5 kHz.
                DecoderEvent.Settle ->
                    sounds.playVaried(Sound.DecoderSettle, rate = .88f + Random.nextFloat() * .3f)
                DecoderEvent.Done -> {
                    sounds.play(Sound.DecoderDone)
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                }
                DecoderEvent.Back -> {
                    sounds.play(Sound.DecoderBack)
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
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
            snapshotFlow { state.awake }
                .collectLatest { awake ->
                    if (!awake) return@collectLatest
                    var last = 0L
                    while (true) {
                        withFrameNanos { now ->
                            // The first frame moves nothing; a stall can't jump the wave.
                            val dt =
                                if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(.15f)
                            last = now
                            state.step(dt)
                        }
                    }
                }
        }
    }
}

/**
 * The message whose letters the ring turns: [spanish] and [english] are both laid out at its width,
 * and it is as tall as the lines its letters are on. Its semantics are the text it shows. The
 * bubble's own box, [shape] and colours are the caller's [modifier]; this fills it, 1 dp in from
 * its border, as the prototype's canvas sits.
 */
@Composable
internal fun DecoderMessage(
    state: DecoderRingState,
    spanish: String,
    english: String,
    ink: Color,
    dark: Boolean,
    modifier: Modifier = Modifier,
    bubble: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val scale = density.fontScale
    val measurer = rememberTextMeasurer()
    val glyphs = remember(density) { GlyphCache() }
    BoxWithConstraints(modifier) {
        val inner = maxWidth.value - Border * 2 - PadX * 2
        val lines =
            remember(inner, density, spanish, english, measurer) {
                val c = Canvas2D.create(1, 1)
                c.textMeasurer = measurer
                c.textResolution = density.density
                c.font = decoderFont(15f * scale)
                val k = density.density
                val measure = { s: String ->
                    if (s.isEmpty()) 0f
                    else c.layoutText(s).getHorizontalPosition(s.length, true) / k
                }
                wrapDecoderText(spanish, inner, measure) to wrapDecoderText(english, inner, measure)
            }
        SideEffect {
            if (state.slots.isEmpty() || state.layoutKey !== lines) {
                state.layoutKey = lines
                state.layout(lines.first, lines.second, DecoderRingState.LineHeight * scale)
            }
        }
        // As tall as the state says, which only lays it out again, never recomposes. Measured
        // after the lines above are laid out, so it is never a frame behind them.
        Canvas(
            bubble
                .semantics { contentDescription = if (state.english) english else spanish }
                .layout { m, c ->
                    val h = (state.height + Border * 2).dp.roundToPx()
                    val p = m.measure(Constraints.fixed(c.maxWidth, h))
                    layout(p.width, p.height) { p.place(0, 0) }
                }
                .padding(Border.dp),
        ) {
            state.frame
            drawIntoCanvas { canvas ->
                val c = Canvas2D(canvas, size.width.roundToInt(), size.height.roundToInt())
                c.textMeasurer = measurer
                c.textResolution = this.density
                c.scale(this.density, this.density)
                c.drawDecoderText(state, ink, dark, scale, glyphs)
            }
        }
    }
}

/**
 * The ring itself: a 62 dp button drawn as the brass decoder ring, its canvas spilling 8 dp round
 * it. Twist it round, either way, to run the wave; a tap, Enter, Space or a screen reader's click
 * toggles. [label] names what a press does, [description] what is shown.
 */
@Composable
internal fun DecoderRingButton(
    state: DecoderRingState,
    label: String,
    description: String,
    focusRing: Color,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current.density
    val measurer = rememberTextMeasurer()
    val mono = Font(Res.font.jetbrains_mono_semibold, FontWeight.SemiBold)
    val fonts = remember(mono) { mapOf(MonoFont to FontFamily(mono)) }
    val glyphs = remember(density, fonts) { RingGlyphs() }
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier
            .size(RingButton.dp)
            .drawBehind {
                // The prototype's focus outline: 2 dp of the accent, 1 dp out.
                if (!focused) return@drawBehind
                val w = 2.dp.toPx()
                drawCircle(
                    focusRing,
                    size.minDimension / 2f + 1.dp.toPx() + w / 2f,
                    style = Stroke(w),
                )
            }
            .pointerInput(state) {
                awaitEachGesture {
                    val first = awaitFirstDown(requireUnconsumed = false)
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val k = this.density
                    val r = RingButton / 2f
                    fun rel(p: Offset) = Offset((p.x - cx) / k, (p.y - cy) / k)
                    var at = rel(first.position)
                    val id = state.down(at.x, at.y)
                    first.consume()
                    var released = false
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == first.id } ?: break
                            at = rel(change.position)
                            if (change.changedToUpIgnoreConsumed()) {
                                released = true
                                if (state.holds(id)) state.up(at.x, at.y, r)
                                break
                            }
                            if (state.holds(id)) state.move(at.x, at.y, r)
                            change.consume()
                        }
                    } finally {
                        // Taken away: it finishes from the last point the finger reached.
                        if (!released && state.holds(id)) state.up(at.x, at.y, r, cancel = true)
                    }
                }
            }
            .onFocusChanged { focused = it.isFocused }
            .testTag("decoder-ring")
            .semantics {
                role = Role.Button
                contentDescription = label
                stateDescription = description
                onClick {
                    state.toggle()
                    true
                }
            }
            .onKeyEvent {
                val press = it.type == KeyEventType.KeyDown
                if (press && (it.key == Key.Spacebar || it.key == Key.Enter)) {
                    state.toggle()
                    true
                } else {
                    false
                }
            }
            .focusable(),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.requiredSize(RingCanvas.dp).clearAndSetSemantics {}) {
            state.frame
            drawIntoCanvas { canvas ->
                val c = Canvas2D(canvas, size.width.roundToInt(), size.height.roundToInt())
                c.textMeasurer = measurer
                c.fonts = fonts
                c.textResolution = this.density
                c.scale(this.density, this.density)
                c.drawDecoderRing(state.ringA, this.density / PrototypePixelRatio, glyphs)
            }
        }
    }
}

private const val MonoFont = "JetBrains Mono"

/** The bubble's border, in dp: the prototype's canvas sits inside it. */
internal const val Border = 1f

/** The browser's pixel ratio the prototype was tuned at, which its shadow sizes are given in. */
private const val PrototypePixelRatio = 2f
