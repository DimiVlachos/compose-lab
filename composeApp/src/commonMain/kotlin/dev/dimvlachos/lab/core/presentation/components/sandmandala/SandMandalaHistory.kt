package dev.dimvlachos.lab.core.presentation.components.sandmandala

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.dimvlachos.lab.core.platform.Sound
import dev.dimvlachos.lab.core.platform.SoundPlayer
import dev.dimvlachos.lab.core.presentation.ui.SandMandalaScreenColors
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/** The clips the mandala plays. */
internal val SandMandalaSounds =
    setOf(Sound.SandHiss, Sound.SandBowl, Sound.SandSettle, Sound.SandSweep)

/** Everything the History screen says, in the screen's language. */
internal class SandMandalaText(
    val title: String,
    val clear: String,
    val keep: String,
    val search: String,
    val hint: String,
    val clearNow: String,
    val emptyTitle: String,
    val emptyBody: String,
    val restore: String,
    val cleared: String,
    val undo: String,
    val formed: String,
    val kept: String,
    val restored: String,
    val list: String,
)

/**
 * Where the screen's controls are, in the root's dp, for the script's fingertip: the bar's Clear
 * history, the snackbar's Undo, and the list's top left.
 */
@Stable
internal class SandPlaces {
    var clear = Offset.Zero
    var undo = Offset.Zero
    var list = Offset.Zero
}

/**
 * The prototype's browser History screen, where clearing the history turns every row into coloured
 * sand that flows into a mandala; a sweep of the finger spirals it into one grey heap, which pours
 * away, and Undo brings it all back. [onTouch] is called when a real finger comes down, so a script
 * can let go.
 */
@Composable
internal fun SandMandalaHistory(
    state: SandMandalaState,
    text: SandMandalaText,
    colors: SandMandalaScreenColors,
    sounds: SoundPlayer,
    modifier: Modifier = Modifier,
    places: SandPlaces = remember { SandPlaces() },
    onTouch: () -> Unit = {},
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val measurer = rememberTextMeasurer(cacheSize = 48)
    val painter = remember { SandMandalaPainter() }
    val pigments = SandMandalaScreenColors.SiteColors

    // What to say: cleared first, as the prototype's say() does, so the same words twice are heard.
    var said by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf("") }
    var sayings by remember { mutableIntStateOf(0) }
    LaunchedEffect(sayings) {
        if (sayings == 0) return@LaunchedEffect
        said = ""
        delay(30)
        said = pending
    }
    LaunchedEffect(state, sounds, haptics, text) {
        val listener: (SandEvent) -> Unit = { event ->
            fun say(s: String) {
                pending = s
                sayings++
            }
            when (event) {
                SandEvent.Brushed -> haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                SandEvent.Formed -> {
                    sounds.play(Sound.SandBowl)
                    say(text.formed)
                }
                SandEvent.Kept -> say(text.kept)
                SandEvent.Restored -> say(text.restored)
                SandEvent.Swept -> {
                    sounds.play(Sound.SandSweep)
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                }
                SandEvent.Cleared -> {
                    sounds.play(Sound.SandSettle)
                    say(text.cleared)
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
    // The sand's hiss, its level following the state's.
    val hiss = remember(sounds) { sounds.loop(Sound.SandHiss, 0f) }
    DisposableEffect(hiss) { onDispose { hiss.stop() } }
    LaunchedEffect(state, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var last = 0L
            // Paused mid-move, the hiss stops with the loop.
            try {
                while (true) {
                    // At rest the loop sleeps, until a touch, a button or the script moves
                    // something.
                    if (state.settled) {
                        hiss.setVolume(0f)
                        val seenFrame = state.frame
                        val seenVersion = state.version
                        snapshotFlow { state.frame to state.version }
                            .first { it.first != seenFrame || it.second != seenVersion }
                        last = 0L
                    }
                    withFrameNanos { now ->
                        // The first frame moves nothing, so a stall can't jump the sand.
                        val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(.15f)
                        last = now
                        state.step(dt)
                    }
                    hiss.setVolume(state.hiss / .16f)
                }
            } finally {
                hiss.setVolume(0f)
            }
        }
    }

    // A state laid out before (a demo run again) starts at its size, so the rows draw at once.
    var listW by remember { mutableFloatStateOf(state.width) }
    var listH by remember { mutableFloatStateOf(state.height) }
    var screenH by remember { mutableFloatStateOf(state.screenH) }
    val short = screenH in 1f..639.99f
    val dark = colors === SandMandalaScreenColors.Dark
    val rowText =
        remember(listW, density.density, density.fontScale, colors, state) {
            if (listW <= 0f) null else rowText(measurer, state, colors, listW, density)
        }
    // Lays the list out, then draws the rows' words at 1 px per dp for the sand to sample.
    LaunchedEffect(listW, listH, dark, short, density.fontScale, state) {
        if (listW <= 0f || listH <= 0f) return@LaunchedEffect
        state.layout(listW, listH, dark, short, density.fontScale)
        if (state.needsText) {
            val w = listW.roundToInt()
            val h = listH.roundToInt()
            state.build(sampleText(measurer, state, colors, w, h, density.fontScale), w, h)
        }
    }

    val keepT by animateFloatAsState(if (state.keeps) 1f else 0f, softFade(state, 450))
    val clearAlpha by
        animateFloatAsState(if (state.clearEnabled) 1f else .35f, softFade(state, 400))
    val root = remember { RootHolder() }
    fun centreOf(it: LayoutCoordinates): Offset {
        val r = root.coords ?: return Offset.Zero
        if (!r.isAttached || !it.isAttached) return Offset.Zero
        val p = r.localPositionOf(it, Offset.Zero)
        return Offset(p.x + it.size.width / 2f, p.y + it.size.height / 2f) / density.density
    }
    Box(
        modifier
            .onSizeChanged {
                screenH = it.height / density.density
                state.screenH = screenH
            }
            .onGloballyPositioned { root.coords = it },
    ) {
        Column(Modifier.fillMaxSize().background(colors.background)) {
            // The bar: back, History, and Clear history (Keep history while the mandala forms).
            state.version
            Row(
                Modifier.fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .padding(start = 6.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Canvas(Modifier.size(40.dp).clearAndSetSemantics {}) {
                    chevron(colors.ink, back = true)
                }
                BasicText(
                    text.title,
                    Modifier.weight(1f).semantics { heading() },
                    style = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.Bold),
                    color = { colors.ink },
                    maxLines = 1,
                )
                Box(
                    Modifier.heightIn(min = 40.dp)
                        .graphicsLayer { alpha = clearAlpha }
                        // A text button: no ripple.
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            enabled = state.clearEnabled,
                            role = Role.Button,
                        ) {
                            onTouch()
                            state.tapClear()
                        }
                        .onGloballyPositioned { places.clear = centreOf(it) }
                        .padding(horizontal = 10.dp)
                        .semantics { testTag = "clear" },
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    // Both labels are always laid out, right-aligned, so the button keeps the
                    // longer one's width and neither label moves as they crossfade.
                    val keeps = state.keeps
                    BarLabel(text.clear, { keepT }, show = 0f, shown = !keeps, colors)
                    BarLabel(text.keep, { keepT }, show = 1f, shown = keeps, colors)
                }
            }
            if (!short) SearchField(text.search, colors)
            Box(
                Modifier.fillMaxWidth()
                    .weight(1f)
                    .onSizeChanged {
                        listW = it.width / density.density
                        listH = it.height / density.density
                    }
                    .onGloballyPositioned {
                        val r = root.coords
                        if (r != null && r.isAttached && it.isAttached)
                            places.list = r.localPositionOf(it, Offset.Zero) / density.density
                    }
                    .pointerInput(state) { brushGesture(state, onTouch) }
                    .semantics { contentDescription = text.list },
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    state.frame
                    val rt = rowText ?: return@Canvas
                    if (state.width <= 0f) return@Canvas
                    with(painter) {
                        drawRows(state, rt, colors, pigments)
                        drawChalk(state, colors)
                        drawGrains(state, colors)
                    }
                }
                RowNodes(state)
                Hint(state, text, colors, onTouch)
                Empty(state, text, colors, onTouch)
            }
            Tools(colors)
        }
        Snackbar(state, text, colors, onTouch) { places.undo = centreOf(it) }
        Box(
            Modifier.size(1.dp).semantics {
                liveRegion = LiveRegionMode.Polite
                if (said.isNotEmpty()) contentDescription = said
            },
        )
    }
}

// The finger on the list, as the prototype's pointer handlers: down, moves, and up (or cancel),
// which brush the rows or the mandala. A real touch takes over from the script.
private suspend fun PointerInputScope.brushGesture(state: SandMandalaState, onTouch: () -> Unit) {
    awaitEachGesture {
        val down = awaitFirstDown()
        onTouch()
        state.scripted = false
        state.brushDown(down.position.x / density, down.position.y / density)
        down.consume()
        var pointer = down.id
        // Cancelled mid-stroke (the list left the screen), the stroke is dropped, so the loop
        // can sleep.
        try {
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == pointer } ?: break
                if (change.positionChange() != Offset.Zero)
                    state.brushMove(change.position.x / density, change.position.y / density)
                if (!change.pressed) {
                    state.brushMove(change.position.x / density, change.position.y / density)
                    state.brushUp()
                    change.consume()
                    return@awaitEachGesture
                }
                change.consume()
                pointer = change.id
            }
            state.brushUp()
        } finally {
            state.brushCancel()
        }
    }
}

/** Screen-reader nodes for the rows while they are shown: each visit's title and its site line. */
@Composable
private fun RowNodes(state: SandMandalaState) {
    state.version
    if (!state.rowsShown || state.width <= 0f) return
    for (i in state.visits.indices) {
        val top = state.rowTop(i)
        if (top + state.rowH * .45f > state.height) continue
        val v = state.visits[i]
        Box(
            Modifier.offset(y = top.dp).fillMaxWidth().height(state.rowH.dp).semantics {
                contentDescription = "${v.title}, ${v.sub}"
            },
        )
    }
}

/** Under the finished mandala: how to clear it, and Clear now, which sweeps it for you. */
@Composable
private fun Hint(
    state: SandMandalaState,
    text: SandMandalaText,
    colors: SandMandalaScreenColors,
    onTouch: () -> Unit,
) {
    state.version
    val on = state.hintOn
    val alpha by animateFloatAsState(if (on) 1f else 0f, fade(state, 500))
    if (alpha <= 0f || state.width <= 0f) return
    Column(
        Modifier.fillMaxWidth().offset(y = state.hintTop.dp).graphicsLayer { this.alpha = alpha },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        BasicText(text.hint, style = TextStyle(fontSize = 13.sp), color = { colors.muted })
        Box(
            Modifier.heightIn(min = 32.dp)
                // A text button: no ripple.
                .clickable(
                    interactionSource = null,
                    indication = null,
                    enabled = on,
                    role = Role.Button,
                ) {
                    onTouch()
                    state.commit(1)
                }
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .semantics { testTag = "clearNow" },
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text.clearNow,
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                color = { colors.accent },
            )
        }
    }
}

/** "No history", fading in as the heap drains, and the link that brings the sample back. */
@Composable
private fun Empty(
    state: SandMandalaState,
    text: SandMandalaText,
    colors: SandMandalaScreenColors,
    onTouch: () -> Unit,
) {
    state.version
    if (state.width <= 0f) return
    val restoreAlpha by animateFloatAsState(if (state.restoreOn) 1f else 0f, fade(state, 400))
    val cleared = state.at == SandAt.Cleared
    Column(
        Modifier.fillMaxWidth()
            .offset(y = (state.height * .34f).dp)
            .padding(horizontal = 24.dp)
            .graphicsLayer { alpha = SandMandalaPainter.emptyAlpha(state).also { state.frame } }
            .then(if (cleared) Modifier else Modifier.clearAndSetSemantics {}),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Canvas(Modifier.padding(bottom = 4.dp).size(44.dp).clearAndSetSemantics {}) {
            val s = size.width / 48f
            val stroke = Stroke(1.4f * s, cap = StrokeCap.Round)
            val c = colors.muted.copy(alpha = .7f)
            drawCircle(c, 17f * s, Offset(24f * s, 24f * s), style = stroke)
            val p =
                Path().apply {
                    moveTo(24f * s, 14f * s)
                    lineTo(24f * s, 24f * s)
                    lineTo(30f * s, 28f * s)
                }
            drawPath(p, c, style = stroke)
        }
        BasicText(
            text.emptyTitle,
            style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight(650)),
            color = { colors.ink },
        )
        BasicText(text.emptyBody, style = TextStyle(fontSize = 13.5.sp), color = { colors.muted })
        Box(
            Modifier.padding(top = 8.dp)
                .graphicsLayer { alpha = restoreAlpha }
                // A text button: no ripple.
                .clickable(
                    interactionSource = null,
                    indication = null,
                    enabled = cleared && state.restoreOn,
                    role = Role.Button,
                ) {
                    onTouch()
                    state.undo()
                }
                .padding(horizontal = 10.dp, vertical = 8.dp)
                // Hidden from screen readers too until it shows, as the prototype's tabindex.
                .then(if (state.restoreOn) Modifier else Modifier.clearAndSetSemantics {})
                .semantics { testTag = "restore" },
        ) {
            BasicText(
                text.restore,
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                color = { colors.accent },
            )
        }
    }
}

/** "Browsing history cleared" with Undo, for eight seconds, rising 16 dp as it fades in. */
@Composable
private fun Snackbar(
    state: SandMandalaState,
    text: SandMandalaText,
    colors: SandMandalaScreenColors,
    onTouch: () -> Unit,
    placed: (LayoutCoordinates) -> Unit,
) {
    state.version
    val on = state.snackOn
    val rise by
        animateFloatAsState(
            if (on) 0f else 16f,
            if (state.reducedMotion) snap()
            else tween(350, easing = CubicBezierEasing(.3f, .7f, .3f, 1f)),
        )
    val alpha by animateFloatAsState(if (on) 1f else 0f, fade(state, 300))
    if (alpha <= 0f) return
    Box(
        Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp, bottom = 64.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            Modifier.fillMaxWidth()
                .graphicsLayer {
                    translationY = rise * this.density
                    this.alpha = alpha
                }
                .heightIn(min = 48.dp)
                // Its body takes a touch, so it doesn't brush the sand under it.
                .pointerInput(Unit) { awaitEachGesture { awaitFirstDown().consume() } }
                .background(colors.snack, RoundedCornerShape(12.dp))
                .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BasicText(
                text.cleared,
                Modifier.weight(1f),
                style = TextStyle(fontSize = 14.sp),
                color = { colors.snackInk },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Box(
                Modifier.heightIn(min = 40.dp)
                    // A text button: no ripple.
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        enabled = on,
                        role = Role.Button,
                    ) {
                        onTouch()
                        state.undo()
                    }
                    .onGloballyPositioned(placed)
                    .padding(horizontal = 14.dp)
                    .semantics { testTag = "undo" },
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text.undo,
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    color = { colors.snackAccent },
                )
            }
        }
    }
}

/** The search field over the list: not part of the trick, just the screen. */
@Composable
private fun SearchField(label: String, colors: SandMandalaScreenColors) {
    Row(
        Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp)
            .fillMaxWidth()
            .height(40.dp)
            .background(colors.card, RoundedCornerShape(12.dp))
            .border(1.dp, colors.line, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp)
            .clearAndSetSemantics {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Canvas(Modifier.size(18.dp)) {
            val s = size.width / 24f
            val stroke = Stroke(2f * s, cap = StrokeCap.Round)
            drawCircle(colors.muted, 6.5f * s, Offset(11f * s, 11f * s), style = stroke)
            drawLine(
                colors.muted,
                Offset(16f * s, 16f * s),
                Offset(20.5f * s, 20.5f * s),
                2f * s,
                StrokeCap.Round,
            )
        }
        BasicText(label, style = TextStyle(fontSize = 14.sp), color = { colors.muted })
    }
}

/** The browser's tool row: back, forward, new tab, four tabs, more. */
@Composable
private fun Tools(colors: SandMandalaScreenColors) {
    Row(
        Modifier.fillMaxWidth()
            .background(colors.background)
            .drawBehind { drawRect(colors.line, size = Size(size.width, 1.dp.toPx())) }
            .padding(start = 8.dp, end = 8.dp, top = 7.dp, bottom = 8.dp)
            .clearAndSetSemantics {},
    ) {
        for (k in 0 until 5) {
            Box(Modifier.weight(1f).height(40.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(22.dp)) {
                    val s = size.width / 24f
                    val stroke = Stroke(1.8f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    val c = colors.muted
                    when (k) {
                        0 -> chevron(c, back = true, unit = s)
                        1 -> chevron(c, back = false, unit = s)
                        2 -> {
                            drawLine(
                                c,
                                Offset(12f * s, 5f * s),
                                Offset(12f * s, 19f * s),
                                1.8f * s,
                                StrokeCap.Round,
                            )
                            drawLine(
                                c,
                                Offset(5f * s, 12f * s),
                                Offset(19f * s, 12f * s),
                                1.8f * s,
                                StrokeCap.Round,
                            )
                        }
                        3 -> Unit
                        else ->
                            for (y in intArrayOf(6, 12, 18)) drawCircle(
                                c,
                                1.2f * s,
                                Offset(12f * s, y * s),
                                style = stroke,
                            )
                    }
                }
                if (k == 3) TabCount(colors)
            }
        }
    }
}

@Composable
private fun TabCount(colors: SandMandalaScreenColors) {
    Box(
        Modifier.size(21.dp).border(1.8.dp, colors.muted, RoundedCornerShape(5.dp)),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            "4",
            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold),
            color = { colors.muted },
        )
    }
}

// A chevron, as the prototype's 24-unit icons: "M15 5l-7 7 7 7" back, "M9 5l7 7-7 7" forward. With
// no [unit], it is the bar's 22 dp icon centred in its 40 dp box.
private fun androidx.compose.ui.graphics.drawscope.DrawScope.chevron(
    color: Color,
    back: Boolean,
    unit: Float = 0f,
) {
    val s = if (unit > 0f) unit else 22.dp.toPx() / 24f
    val o =
        if (unit > 0f) Offset.Zero
        else Offset((size.width - 24f * s) / 2f, (size.height - 24f * s) / 2f)
    val p =
        Path().apply {
            if (back) {
                moveTo(o.x + 15f * s, o.y + 5f * s)
                lineTo(o.x + 8f * s, o.y + 12f * s)
                lineTo(o.x + 15f * s, o.y + 19f * s)
            } else {
                moveTo(o.x + 9f * s, o.y + 5f * s)
                lineTo(o.x + 16f * s, o.y + 12f * s)
                lineTo(o.x + 9f * s, o.y + 19f * s)
            }
        }
    drawPath(
        p,
        color,
        style =
            Stroke(
                (if (unit > 0f) 1.8f else 1.9f) * s,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
    )
}

private fun titleStyle(colors: SandMandalaScreenColors) =
    TextStyle(
        color = colors.ink,
        fontSize = 14.5.sp,
        fontWeight = FontWeight(550),
        lineHeight = 1.3.em,
        lineHeightStyle =
            LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
    )

private fun subStyle(colors: SandMandalaScreenColors) =
    TextStyle(
        color = colors.muted,
        fontSize = 12.sp,
        lineHeight = 1.35.em,
        lineHeightStyle =
            LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
    )

// The rows' words, laid out once for the list's width, as the painter draws them.
private fun rowText(
    measurer: TextMeasurer,
    state: SandMandalaState,
    colors: SandMandalaScreenColors,
    listW: Float,
    density: Density,
): SandRowText {
    val textW =
        ((listW - SandMandalaPainter.TextX - 20f) * density.density).roundToInt().coerceAtLeast(1)
    fun line(s: String, style: TextStyle, w: Int = textW): TextLayoutResult =
        measurer.measure(
            s,
            style,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
            maxLines = 1,
            constraints = Constraints(maxWidth = w),
            density = density,
        )
    val letter =
        TextStyle(
            color = SandMandalaScreenColors.FavInk,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    val section =
        TextStyle(
            color = colors.muted,
            fontSize = 12.5.sp,
            fontWeight = FontWeight(650),
            letterSpacing = .02.em,
            lineHeight = 1.55.em,
            lineHeightStyle =
                LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
        )
    val sections = HashMap<Int, TextLayoutResult>()
    state.entries.forEachIndexed { i, e ->
        if (e is SandEntry.Section) sections[i] = line(e.label, section, Constraints.Infinity)
    }
    return SandRowText(
        titles = state.visits.map { line(it.title, titleStyle(colors)) },
        subs = state.visits.map { line(it.sub, subStyle(colors)) },
        letters =
            state.visits.map {
                line(SandSiteInitials[it.site].toString(), letter, Constraints.Infinity)
            },
        sections = sections,
    )
}

/** One of the bar button's two labels, faded while drawing: fully shown when [t] is [show]. */
@Composable
private fun BarLabel(
    label: String,
    t: () -> Float,
    show: Float,
    shown: Boolean,
    colors: SandMandalaScreenColors,
) {
    BasicText(
        label,
        Modifier.graphicsLayer { alpha = 1f - abs(t() - show) }
            // Only the label showing is read out.
            .then(if (shown) Modifier else Modifier.clearAndSetSemantics {}),
        style = TextStyle(fontSize = 14.5.sp, fontWeight = FontWeight(650)),
        color = { colors.accent },
        maxLines = 1,
    )
}

// Eased in and out, for changes in the bar that should read as one soft move.
private fun softFade(
    state: SandMandalaState,
    ms: Int,
): androidx.compose.animation.core.FiniteAnimationSpec<Float> =
    if (state.reducedMotion) snap() else tween(ms, easing = CubicBezierEasing(.4f, 0f, .2f, 1f))

// Under reduced motion, as the prototype's CSS drops its transitions, things show at once.
private fun fade(
    state: SandMandalaState,
    ms: Int,
): androidx.compose.animation.core.AnimationSpec<Float> =
    if (state.reducedMotion) snap() else tween(ms)

/** The History screen's own layout, for placing the script's fingertip within it. */
private class RootHolder {
    var coords: LayoutCoordinates? = null
}

/** Each site's favicon letter, the first of its name. */
internal val SandSiteInitials = charArrayOf('C', 'B', 'P', 'M', 'L', 'S', 'H', 'T', 'P')

// The rows' words drawn at 1 px per dp into a [w] × [h] bitmap, read back as ARGB: their opaque
// pixels are where the cream grains start, as the prototype's text canvas.
private fun sampleText(
    measurer: TextMeasurer,
    state: SandMandalaState,
    colors: SandMandalaScreenColors,
    w: Int,
    h: Int,
    fontScale: Float,
): IntArray {
    val one = Density(1f, fontScale)
    val rows = rowText(measurer, state, colors, w.toFloat(), one)
    val bitmap = ImageBitmap(w, h)
    val canvas = androidx.compose.ui.graphics.Canvas(bitmap)
    val painter = SandMandalaPainter()
    CanvasDrawScope().draw(one, LayoutDirection.Ltr, canvas, Size(w.toFloat(), h.toFloat())) {
        for (i in state.visits.indices) {
            if (state.rowTop(i) + state.rowH * .45f >= state.height) continue
            drawText(
                rows.titles[i],
                topLeft = Offset(SandMandalaPainter.TextX, painter.titleTop(state, i)),
            )
            drawText(
                rows.subs[i],
                topLeft = Offset(SandMandalaPainter.TextX, painter.subTop(state, i)),
            )
        }
    }
    return IntArray(w * h).also { bitmap.readPixels(it) }
}
