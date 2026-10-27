package dev.dimvlachos.lab.core.presentation.components.paintroller

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
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
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.dimvlachos.lab.core.platform.Sound
import dev.dimvlachos.lab.core.platform.SoundLoop
import dev.dimvlachos.lab.core.platform.SoundPlayer
import dev.dimvlachos.lab.core.platform.playVaried
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.canvas2d.cssColor
import dev.dimvlachos.lab.core.presentation.canvas2d.setBlur
import dev.dimvlachos.lab.core.presentation.ui.PaintRollerScreenColors
import dev.dimvlachos.lab.core.presentation.ui.RollerPaints
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.max
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first

/** The clips the paint roller plays. */
internal val PaintRollerSounds = setOf(Sound.RollerDip, Sound.RollerSet, Sound.RollerRoll)

/** Where the composed screen puts what the roller needs to know, reported by the base coat. */
internal class Geometry(val state: PaintRollerState) {
    var density = 1f
    var root: LayoutCoordinates? = null
    var section: LayoutCoordinates? = null
    val trays = arrayOfNulls<LayoutCoordinates>(state.paintCount)
    val wet = arrayOfNulls<LayoutCoordinates>(state.wetCount)

    fun update() {
        val r = root?.takeIf { it.isAttached } ?: return
        val s = section?.takeIf { it.isAttached } ?: return
        fun rect(c: LayoutCoordinates): Rect {
            val o = r.localPositionOf(c, Offset.Zero) / density
            return Rect(o.x, o.y, o.x + c.size.width / density, o.y + c.size.height / density)
        }
        val trayRects = trays.map { c -> rect(c?.takeIf { it.isAttached } ?: return) }
        val centres = wet.map { c -> rect(c?.takeIf { it.isAttached } ?: return).center }
        state.layout(
            r.size.width / density,
            r.size.height / density,
            rect(s).top,
            trayRects,
            centres,
        )
    }
}

/**
 * What a screen painted by the roller is composed with. The screen is composed twice: the base coat
 * in the current accent and, over it, the new coat ([isCoat]) in the roller's colour, clipped to
 * where the roller has been. Fill whatever wears the accent with [accent] (ink on it with
 * [onAccent]), mark each fill that takes wet paint with [wetPaint], put [Trays] in the section
 * marked [accentSection], and give the new coat no input: it is only paint.
 */
@Stable
internal class PaintRollerScope
internal constructor(
    val state: PaintRollerState,
    val isCoat: Boolean,
    private val geometry: Geometry?,
    private val dark: Boolean,
    val colors: PaintRollerScreenColors,
) {
    private val paints = RollerPaints.map { cssColor(it.css(dark)) }

    /** The accent this coat wears. */
    val accent: Color
        get() = paints[if (isCoat) max(0, state.coat) else state.current]

    val onAccent: Color
        get() = colors.onAccent

    /** An accent fill shaped [shape] that shines while wet; [index] is its place among them. */
    fun Modifier.wetPaint(index: Int, shape: Shape, under: Boolean = false): Modifier {
        val g = geometry
        val placed =
            if (g == null) this
            else
                onGloballyPositioned {
                    g.wet[index] = it
                    g.update()
                }
        return placed.wetSheen(shape, under) {
            state.frame
            (if (isCoat) state.wetCoat(index) else state.wetBase(index)) * colors.gloss
        }
    }

    /** The section the trays sit in: the wall stops just above it, and the new coat hides it. */
    fun Modifier.accentSection(): Modifier {
        val g = geometry ?: return graphicsLayer { alpha = 0f }
        return onGloballyPositioned {
            g.section = it
            g.update()
        }
    }

    /**
     * The five trays, as radio buttons named [names], [height] tall, the roller drawn over them.
     * Pressing one dips the roller, a tap paints the screen, and a drag paints where it goes.
     */
    @Composable
    fun Trays(
        names: List<String>,
        height: Dp,
        modifier: Modifier = Modifier,
        onTouch: () -> Unit = {},
    ) {
        Row(
            modifier.fillMaxWidth().then(if (isCoat) Modifier else Modifier.selectableGroup()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val focus = remember { List(names.size) { FocusRequester() } }
            for (i in names.indices) {
                TrayButton(i, names[i], height, focus, onTouch, Modifier.weight(1f))
            }
        }
    }

    @Composable
    private fun TrayButton(
        i: Int,
        name: String,
        height: Dp,
        focus: List<FocusRequester>,
        onTouch: () -> Unit,
        modifier: Modifier,
    ) {
        val checked = state.current == i
        val label =
            TextStyle(
                color = if (checked) colors.ink else colors.muted,
                fontSize = 11.sp,
                fontWeight = FontWeight(if (checked) 750 else 600),
                textAlign = TextAlign.Center,
            )
        if (isCoat) {
            Box(modifier.height(height))
            return
        }
        val g = checkNotNull(geometry)
        val interaction = remember { MutableInteractionSource() }
        val focused by interaction.collectIsFocusedAsState()
        val input = LocalInputModeManager.current
        val ring = accent
        Box(
            modifier
                .height(height)
                .onGloballyPositioned {
                    g.trays[i] = it
                    g.update()
                }
                .focusRequester(focus[i])
                .onKeyEvent {
                    if (it.type != KeyEventType.KeyDown) return@onKeyEvent false
                    val j =
                        when (it.key) {
                            Key.DirectionRight,
                            Key.DirectionDown -> (state.current + 1) % state.paintCount
                            Key.DirectionLeft,
                            Key.DirectionUp ->
                                (state.current + state.paintCount - 1) % state.paintCount
                            Key.Enter,
                            Key.NumPadEnter,
                            Key.Spacebar -> i
                            else -> return@onKeyEvent false
                        }
                    onTouch()
                    if (state.pickTray(j)) runCatching { focus[j].requestFocus() }
                    true
                }
                .focusable(interactionSource = interaction)
                .drawBehind {
                    // The keyboard's focus ring, never after a touch.
                    if (focused && input.inputMode == InputMode.Keyboard) {
                        val o = 1.dp.toPx()
                        drawRoundRect(
                            ring,
                            Offset(-o - 1.dp.toPx(), -o - 1.dp.toPx()),
                            Size(
                                size.width + 2 * (o + 1.dp.toPx()),
                                size.height + 2 * (o + 1.dp.toPx()),
                            ),
                            CornerRadius(13.dp.toPx()),
                            style = Stroke(2.dp.toPx()),
                        )
                    }
                }
                .semantics(mergeDescendants = true) {
                    role = Role.RadioButton
                    selected = checked
                    contentDescription = name
                    onClick {
                        onTouch()
                        state.pickTray(i)
                        true
                    }
                }
                .pointerInput(state, i) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        onTouch()
                        val origin = state.trayRects.getOrNull(i) ?: return@awaitEachGesture
                        fun x(o: Offset) = origin.left + o.x / density
                        fun y(o: Offset) = origin.top + o.y / density
                        // A press the roller ignored (it is busy) is no drag of this finger's.
                        if (!state.trayDown(i, x(down.position), y(down.position))) {
                            return@awaitEachGesture
                        }
                        try {
                            while (true) {
                                val e = awaitPointerEvent()
                                val c = e.changes.firstOrNull { it.id == down.id }
                                if (c == null) {
                                    state.trayCancel()
                                    break
                                }
                                c.consume()
                                if (!c.pressed) {
                                    state.trayUp(x(c.position), y(c.position))
                                    break
                                }
                                state.trayMove(x(c.position), y(c.position))
                            }
                        } catch (e: CancellationException) {
                            state.trayCancel()
                            throw e
                        }
                    }
                },
            contentAlignment = Alignment.BottomCenter,
        ) {
            BasicText(name, Modifier.clearAndSetSemantics {}, style = label, maxLines = 1)
        }
    }
}

/**
 * An Appearance screen whose accent colours are five little paint trays with a roller resting in
 * one, as the prototype draws it. Dip the roller in a new colour (tap a tray, or press it and drag
 * up) and roll it over the screen: every accent fill it passes takes the new paint, glossy and wet,
 * then dries flat. Let go early and it finishes the wall itself and goes home; a snackbar offers
 * [undoLabel]. [names] name the paints, [snackText] and [said] word the snackbar and the
 * announcement for a paint's name. [onTouch] hears a real finger or key on the trays or Undo.
 *
 * Layers, bottom to top: the base coat, the new coat clipped to the paint, the trays and roller,
 * the snackbar and the live region.
 */
@Composable
internal fun PaintRollerAccent(
    state: PaintRollerState,
    colors: PaintRollerScreenColors,
    dark: Boolean,
    sounds: SoundPlayer,
    names: List<String>,
    snackText: (String) -> String,
    said: (String) -> String,
    undoLabel: String,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false,
    snackPadding: Dp = 0.dp,
    onTouch: () -> Unit = {},
    content: @Composable PaintRollerScope.() -> Unit,
) {
    SideEffect { state.reducedMotion = reducedMotion }
    val haptics = LocalHapticFeedback.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val density = LocalDensity.current.density
    val geometry = remember(state) { Geometry(state) }
    SideEffect { geometry.density = density }
    LaunchedEffect(state, sounds, haptics) {
        val listener: (RollerEvent) -> Unit = { event ->
            when (event) {
                RollerEvent.Dip -> {
                    sounds.playVaried(Sound.RollerDip, .9f)
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                }
                RollerEvent.Commit -> haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                RollerEvent.Set -> {
                    sounds.playVaried(Sound.RollerSet, .7f)
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
    LaunchedEffect(state, lifecycle, sounds) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            // The wet nap on the wall, as loud as the roller is fast, playing only while awake.
            var roll: SoundLoop? = null
            try {
                var last = 0L
                while (true) {
                    // At rest the loop sleeps, until a tap or the script sets the roller moving.
                    if (state.settled) {
                        roll?.stop()
                        roll = null
                        val seen = state.wake
                        snapshotFlow { state.wake }.first { it != seen }
                        last = 0L
                    }
                    withFrameNanos { now ->
                        // The first frame moves nothing; a stall can't jump the roller.
                        val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(.15f)
                        last = now
                        state.step(dt)
                    }
                    val loud = (state.rollLevel * 2f).coerceIn(0f, 1f)
                    if (roll == null && loud > .01f) roll = sounds.loop(Sound.RollerRoll, 0f)
                    roll?.setVolume(loud)
                }
            } finally {
                roll?.stop()
            }
        }
    }

    val base =
        remember(state, colors, dark) { PaintRollerScope(state, false, geometry, dark, colors) }
    val coat = remember(state, colors, dark) { PaintRollerScope(state, true, null, dark, colors) }
    val painted = remember(state) { PaintedClip() }
    Box(
        modifier.onGloballyPositioned {
            geometry.root = it
            geometry.update()
        },
    ) {
        Box(Modifier.fillMaxSize()) { base.content() }
        // The new coat: an exact copy of the screen in the roller's colour, opaque so the old
        // colour never fringes its text, drawn only where the roller has been.
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer()
                .clearAndSetSemantics {}
                .drawWithContent {
                    state.paintVersion
                    if (state.coat < 0 || state.rects.isEmpty()) return@drawWithContent
                    if (painted.version != state.paintVersion || painted.density != density) {
                        state.paintedPath(painted.path, density)
                        painted.version = state.paintVersion
                        painted.density = density
                    }
                    clipPath(painted.path) { this@drawWithContent.drawContent() }
                }
                .background(colors.background),
        ) {
            if (state.coat >= 0) coat.content()
        }
        Canvas(Modifier.fillMaxSize().graphicsLayer()) {
            state.frame
            drawIntoCanvas { canvas ->
                val c = Canvas2D(canvas, size.width.toInt(), size.height.toInt())
                c.scale(density, density)
                c.drawPaintRoller(state, dark, density)
            }
        }
        val name = names[state.current]
        Snackbar(
            state = state,
            text = snackText(name),
            undoLabel = undoLabel,
            undoColor = shade(cssColor(RollerPaints[state.current].css(dark)), .45f),
            reducedMotion = reducedMotion,
            bottom = state.snackBottom.dp + snackPadding,
            onTouch = onTouch,
            rootOf = { geometry.root ?: it },
        )
        // What changed, for a screen reader.
        val (heard, count) = state.said
        // Made anew for each announcement, so the same colour said twice is heard twice.
        key(count) {
            Box(
                Modifier.size(1.dp).semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = if (heard >= 0) said(names[heard]) else ""
                },
            )
        }
    }
}

private class PaintedClip {
    val path = Path()
    var version = -1
    var density = 0f
}

private val SnackShadow = Paint().apply { color = Color(0f, 0f, 0f, .3f) }

private val SnackEase = CubicBezierEasing(.2f, .9f, .25f, 1f)

/** The prototype's snackbar: it slides up from under the screen's foot, and Undo rolls back. */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.Snackbar(
    state: PaintRollerState,
    text: String,
    undoLabel: String,
    undoColor: Color,
    reducedMotion: Boolean,
    bottom: Dp,
    onTouch: () -> Unit,
    rootOf: (LayoutCoordinates) -> LayoutCoordinates,
) {
    val density = LocalDensity.current.density
    val on = state.snackOn
    val slide by
        animateFloatAsState(
            if (on) 0f else 1.4f,
            if (reducedMotion) snap() else tween(300, easing = SnackEase),
            label = "snack",
        )
    val fade by
        animateFloatAsState(
            if (on) 1f else 0f,
            if (reducedMotion) snap() else tween(200),
            label = "snackFade",
        )
    if (!on && fade <= 0f) return
    Row(
        Modifier.align(Alignment.BottomCenter)
            .padding(start = 12.dp, end = 12.dp, bottom = bottom)
            .fillMaxWidth()
            .graphicsLayer {
                translationY = slide * size.height
                alpha = fade
            }
            .drawBehind {
                // box-shadow: 0 10px 30px rgba(0,0,0,.3).
                drawIntoCanvas {
                    SnackShadow.setBlur(15.dp.toPx())
                    it.drawRoundRect(
                        0f,
                        10.dp.toPx(),
                        size.width,
                        size.height + 10.dp.toPx(),
                        14.dp.toPx(),
                        14.dp.toPx(),
                        SnackShadow,
                    )
                }
            }
            .background(PaintRollerScreenColors.Snack, RoundedCornerShape(14.dp))
            // While up, the bar takes the touches that land on it, as the prototype's does.
            .pointerInput(on) { if (on) awaitEachGesture { awaitFirstDown().consume() } }
            .padding(start = 16.dp, top = 6.dp, end = 6.dp, bottom = 6.dp)
            .then(if (on) Modifier else Modifier.clearAndSetSemantics {}),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text,
            Modifier.weight(1f),
            style =
                TextStyle(
                    color = PaintRollerScreenColors.SnackText,
                    fontSize = 14.sp,
                    lineHeight = 1.55.em,
                ),
        )
        Box(
            Modifier.heightIn(min = 40.dp)
                .onGloballyPositioned { c ->
                    val root = rootOf(c)
                    val o = root.localPositionOf(c, Offset.Zero)
                    val d = density
                    state.undoCentre =
                        Offset((o.x + c.size.width / 2f) / d, (o.y + c.size.height / 2f) / d)
                }
                .pointerInput(state, on) {
                    if (!on) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        onTouch()
                        val up = waitForUpOrCancellation()
                        if (up != null) state.undo()
                    }
                }
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    onClick {
                        onTouch()
                        state.undo()
                        true
                    }
                }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                undoLabel,
                style =
                    TextStyle(color = undoColor, fontSize = 14.5.sp, fontWeight = FontWeight.Bold),
            )
        }
    }
}
