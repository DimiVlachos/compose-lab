package dev.dimvlachos.lab.core.presentation.components.pencilcup

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import dev.dimvlachos.lab.core.platform.Sound
import dev.dimvlachos.lab.core.platform.SoundPlayer
import dev.dimvlachos.lab.core.platform.playVaried
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.canvas2d.setBlur
import dev.dimvlachos.lab.core.presentation.ui.PencilCupScreenColors
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.archivo_extrabold
import dev.dimvlachos.lab.resources.jetbrains_mono_semibold
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.Font

/** The clips the pencil cup plays. */
internal val PencilCupSounds = setOf(Sound.PencilClink, Sound.PencilLift, Sound.PencilPick)

/**
 * A speed-dial button that is a cobalt-glazed pencil cup in the bottom-right corner, laid over the
 * screen it fills, as the prototype draws it. Tap it and its tools leap out in a fan, each with its
 * label from [labels] (by tool, as [CupTools]); tap one, or press the cup, slide onto one and let
 * go, and it flies up into the [sheet] the host shows while [PencilCupState.sheetOpen], as the rest
 * fall back into the cup. The list under it dims while the fan or a sheet is out.
 *
 * Layers, bottom to top: the dimming, the cup and its tools, the cup's touch target and the labels,
 * the [sheet], and the tool in flight, so it is seen going into the sheet. [cupLabel] names the cup
 * for screen readers. [onTouch] hears a real finger come down on the cup, a label or the dimming.
 */
@Composable
internal fun PencilCupFab(
    state: PencilCupState,
    labels: List<String>,
    cupLabel: String,
    colors: PencilCupScreenColors,
    dark: Boolean,
    sounds: SoundPlayer,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false,
    onTouch: () -> Unit = {},
    cupFocus: FocusRequester = remember { FocusRequester() },
    sheet: @Composable BoxScope.() -> Unit = {},
) {
    SideEffect { state.reducedMotion = reducedMotion }
    val haptics = LocalHapticFeedback.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(state, sounds, haptics) {
        val listener: (CupEvent, Float, Int) -> Unit = { event, strength, tool ->
            when (event) {
                // The prototype's lift sits higher for each tool further forward.
                CupEvent.Lift -> sounds.playVaried(Sound.PencilLift, .7f, 1f + tool * .12f)
                CupEvent.Pick -> {
                    sounds.playVaried(Sound.PencilPick, .8f)
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                }
                CupEvent.Clink -> {
                    // Each tool rings a little higher on the glaze, as the prototype's + i·300 Hz.
                    sounds.playVaried(Sound.PencilClink, strength, 1f + tool * .125f)
                    if (strength > .3f) {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                    }
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
    // Felt: the fan opening, and each tool a drag lights.
    LaunchedEffect(state, haptics) {
        snapshotFlow { state.isOpen }
            .drop(1)
            .filter { it }
            .collect { haptics.performHapticFeedback(HapticFeedbackType.ContextClick) }
    }
    LaunchedEffect(state, haptics) {
        snapshotFlow { state.hover }
            .drop(1)
            .filter { it >= 0 }
            .collect { haptics.performHapticFeedback(HapticFeedbackType.SegmentTick) }
    }
    LaunchedEffect(state, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var last = 0L
            while (true) {
                // At rest the loop sleeps, until a tap or a timer sets the cup moving again.
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
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = state.isOpen,
        onBackCompleted = { state.close() },
    )

    val mono = Font(Res.font.jetbrains_mono_semibold, FontWeight.SemiBold)
    val sans = Font(Res.font.archivo_extrabold, FontWeight.ExtraBold)
    val fonts =
        remember(mono, sans) { mapOf(MonoFont to FontFamily(mono), SansFont to FontFamily(sans)) }
    val measurer = rememberTextMeasurer()
    val itemFocus = remember { List(CupTools.count) { FocusRequester() } }
    // Opened from the keyboard: the first label takes focus once it can, as the prototype's does.
    var focusMenu by remember { mutableStateOf(false) }
    LaunchedEffect(focusMenu) {
        if (!focusMenu) return@LaunchedEffect
        withFrameNanos {}
        runCatching { itemFocus[CupTools.menuOrder[0]].requestFocus() }
        focusMenu = false
    }

    Box(
        // The cup is laid out from the space it fills, before anything in it is placed.
        modifier.layout { measurable, constraints ->
            state.layout(constraints.maxWidth.toDp().value, constraints.maxHeight.toDp().value)
            val p = measurable.measure(constraints)
            layout(p.width, p.height) { p.place(0, 0) }
        },
    ) {
        val dimmed = state.isOpen || state.sheetOpen
        val scrim by
            animateFloatAsState(
                if (dimmed) 1f else 0f,
                if (reducedMotion) snap() else tween(250),
                label = "scrim",
            )
        Box(
            Modifier.fillMaxSize()
                .drawBehind { drawRect(colors.scrim.copy(alpha = colors.scrim.alpha * scrim)) }
                .clearAndSetSemantics {}
                .then(
                    if (dimmed) {
                        Modifier.pointerInput(state) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                down.consume()
                                onTouch()
                                var at = down.position
                                var lifted = false
                                while (true) {
                                    val e = awaitPointerEvent()
                                    val c = e.changes.firstOrNull { it.id == down.id } ?: break
                                    at = c.position
                                    c.consume()
                                    if (!c.pressed) {
                                        lifted = true
                                        break
                                    }
                                }
                                // A gesture the system took away is no tap.
                                if (lifted) state.tapScrim(at.x / density, at.y / density)
                            }
                        }
                    } else Modifier,
                ),
        )
        Canvas(Modifier.fillMaxSize()) {
            state.frame
            drawIntoCanvas { canvas ->
                val c = Canvas2D(canvas, size.width.toInt(), size.height.toInt())
                c.textMeasurer = measurer
                c.fonts = fonts
                c.scale(density, density)
                c.drawPencilCup(state, dark, density)
            }
        }
        CupButton(state, cupLabel, colors, cupFocus, { focusMenu = true }, onTouch)
        for (i in CupTools.menuOrder) {
            ToolLabel(state, i, labels[i], colors, reducedMotion, itemFocus, cupFocus, onTouch)
        }
        sheet()
        Canvas(Modifier.fillMaxSize()) {
            state.frame
            drawIntoCanvas { canvas ->
                val c = Canvas2D(canvas, size.width.toInt(), size.height.toInt())
                c.textMeasurer = measurer
                c.fonts = fonts
                c.scale(density, density)
                c.drawPencilCupFlight(state, dark, density)
            }
        }
    }
}

/**
 * The cup's touch target: a tap toggles the fan, a drag opens it and lights the tool under the
 * finger, and the release picks it. A button for screen readers, expanded or collapsed, and for
 * keys: Enter or Space toggles and moves to the first label, Escape closes.
 */
@Composable
private fun CupButton(
    state: PencilCupState,
    label: String,
    colors: PencilCupScreenColors,
    focus: FocusRequester,
    onKeyOpen: () -> Unit,
    onTouch: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val input = LocalInputModeManager.current
    fun toggleByKey() {
        state.toggle()
        if (state.isOpen) onKeyOpen()
    }
    Box(
        // Placed from the cup's geometry as it is laid out, so it follows the cup when it moves.
        Modifier.layout { measurable, constraints ->
                state.geometry
                val p =
                    measurable.measure(
                        Constraints.fixed(
                            (state.cupWidth * density).roundToInt(),
                            (state.cupHeight * density).roundToInt(),
                        ),
                    )
                layout(constraints.maxWidth, constraints.maxHeight) {
                    p.place(
                        (state.cupLeft * density).roundToInt(),
                        (state.cupTop * density).roundToInt(),
                    )
                }
            }
            .testTag("pencil-cup")
            .focusRequester(focus)
            .onKeyEvent {
                if (it.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (it.key) {
                    Key.Enter,
                    Key.NumPadEnter,
                    Key.Spacebar -> {
                        toggleByKey()
                        true
                    }
                    Key.Escape ->
                        if (state.isOpen) {
                            state.close()
                            true
                        } else false
                    else -> false
                }
            }
            .focusable(interactionSource = interaction)
            .drawBehind {
                // The keyboard's focus ring, never after a touch.
                if (focused && input.inputMode == InputMode.Keyboard) {
                    val o = 2.dp.toPx()
                    drawRoundRect(
                        colors.accent,
                        Offset(-o - 1.dp.toPx(), -o - 1.dp.toPx()),
                        androidx.compose.ui.geometry.Size(
                            size.width + 2 * (o + 1.dp.toPx()),
                            size.height + 2 * (o + 1.dp.toPx()),
                        ),
                        CornerRadius(24.dp.toPx()),
                        style = Stroke(2.dp.toPx()),
                    )
                }
            }
            .semantics {
                contentDescription = label
                role = Role.Button
                onClick {
                    state.toggle()
                    true
                }
                if (state.isOpen) {
                    collapse {
                        state.close()
                        true
                    }
                } else {
                    expand {
                        state.open()
                        true
                    }
                }
            }
            .pointerInput(state) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    onTouch()
                    fun x(o: Offset) = state.cupLeft + o.x / density
                    fun y(o: Offset) = state.cupTop + o.y / density
                    state.down(x(down.position), y(down.position))
                    try {
                        while (true) {
                            val e = awaitPointerEvent(PointerEventPass.Main)
                            val c = e.changes.firstOrNull { it.id == down.id }
                            if (c == null) {
                                state.cancel()
                                break
                            }
                            c.consume()
                            if (!c.pressed) {
                                state.up(x(c.position), y(c.position))
                                break
                            }
                            state.move(x(c.position), y(c.position))
                        }
                    } catch (e: CancellationException) {
                        state.cancel()
                        throw e
                    }
                }
            },
    )
}

/**
 * One tool's label, a menu item hung off its tip: it fades and grows in once its tool is out, and
 * lights in the accent while a drag is over its tool. Tapping it picks the tool.
 */
@Composable
private fun ToolLabel(
    state: PencilCupState,
    tool: Int,
    text: String,
    colors: PencilCupScreenColors,
    reducedMotion: Boolean,
    items: List<FocusRequester>,
    cup: FocusRequester,
    onTouch: () -> Unit,
) {
    val shown = state.labelShown(tool)
    val hot by remember(state, tool) { derivedStateOf { state.hover == tool } }
    val input = LocalInputModeManager.current
    val show by
        animateFloatAsState(
            if (shown) 1f else 0f,
            if (reducedMotion) snap() else tween(180),
            label = "label",
        )
    val fill by
        animateColorAsState(
            if (hot) colors.accent else colors.card,
            if (reducedMotion) snap() else tween(120),
            label = "fill",
        )
    val ink by
        animateColorAsState(
            if (hot) colors.accentInk else colors.ink,
            if (reducedMotion) snap() else tween(120),
            label = "ink",
        )
    val heat by
        animateFloatAsState(
            if (hot) 1f else 0f,
            if (reducedMotion) snap() else tween(120),
            label = "heat",
        )
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val paint = remember { Paint() }
    val ring = remember { Path() }
    val order = CupTools.menuOrder
    val at = order.indexOf(tool)
    Box(
        Modifier.layout { measurable, constraints ->
                val p = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                state.setLabelSize(tool, p.width / density, p.height / density)
                state.geometry
                layout(constraints.maxWidth, constraints.maxHeight) {
                    p.place(
                        (state.labelRight(tool) * density).toInt() - p.width,
                        (state.labelY(tool) * density).toInt() - p.height / 2,
                    )
                }
            }
            .testTag("pencil-label-$tool")
            .graphicsLayer {
                alpha = show
                val s = .9f + .1f * show
                scaleX = s
                scaleY = s
                transformOrigin = TransformOrigin(1f, .5f)
            }
            .drawBehind {
                val r = size.height / 2
                // box-shadow: 0 4px 14px rgba(30,20,10,.2), lit 0 6px 18px rgba(30,50,120,.3).
                val dy = (4 + 2 * heat).dp.toPx()
                val blur = (14 + 4 * heat).dp.toPx()
                paint.color =
                    androidx.compose.ui.graphics.lerp(
                        Color(30, 20, 10, 51),
                        Color(30, 50, 120, 77),
                        heat,
                    )
                paint.setBlur(blur / 2)
                drawIntoCanvas {
                    it.drawRoundRect(0f, dy, size.width, size.height + dy, r, r, paint)
                }
                drawRoundRect(fill, cornerRadius = CornerRadius(r))
                // And its 1 px ring of the line colour, which the lit label's shadow replaces.
                if (heat < 1f) {
                    ring.reset()
                    val w = 1.dp.toPx()
                    ring.addRoundRect(
                        RoundRect(-w, -w, size.width + w, size.height + w, CornerRadius(r + w)),
                    )
                    drawPath(
                        ring,
                        colors.line.copy(alpha = colors.line.alpha * (1f - heat)),
                        style = Stroke(2 * w),
                    )
                }
                if (focused && input.inputMode == InputMode.Keyboard) {
                    val o = 3.dp.toPx()
                    drawRoundRect(
                        colors.accent,
                        Offset(-o, -o),
                        androidx.compose.ui.geometry.Size(size.width + 2 * o, size.height + 2 * o),
                        CornerRadius(r + o),
                        style = Stroke(2.dp.toPx()),
                    )
                }
            }
            .then(
                if (shown) {
                    Modifier.pointerInput(state, tool) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                down.consume()
                                onTouch()
                                while (true) {
                                    val e = awaitPointerEvent()
                                    val c = e.changes.firstOrNull { it.id == down.id } ?: break
                                    c.consume()
                                    if (!c.pressed) {
                                        // Only a release still on the label picks its tool.
                                        val q = c.position
                                        val on =
                                            q.x >= 0f &&
                                                q.y >= 0f &&
                                                q.x <= size.width &&
                                                q.y <= size.height
                                        if (on) state.pick(tool)
                                        break
                                    }
                                }
                            }
                        }
                        .semantics(mergeDescendants = true) {
                            role = Role.Button
                            onClick {
                                state.pick(tool)
                                true
                            }
                        }
                } else Modifier.clearAndSetSemantics {},
            )
            .focusRequester(items[tool])
            .onKeyEvent {
                if (it.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (it.key) {
                    Key.DirectionDown -> {
                        runCatching { items[order[(at + 1) % order.size]].requestFocus() }
                        true
                    }
                    Key.DirectionUp -> {
                        runCatching {
                            items[order[(at + order.size - 1) % order.size]].requestFocus()
                        }
                        true
                    }
                    Key.Enter,
                    Key.NumPadEnter,
                    Key.Spacebar -> {
                        state.pick(tool)
                        true
                    }
                    Key.Escape -> {
                        state.close()
                        runCatching { cup.requestFocus() }
                        true
                    }
                    else -> false
                }
            }
            // Reachable by keys as soon as the fan is out, before its label has faded in.
            .focusable(enabled = state.isOpen, interactionSource = interaction)
            .padding(horizontal = 13.dp, vertical = 7.dp),
    ) {
        BasicText(
            text,
            style = LabelStyle,
            color = ColorProducer { ink },
            maxLines = 1,
            softWrap = false,
        )
    }
}

/** The label's type: the system UI font at 600, 13.5 px, as the prototype's `.item`. */
private val LabelStyle = TextStyle(fontSize = 13.5.sp, fontWeight = FontWeight(600))
