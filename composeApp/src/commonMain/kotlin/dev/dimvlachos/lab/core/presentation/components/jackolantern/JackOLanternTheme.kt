package dev.dimvlachos.lab.core.presentation.components.jackolantern

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.dimvlachos.lab.core.platform.Sound
import dev.dimvlachos.lab.core.platform.SoundPlayer
import dev.dimvlachos.lab.core.platform.playVaried
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.ui.JackOLanternScreenColors
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first

/** The clips the jack-o'-lantern plays. */
internal val JackOLanternSounds =
    setOf(Sound.LanternStrike, Sound.LanternCatch, Sound.LanternBlow, Sound.LanternCrackle)

/** Where the composed screen puts the pumpkin's slot, reported by the day copy. */
internal class LanternGeometry(val state: JackOLanternState) {
    var density = 1f
    var root: LayoutCoordinates? = null
    var slot: LayoutCoordinates? = null

    fun update() {
        val r = root?.takeIf { it.isAttached } ?: return
        val s = slot?.takeIf { it.isAttached } ?: return
        val b = r.localBoundingBoxOf(s, clipBounds = false)
        state.layout(
            r.size.width / density,
            r.size.height / density,
            b.center.x / density,
            b.center.y / density,
        )
    }
}

/**
 * What a screen themed by the jack-o'-lantern is composed with. The screen is composed twice: by
 * day underneath and, over it, by night ([night]), shown only where the candle's light has spread.
 * Style it with [colors], put the top bar's pumpkin slot under [pumpkinSlot], and give the night
 * copy no input: it is only light.
 */
@Stable
internal class JackOLanternScope
internal constructor(
    val night: Boolean,
    val colors: JackOLanternScreenColors,
    private val geometry: LanternGeometry?,
) {
    /**
     * The top bar's room for the pumpkin, 64 by 60 dp in the prototype; the day copy reports it.
     */
    fun Modifier.pumpkinSlot(): Modifier {
        val g = geometry ?: return this
        return onGloballyPositioned {
            g.slot = it
            g.update()
        }
    }
}

/**
 * A [Canvas2D] over a layer's canvas, kept from frame to frame while the layer draws into the same
 * canvas at the same size, with its bounds: drawing makes no new wrapper each frame.
 */
private class LayerCanvas {
    private var canvas: androidx.compose.ui.graphics.Canvas? = null
    private var c2d: Canvas2D? = null
    private var rect = Rect.Zero

    fun get(
        canvas: androidx.compose.ui.graphics.Canvas,
        w: Float,
        h: Float,
        density: Float,
    ): Canvas2D {
        val kept = c2d
        val c =
            if (
                kept != null &&
                    canvas === this.canvas &&
                    kept.width == w.toInt() &&
                    kept.height == h.toInt()
            )
                kept
            else
                Canvas2D(canvas, w.toInt(), h.toInt()).also {
                    this.canvas = canvas
                    c2d = it
                }
        c.setTransform(density, 0f, 0f, density, 0f, 0f)
        return c
    }

    fun bounds(w: Float, h: Float): Rect {
        if (rect.width != w || rect.height != h) rect = Rect(0f, 0f, w, h)
        return rect
    }
}

/** The baked grin, kept until the screen changes size. */
private class Projection {
    var w = 0f
    var h = 0f
    var image: ImageBitmap? = null

    fun get(width: Float, height: Float): ImageBitmap? {
        if (width <= 0f || height <= 0f) return null
        if (width != w || height != h || image == null) {
            w = width
            h = height
            image = bakeProjection(width, height)
        }
        return image
    }
}

/**
 * A screen whose theme switch is a carved pumpkin in its top bar, as the prototype draws it. Tap
 * the pumpkin and the candle catches, and the night theme spreads out of its face in one soft-edged
 * circle of light, with the grin cast faintly across the feed. Flick across the flame, from either
 * side, and it blows out: smoke curls off the lid and the night draws back into the pumpkin. Either
 * change turns round mid-flight. [switchLabel] names the switch for a screen reader, [nightSaid]
 * and [daySaid] are announced as the theme changes, and [hintLight] and [hintBlow] word the hint
 * chip.
 *
 * Layers, bottom to top: the day copy, the night copy (with the candle's glow) masked to the light,
 * the pumpkin and its smoke, the touch zone, the switch, the hint and the live region.
 */
@Composable
internal fun JackOLanternTheme(
    state: JackOLanternState,
    sounds: SoundPlayer,
    switchLabel: String,
    nightSaid: String,
    daySaid: String,
    hintLight: String,
    hintBlow: String,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false,
    content: @Composable JackOLanternScope.() -> Unit,
) {
    SideEffect { state.reducedMotion = reducedMotion }
    val haptics = LocalHapticFeedback.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val density = LocalDensity.current.density
    val geometry = remember(state) { LanternGeometry(state) }
    SideEffect { geometry.density = density }
    LaunchedEffect(state, sounds, haptics) {
        val listener: (LanternEvent) -> Unit = { event ->
            when (event) {
                LanternEvent.Strike -> sounds.playVaried(Sound.LanternStrike, .8f)
                LanternEvent.Catch -> {
                    sounds.playVaried(Sound.LanternCatch, .8f)
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                }
                LanternEvent.Blow -> {
                    sounds.playVaried(Sound.LanternBlow, .9f)
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                }
                LanternEvent.Crackle -> sounds.playVaried(Sound.LanternCrackle, .6f)
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
                // Out and still with the hint up (or a script playing), nothing moves: the clock
                // sleeps until the theme or the hint changes.
                if (state.settled && (state.hintOn || state.scripted)) {
                    val seen = state.said to state.hintOn
                    snapshotFlow { state.said to state.hintOn }.first { it != seen }
                    last = 0L
                }
                withFrameNanos { now ->
                    // The first frame moves nothing; a stall can't jump the candle.
                    val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(.15f)
                    last = now
                    state.advance(dt)
                }
            }
        }
    }

    val day =
        remember(geometry) { JackOLanternScope(false, JackOLanternScreenColors.Day, geometry) }
    val night = remember { JackOLanternScope(true, JackOLanternScreenColors.Night, null) }
    val paints = remember(state) { LanternPaints() }
    val glowPaints = remember(state) { LanternPaints() }
    val projection = remember(state) { Projection() }
    val screen = remember { Paint().apply { blendMode = BlendMode.Screen } }
    val glowCanvas = remember { LayerCanvas() }
    val lanternCanvas = remember { LayerCanvas() }
    Box(
        modifier.onGloballyPositioned {
            geometry.root = it
            geometry.update()
        },
    ) {
        Box(Modifier.fillMaxSize()) { day.content() }
        // The night copy: an exact copy of the screen in the night theme, with the candle's light
        // laid over it, shown in a soft-edged circle from the pumpkin as far as the light has
        // spread.
        Box(
            Modifier.fillMaxSize()
                .clearAndSetSemantics {}
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    state.frame
                    val s = state.spread()
                    if (s <= .0005f || !state.laidOut) return@drawWithContent
                    drawContent()
                    drawIntoCanvas { canvas ->
                        canvas.saveLayer(glowCanvas.bounds(size.width, size.height), screen)
                        val c = glowCanvas.get(canvas, size.width, size.height, density)
                        c.drawCandleGlow(
                            state,
                            glowPaints,
                            projection.get(state.width, state.height),
                        )
                        canvas.restore()
                    }
                    if (s < .9995f) {
                        val big = nightRadius(s, state.px, state.py, state.width, state.height)
                        val solid = max(0f, big - NightFeather) / big
                        drawRect(
                            Brush.radialGradient(
                                0f to Color.Black,
                                solid to Color.Black,
                                1f to Color.Transparent,
                                center = Offset(state.px * density, state.py * density),
                                radius = max(1f, big * density),
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    }
                },
        ) {
            night.content()
        }
        // The pumpkin and the smoke off its lid, in a layer of their own as the prototype's canvas
        // is: its halo and lid glow add light to the pumpkin, not to the screen under it.
        Canvas(
            Modifier.fillMaxSize().graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            },
        ) {
            state.frame
            if (!state.laidOut) return@Canvas
            drawIntoCanvas { canvas ->
                lanternCanvas
                    .get(canvas, size.width, size.height, density)
                    .drawLantern(state, paints)
            }
        }
        if (state.laidOut) {
            // Where a finger works the candle: the top of the screen, down well past the pumpkin,
            // so a flick can start and end clear of it.
            Box(
                Modifier.fillMaxWidth()
                    .height((state.py + 190f).dp)
                    .clearAndSetSemantics {}
                    .pointerInput(state) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            down.consume()
                            val d = density
                            state.down(
                                down.position.x / d,
                                down.position.y / d,
                                down.uptimeMillis.toDouble(),
                            )
                            try {
                                while (true) {
                                    val e = awaitPointerEvent()
                                    val c = e.changes.firstOrNull { it.id == down.id }
                                    if (c == null) {
                                        state.cancel()
                                        break
                                    }
                                    c.consume()
                                    val x = c.position.x / d
                                    val y = c.position.y / d
                                    if (!c.pressed) {
                                        state.up(x, y, c.uptimeMillis.toDouble())
                                        break
                                    }
                                    state.move(x, y, c.uptimeMillis.toDouble())
                                }
                            } catch (e: CancellationException) {
                                state.cancel()
                                throw e
                            }
                        }
                    },
            )
            LanternSwitch(state, switchLabel)
            Hint(state, if (state.hintLit) hintBlow else hintLight, reducedMotion)
        }
        // Which theme the screen is in now, for a screen reader; made anew for each change, so
        // the same theme said twice is heard twice.
        key(state.said) {
            Box(
                Modifier.size(1.dp).semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription =
                        if (state.said == 0) "" else if (state.lit) nightSaid else daySaid
                },
            )
        }
    }
}

/**
 * The pumpkin as a switch for a screen reader and the keyboard: Night theme, on while lit. Its
 * touches are the zone's, so a tap on a lit pumpkin only wobbles the flame, as the prototype's.
 */
@Composable
private fun LanternSwitch(state: JackOLanternState, label: String) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val input = LocalInputModeManager.current
    Box(
        Modifier.offset {
                IntOffset(
                    ((state.px - 36f) * density).roundToInt(),
                    ((state.py - 38f) * density).roundToInt(),
                )
            }
            .size(72.dp)
            .onKeyEvent {
                if (it.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (it.key) {
                    Key.Spacebar,
                    Key.Enter,
                    Key.NumPadEnter -> {
                        state.toggle()
                        true
                    }
                    else -> false
                }
            }
            .semantics {
                role = Role.Switch
                contentDescription = label
                toggleableState = ToggleableState(state.lit)
                onClick {
                    state.toggle()
                    true
                }
            }
            .focusable(interactionSource = interaction)
            .drawBehind {
                // The keyboard's focus ring: outline 2px #f08a3c, 2 px out, round.
                if (focused && input.inputMode == InputMode.Keyboard) {
                    drawCircle(
                        JackOLanternScreenColors.Night.accent,
                        size.minDimension / 2f + 3.dp.toPx(),
                        style = Stroke(2.dp.toPx()),
                    )
                }
            },
    )
}

/** The prototype's hint chip under the pumpkin, at its right edge, fading in and out. */
@Composable
private fun Hint(state: JackOLanternState, text: String, reducedMotion: Boolean) {
    // The fade is read only where it is drawn; composition hears only whether it shows at all.
    val shown =
        animateFloatAsState(
            if (state.hintOn) 1f else 0f,
            if (reducedMotion) snap() else tween(500),
            label = "hint",
        )
    val visible by remember { derivedStateOf { shown.value > 0f } }
    if (!visible) return
    Box(Modifier.fillMaxSize().clearAndSetSemantics {}, contentAlignment = Alignment.TopEnd) {
        BasicText(
            text,
            Modifier.padding(
                    top = (state.py + state.r + 36f).dp,
                    end = max(10f, state.width - state.px - 30f).dp,
                )
                .graphicsLayer { alpha = shown.value }
                .background(JackOLanternScreenColors.Hint, RoundedCornerShape(999.dp))
                .border(1.dp, Color(255, 255, 255, 31), RoundedCornerShape(999.dp))
                .padding(horizontal = 11.dp, vertical = 6.dp),
            style =
                TextStyle(
                    color = JackOLanternScreenColors.HintText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            maxLines = 1,
        )
    }
}
