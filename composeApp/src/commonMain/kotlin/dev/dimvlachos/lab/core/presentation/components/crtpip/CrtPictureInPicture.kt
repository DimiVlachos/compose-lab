package dev.dimvlachos.lab.core.presentation.components.crtpip

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.dimvlachos.lab.core.platform.Sound
import dev.dimvlachos.lab.core.platform.SoundPlayer
import dev.dimvlachos.lab.core.platform.playVaried
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.canvas2d.setBlur
import dev.dimvlachos.lab.core.presentation.ui.CrtPipScreenColors
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first

/** The clips the set plays. */
internal val CrtPipSounds = setOf(Sound.CrtOff, Sound.CrtFly, Sound.CrtOn)

/** What the player, the set and the toast say, in the screen's words. */
internal class CrtPipLabels(
    /** The player, for a screen reader: the video and how to shrink it. */
    val player: String,
    /** The chevron. */
    val minimize: String,
    /** The hint over the picture while the controls show. */
    val hint: String,
    /** The set, as a button. */
    val expand: String,
    /** The set's custom action. */
    val stop: String,
    val stopped: String,
    val undo: String,
    val nowMini: String,
    val nowFull: String,
)

/**
 * A video player that switches off like an old TV when you swipe it down: the picture squeezes into
 * a line, the line into a glowing dot, and the dot flies to the corner, where a little walnut set
 * with rabbit ears warms up with the show still playing, as the prototype draws it.
 *
 * It lays out a column of [appBar], the player's slot (16:9, collapsing to nothing as the video
 * leaves it), the [page] filling what is left, and the [navBar]; over them the set, the picture,
 * line and dot, the player's controls, the set's button and the "Playback stopped" toast, in the
 * prototype's order. [onTouch] hears the user take over: a real finger on the player or the set, or
 * a key or screen reader action on the chevron, the set or Undo.
 */
@Composable
internal fun CrtPictureInPicture(
    state: CrtPipState,
    labels: CrtPipLabels,
    colors: CrtPipScreenColors,
    dark: Boolean,
    sounds: SoundPlayer,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false,
    onTouch: () -> Unit = {},
    appBar: @Composable () -> Unit,
    page: @Composable () -> Unit,
    navBar: @Composable () -> Unit,
) {
    // Long enough to reach Undo with a screen reader, or however long the person has asked for.
    val accessibility = LocalAccessibilityManager.current
    val toastMs =
        accessibility
            ?.calculateRecommendedTimeoutMillis(
                CrtPipState.ToastMs.toLong(),
                containsIcons = false,
                containsText = true,
                containsControls = true,
            )
            ?.toFloat() ?: CrtPipState.ToastMs
    SideEffect {
        state.reducedMotion = reducedMotion
        state.toastMs = toastMs
    }
    val haptics = LocalHapticFeedback.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(state, sounds, haptics) {
        val listener: (PipEvent, Float) -> Unit = { event, strength ->
            when (event) {
                PipEvent.Off -> sounds.playVaried(Sound.CrtOff, strength)
                PipEvent.Fly -> sounds.playVaried(Sound.CrtFly, .8f)
                PipEvent.On -> sounds.playVaried(Sound.CrtOn, .9f)
                PipEvent.Commit ->
                    haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                PipEvent.Snap -> haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                PipEvent.Stop -> haptics.performHapticFeedback(HapticFeedbackType.Reject)
                PipEvent.Undo -> haptics.performHapticFeedback(HapticFeedbackType.Confirm)
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
                // Stopped, with nothing pending, the loop sleeps until something moves again.
                if (state.settled) {
                    val seen = state.wake
                    snapshotFlow { state.wake }.first { it != seen }
                    last = 0L
                }
                withFrameNanos { now ->
                    // The first frame moves nothing; a stall can't blow the spring up.
                    val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(.15f)
                    last = now
                    state.step(dt)
                }
            }
        }
    }

    val playerFocus = remember { FocusRequester() }
    val setFocus = remember { FocusRequester() }
    val undoFocus = remember { FocusRequester() }
    // Keyboard focus follows the video: chevron to the set, the set back to the player, Delete
    // to Undo, never dropping to the page.
    LaunchedEffect(state) {
        snapshotFlow { state.focusRequest }
            .collect { to ->
                if (to == PipFocus.None) return@collect
                withFrameNanos {}
                runCatching {
                    when (to) {
                        PipFocus.Set -> setFocus.requestFocus()
                        PipFocus.Player -> playerFocus.requestFocus()
                        PipFocus.Undo -> undoFocus.requestFocus()
                        PipFocus.None -> Unit
                    }
                }
                state.focusRequest = PipFocus.None
            }
    }

    val measurer = rememberTextMeasurer()
    val paints = remember { CrtPipPaints() }
    val noise = remember { Random(7) }
    val fonts = remember { mapOf<String, FontFamily>() }

    Layout(
        modifier = modifier,
        content = {
            Box { appBar() }
            Box(Modifier.background(CrtPipScreenColors.Slot))
            Box(Modifier.clipToBounds()) { page() }
            Box { navBar() }
            // The set, faded as one piece: an offscreen layer laid down once at its alpha.
            Canvas(
                Modifier.graphicsLayer {
                    state.frame
                    val a = if (state.pose.cab) state.pose.cabAlpha else 0f
                    alpha = a
                    // Faded, it is drawn whole offscreen and laid down once at its alpha; at full
                    // alpha there is nothing to composite.
                    compositingStrategy =
                        if (a < 1f) CompositingStrategy.Offscreen else CompositingStrategy.Auto
                },
            ) {
                state.frame
                if (!state.pose.cab) return@Canvas
                drawIntoCanvas { canvas ->
                    val c = Canvas2D(canvas, size.width.toInt(), size.height.toInt())
                    c.textMeasurer = measurer
                    c.fonts = fonts
                    c.textResolution = density
                    c.scale(density, density)
                    c.drawCrtCabinet(state, paints, noise)
                }
            }
            Canvas(Modifier) {
                state.frame
                drawIntoCanvas { canvas ->
                    val c = Canvas2D(canvas, size.width.toInt(), size.height.toInt())
                    c.textMeasurer = measurer
                    c.fonts = fonts
                    c.textResolution = density
                    c.scale(density, density)
                    c.drawCrtFx(state, dark, paints)
                }
            }
            Player(state, labels, playerFocus, onTouch)
            SetButton(state, labels, colors, setFocus, onTouch)
            Toast(state, labels, colors, reducedMotion, undoFocus, onTouch)
        },
    ) { measurables, constraints ->
        val w = constraints.maxWidth
        val h = constraints.maxHeight
        val loose = constraints.copy(minWidth = w, maxWidth = w, minHeight = 0)
        val bar = measurables[0].measure(loose)
        val nav = measurables[3].measure(loose)
        state.layout(
            w.toDp().value,
            h.toDp().value,
            bar.height.toDp().value,
            (h - nav.height).toDp().value,
        )
        val slotPx = (state.slotH * density).roundToInt().coerceAtLeast(0)
        val slot = measurables[1].measure(Constraints.fixed(w, slotPx))
        val pageH = (h - bar.height - slotPx - nav.height).coerceAtLeast(0)
        val pageP = measurables[2].measure(Constraints.fixed(w, pageH))
        val full = Constraints.fixed(w, h)
        val rest = measurables.drop(4).map { it.measure(full) }
        layout(w, h) {
            bar.place(0, 0)
            slot.place(0, bar.height)
            pageP.place(0, bar.height + slotPx)
            nav.place(0, h - nav.height)
            rest.forEach { it.place(0, 0) }
        }
    }
}

/**
 * The player's touch target over its slot: a swipe down pre-squeezes the picture and, let go far
 * enough or fast enough, switches it off into the set; a tap shows the controls. Holds the
 * controls: the shade, the chevron, the hint, the time code and the progress track.
 */
@Composable
private fun Player(
    state: CrtPipState,
    labels: CrtPipLabels,
    focus: FocusRequester,
    onTouch: () -> Unit,
) {
    val full = state.mode == PipMode.Full
    val input = LocalInputModeManager.current
    val playerFocusSource = remember { MutableInteractionSource() }
    val playerFocused by playerFocusSource.collectIsFocusedAsState()
    val chevronFocusSource = remember { MutableInteractionSource() }
    val chevronFocused by chevronFocusSource.collectIsFocusedAsState()
    // The controls also show while the chevron has the keys' focus, so it and its ring are seen.
    val keyOnChevron = chevronFocused && input.inputMode == InputMode.Keyboard
    val shown by
        animateFloatAsState(
            if (state.controlsOn || keyOnChevron) 1f else 0f,
            tween(250),
            label = "ctl",
        )
    val dot by animateFloatAsState(if (state.controlsOn) 1f else 0f, tween(200), label = "knob")
    Box(
        Modifier.layout { measurable, constraints ->
                val top = (state.slotTop * density).roundToInt()
                val hh = (state.slotH * density).roundToInt().coerceAtLeast(0)
                val p = measurable.measure(Constraints.fixed(constraints.maxWidth, hh))
                layout(constraints.maxWidth, constraints.maxHeight) { p.place(0, top) }
            }
            .clipToBounds()
            .testTag("crt-player")
            .focusRequester(focus)
            .focusable(enabled = full, interactionSource = playerFocusSource)
            .drawWithContent {
                drawContent()
                // #player:focus-visible: 2 px white, 3 px in.
                if (playerFocused && input.inputMode == InputMode.Keyboard) focusRing(3.dp.toPx())
            }
            .then(
                if (full) {
                    Modifier.semantics { contentDescription = labels.player }
                        .pointerInput(state) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                down.consume()
                                onTouch()
                                fun x(o: Offset) = o.x / density
                                fun y(o: Offset) = state.slotTop + o.y / density
                                state.playerDown(
                                    x(down.position),
                                    y(down.position),
                                    down.uptimeMillis.toDouble(),
                                )
                                track(
                                    down,
                                    { c -> state.playerMove(x(c.position), y(c.position), c.ms) },
                                    { c -> state.playerUp(x(c.position), y(c.position), c.ms) },
                                    { t -> state.playerCancel(t) },
                                )
                            }
                        }
                } else Modifier.clearAndSetSemantics {},
            ),
    ) {
        // The shade, top and foot: linear-gradient(rgba(0,0,0,.42), transparent 34 % … 70 %,
        // rgba(0,0,0,.45)).
        Box(
            Modifier.fillMaxSize().graphicsLayer { alpha = shown }.background(ControlShade),
        )
        if (full) {
            Box(
                Modifier.padding(start = 6.dp, top = 4.dp)
                    .size(44.dp)
                    .graphicsLayer { alpha = shown }
                    .drawWithContent {
                        drawContent()
                        // #minBtn:focus-visible: 2 px white, 4 px in.
                        if (keyOnChevron) focusRing(4.dp.toPx(), round = true)
                    }
                    .clip(RoundedCornerShape(22.dp))
                    .then(
                        // Touchable only while the controls show, as the prototype's; a screen
                        // reader and keys reach it always.
                        if (state.controlsOn) {
                            Modifier.clickable(
                                role = Role.Button,
                                interactionSource = chevronFocusSource,
                                indication = null,
                            ) {
                                onTouch()
                                state.minimize(keyboard = input.inputMode == InputMode.Keyboard)
                            }
                        } else {
                            Modifier.semantics {
                                    role = Role.Button
                                    onClick {
                                        onTouch()
                                        state.minimize()
                                        true
                                    }
                                }
                                .onKeyEvent {
                                    val enter =
                                        it.key == Key.Enter ||
                                            it.key == Key.NumPadEnter ||
                                            it.key == Key.Spacebar
                                    if (it.type == KeyEventType.KeyDown && enter) {
                                        onTouch()
                                        state.minimize(keyboard = true)
                                        true
                                    } else false
                                }
                                .focusable(interactionSource = chevronFocusSource)
                        },
                    )
                    .semantics { contentDescription = labels.minimize }
                    .testTag("crt-minimize"),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(26.dp)) { drawChevron() }
            }
        }
        BasicText(
            labels.hint,
            Modifier.align(Alignment.TopCenter)
                .padding(top = 14.dp)
                .graphicsLayer { alpha = shown }
                .clearAndSetSemantics {},
            style = HintStyle.copy(shadow = textShadow(LocalDensity.current.density)),
            maxLines = 1,
        )
        TimeCode(
            state,
            Modifier.align(Alignment.BottomStart).graphicsLayer { alpha = shown },
        )
        // The progress track, on the full player at rest; its knob only with the controls.
        Canvas(Modifier.fillMaxSize().clearAndSetSemantics {}) {
            state.frame
            if (!state.trackOn) return@Canvas
            val th = 3.dp.toPx()
            val y = size.height - th
            drawRect(TrackColor, Offset(0f, y), Size(size.width, th))
            val wv = size.width * state.vt / CrtPipState.Duration
            drawRect(CrtPipScreenColors.Progress, Offset(0f, y), Size(wv, th))
            if (dot > 0f) {
                drawCircle(
                    CrtPipScreenColors.Progress.copy(alpha = dot),
                    5.dp.toPx(),
                    Offset(wv, y - 3.5f.dp.toPx() + 5.dp.toPx()),
                )
            }
        }
    }
}

@Composable
private fun TimeCode(state: CrtPipState, modifier: Modifier) {
    val text = "${clock(state.second)} / $Length"
    BasicText(
        text,
        modifier.padding(start = 12.dp, bottom = 10.dp).clearAndSetSemantics {},
        style = TimeStyle.copy(shadow = textShadow(LocalDensity.current.density)),
        maxLines = 1,
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawChevron() {
    val s = size.width / 24f
    val path =
        Path().apply {
            moveTo(5 * s, 9 * s)
            lineTo(12 * s, 16 * s)
            lineTo(19 * s, 9 * s)
        }
    // drop-shadow(0 1px 2px rgba(0,0,0,.5)), then the white stroke.
    translate(0f, 1.dp.toPx()) {
        drawPath(
            path,
            Color(0f, 0f, 0f, .25f),
            style = Stroke(2.4f * s + 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
    drawPath(
        path,
        Color.White,
        style = Stroke(2.4f * s, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

/**
 * The set's button: a tap expands it, a drag moves it and springs it to a corner or flicks it off a
 * side edge to stop playback. "Expand video: …" for a screen reader, with Stop playback as a custom
 * action; Enter or Space expands it, Delete or Backspace stops it.
 */
@Composable
private fun SetButton(
    state: CrtPipState,
    labels: CrtPipLabels,
    colors: CrtPipScreenColors,
    focus: FocusRequester,
    onTouch: () -> Unit,
) {
    val on = state.mode == PipMode.Mini
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val input = LocalInputModeManager.current
    // Where the button was last placed, in dp: a finger's positions are relative to it.
    val placed = remember { FloatArray(2) }
    Box(
        Modifier.layout { measurable, constraints ->
                val p =
                    measurable.measure(
                        Constraints.fixed(
                            (state.cabW * density).roundToInt(),
                            (state.cabH * density).roundToInt(),
                        ),
                    )
                layout(constraints.maxWidth, constraints.maxHeight) {
                    val x = state.cabX
                    val y = state.cabY
                    placed[0] = x
                    placed[1] = y
                    p.place((x * density).roundToInt(), (y * density).roundToInt())
                }
            }
            .testTag("crt-set")
            .drawBehind {
                if (on && focused && input.inputMode == InputMode.Keyboard) {
                    val o = 3.dp.toPx()
                    drawRoundRect(
                        colors.accent,
                        Offset(-o, -o),
                        Size(size.width + 2 * o, size.height + 2 * o),
                        CornerRadius(16.dp.toPx() + o),
                        style = Stroke(2.dp.toPx()),
                    )
                }
            }
            .focusRequester(focus)
            .onKeyEvent {
                if (!on || it.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (it.key) {
                    Key.Enter,
                    Key.NumPadEnter,
                    Key.Spacebar -> {
                        onTouch()
                        state.expand(keyboard = true)
                        true
                    }
                    Key.Delete,
                    Key.Backspace -> {
                        onTouch()
                        state.stopFromKeys()
                        true
                    }
                    else -> false
                }
            }
            .focusable(enabled = on, interactionSource = interaction)
            .then(
                if (on) {
                    Modifier.semantics {
                            contentDescription = labels.expand
                            role = Role.Button
                            onClick {
                                onTouch()
                                state.expand()
                                true
                            }
                            customActions =
                                listOf(
                                    CustomAccessibilityAction(labels.stop) {
                                        onTouch()
                                        state.stopFromKeys()
                                        true
                                    },
                                )
                        }
                        .pointerInput(state) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                down.consume()
                                onTouch()
                                fun x(o: Offset) = placed[0] + o.x / density
                                fun y(o: Offset) = placed[1] + o.y / density
                                state.setDown(
                                    x(down.position),
                                    y(down.position),
                                    down.uptimeMillis.toDouble(),
                                )
                                track(
                                    down,
                                    { c -> state.setMove(x(c.position), y(c.position), c.ms) },
                                    { c -> state.setUp(x(c.position), y(c.position), c.ms) },
                                    { t -> state.setCancel(t) },
                                )
                            }
                        }
                } else Modifier.clearAndSetSemantics {},
            ),
    )
}

/** A focus-visible outline: 2 dp of white, [inset] inside the edge. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.focusRing(
    inset: Float,
    round: Boolean = false,
) {
    val w = 2.dp.toPx()
    val o = inset + w / 2
    drawRoundRect(
        Color.White,
        Offset(o, o),
        Size(size.width - 2 * o, size.height - 2 * o),
        CornerRadius(if (round) (size.minDimension / 2 - o) else 0f),
        style = Stroke(w),
    )
}

private val PointerInputChange.ms
    get() = uptimeMillis.toDouble()

/**
 * Follows the finger that came [down] until it lifts ([up]) or the system takes it away ([cancel],
 * with the time), reporting each [move].
 */
private suspend fun AwaitPointerEventScope.track(
    down: PointerInputChange,
    move: (PointerInputChange) -> Unit,
    up: (PointerInputChange) -> Unit,
    cancel: (Double) -> Unit,
) {
    var last = down.ms
    try {
        while (true) {
            val e = awaitPointerEvent()
            val c = e.changes.firstOrNull { it.id == down.id }
            if (c == null) {
                cancel(last)
                return
            }
            last = c.ms
            c.consume()
            if (!c.pressed) {
                up(c)
                return
            }
            move(c)
        }
    } catch (e: CancellationException) {
        cancel(last)
        throw e
    }
}

/**
 * The snackbar the stop leaves: "Playback stopped" and Undo, in inverse colours, sliding up and
 * fading in over .2 s once the set has collapsed; Undo hides it at once.
 */
@Composable
private fun Toast(
    state: CrtPipState,
    labels: CrtPipLabels,
    colors: CrtPipScreenColors,
    reducedMotion: Boolean,
    undoFocus: FocusRequester,
    onTouch: () -> Unit,
) {
    val on = state.toastOn
    val show = remember { Animatable(0f) }
    LaunchedEffect(on) {
        if (on) {
            if (reducedMotion) show.snapTo(1f) else show.animateTo(1f, tween(200))
        } else if (state.toastInstant || reducedMotion) show.snapTo(0f)
        else show.animateTo(0f, tween(200))
    }
    val shadow = remember { Paint() }
    val input = LocalInputModeManager.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val accent by rememberUpdatedState(colors.toastAccent)
    Box(
        Modifier.layout { measurable, constraints ->
                val side = (12 * density).roundToInt()
                val p =
                    measurable.measure(
                        Constraints(
                            minWidth = constraints.maxWidth - 2 * side,
                            maxWidth = constraints.maxWidth - 2 * side,
                        ),
                    )
                val bottom = (state.toastBottom * density).roundToInt()
                layout(constraints.maxWidth, constraints.maxHeight) {
                    p.place(side, constraints.maxHeight - bottom - p.height)
                }
            }
            .graphicsLayer {
                alpha = show.value
                translationY = (1 - show.value) * 10.dp.toPx()
            }
            .drawBehind {
                // box-shadow: 0 6px 18px -6px rgba(0,0,0,.35).
                val spread = 6.dp.toPx()
                shadow.color = Color(0f, 0f, 0f, .35f)
                shadow.setBlur(9.dp.toPx())
                val r = 12.dp.toPx()
                drawIntoCanvas {
                    it.drawRoundRect(
                        spread,
                        6.dp.toPx() + spread,
                        size.width - spread,
                        size.height + 6.dp.toPx() - spread,
                        r,
                        r,
                        shadow,
                    )
                }
            }
            .clip(RoundedCornerShape(12.dp))
            .background(colors.toast)
            .then(if (on) Modifier else Modifier.clearAndSetSemantics {})
            .padding(start = 16.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)
            .testTag("crt-toast"),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        ) {
            BasicText(labels.stopped, style = ToastStyle.copy(color = colors.toastInk))
            Box(
                Modifier.heightIn(min = 44.dp)
                    .focusRequester(undoFocus)
                    .drawBehind {
                        if (focused && input.inputMode == InputMode.Keyboard) {
                            val o = 1.dp.toPx()
                            drawRoundRect(
                                accent,
                                Offset(o, o),
                                Size(size.width - 2 * o, size.height - 2 * o),
                                CornerRadius(8.dp.toPx()),
                                style = Stroke(2.dp.toPx()),
                            )
                        }
                    }
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        enabled = on,
                        role = Role.Button,
                        interactionSource = interaction,
                        indication = null,
                    ) {
                        onTouch()
                        state.undo(keyboard = input.inputMode == InputMode.Keyboard)
                    }
                    .padding(horizontal = 12.dp)
                    .testTag("crt-undo"),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(labels.undo, style = UndoStyle.copy(color = colors.toastAccent))
            }
        }
    }
}

/** A polite live region saying what changed: the mini player, the full player, or stopped. */
@Composable
internal fun CrtPipAnnouncer(state: CrtPipState, labels: CrtPipLabels) {
    val said =
        when (state.announcement) {
            PipAnnouncement.None -> ""
            PipAnnouncement.Mini -> labels.nowMini
            PipAnnouncement.Full -> labels.nowFull
            PipAnnouncement.Stopped -> labels.stopped
        }
    Box(
        Modifier.size(1.dp).semantics {
            liveRegion = LiveRegionMode.Polite
            contentDescription = said
        },
    )
}

private val ControlShade =
    Brush.verticalGradient(
        0f to Color(0f, 0f, 0f, .42f),
        .34f to Color.Transparent,
        .7f to Color.Transparent,
        1f to Color(0f, 0f, 0f, .45f),
    )
private val TrackColor = Color(1f, 1f, 1f, .28f)

/** text-shadow: 0 1px 2px rgba(0,0,0,.6), in pixels at [density]. */
private fun textShadow(density: Float) =
    Shadow(Color(0f, 0f, 0f, .6f), Offset(0f, density), 2 * density)

private val HintStyle =
    TextStyle(
        color = Color(1f, 1f, 1f, .9f),
        fontSize = 12.sp,
        fontWeight = FontWeight(600),
        letterSpacing = .02.em,
    )
private val TimeStyle =
    TextStyle(
        color = Color.White,
        fontSize = 11.5.sp,
        fontWeight = FontWeight(600),
        fontFamily = FontFamily.Monospace,
    )
private val ToastStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight(600))
private val UndoStyle =
    TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = .02.em)

/** m:ss. */
private fun clock(s: Int) = "${s / 60}:${(s % 60).toString().padStart(2, '0')}"

/** The video's length, after the time code's slash. */
private val Length = clock(CrtPipState.Duration.toInt())
