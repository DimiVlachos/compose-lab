package dev.dimvlachos.lab.core.presentation.components.coinsorter

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
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
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
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
import dev.dimvlachos.lab.core.presentation.canvas2d.setBlur
import dev.dimvlachos.lab.core.presentation.components.perspective.Homography
import dev.dimvlachos.lab.core.presentation.ui.CoinSorterScreenColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first

/** The clips the sorter plays. */
internal val CoinSorterSounds =
    setOf(
        Sound.CoinClink,
        Sound.CoinRattle,
        Sound.CoinTick,
        Sound.CoinTip,
        Sound.CoinOut,
        Sound.CoinThunk,
    )

/** Stands in for a sort's name in [CoinSorterText]'s lines. */
internal const val SortSlot = "\u0001"

/**
 * What the sorter says: each sort's [labels] (in [CoinSort] order), the chip's [sorting] while one
 * runs, the toolbar's [count], and with [SortSlot] for the sort's name the chip's [chipLabel] for
 * screen readers and what is announced as one is [sortingBy], has [sortedBy] or is [already] the
 * sort. [menu] names the menu and [list] the results.
 */
internal class CoinSorterText(
    val labels: List<String>,
    val sorting: String,
    val count: String,
    val chipLabel: String,
    val sortingBy: String,
    val sortedBy: String,
    val already: String,
    val menu: String,
    val list: String,
)

/**
 * A search results list whose sort control is a chrome coin-sorting tray under the toolbar, as the
 * prototype draws it. Pick a sort from the chip's menu (tap it, or the tray, or press it and slide
 * onto an option) and every row tips up into a coin sized by its rank in the new sort, rattles into
 * the funnel, rolls down the rail and drops through the first slot it fits; then each falls out of
 * its tube and opens back into a row on its new line.
 *
 * [header] is the screen above the toolbar (its bar and search field); [row] draws result i's
 * content inside its card, and [rowLabel] reads it out. The list's order is committed for screen
 * readers when the last row lands.
 */
@Composable
internal fun CoinSorterResults(
    state: CoinSorterState,
    text: CoinSorterText,
    colors: CoinSorterScreenColors,
    sounds: SoundPlayer,
    modifier: Modifier = Modifier,
    rowLabel: (Int) -> String,
    header: @Composable () -> Unit,
    row: @Composable (Int) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var said by remember { mutableStateOf("") }
    LaunchedEffect(state, sounds, haptics, text) {
        val listener: (CoinEvent, Float, Int) -> Unit = { event, v, i ->
            val name = { text.labels[i] }
            when (event) {
                CoinEvent.Tip -> sounds.playVaried(Sound.CoinTip, 1f, (900f + i * 40) / 900f)
                CoinEvent.Rattle ->
                    sounds.playVaried(Sound.CoinRattle, 1f, (3000f + i * 150) / 3000f)
                CoinEvent.Tick,
                CoinEvent.MenuTick ->
                    sounds.playVaried(Sound.CoinTick, v, (2600f + i * 120) / 2600f)
                CoinEvent.Clink -> {
                    sounds.playVaried(Sound.CoinClink, v, (4300f - i * 230) / 4300f)
                    if (v > .5f)
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                }
                CoinEvent.Out -> sounds.playVaried(Sound.CoinOut)
                CoinEvent.Thunk -> sounds.playVaried(Sound.CoinThunk)
                CoinEvent.MenuOpen,
                CoinEvent.MenuClose ->
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                CoinEvent.Hover -> haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                CoinEvent.Pick -> haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                CoinEvent.Sorting -> said = text.sortingBy.replace(SortSlot, name())
                CoinEvent.Sorted -> said = text.sortedBy.replace(SortSlot, name())
                CoinEvent.Already -> said = text.already.replace(SortSlot, name())
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
                // At rest the loop sleeps, until a pick or the script moves something.
                if (state.settled) {
                    val seen = state.frame
                    snapshotFlow { state.frame }.first { it != seen }
                    last = 0L
                }
                withFrameNanos { now ->
                    // The first frame moves nothing; a stall can't throw the coins about.
                    val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(.15f)
                    last = now
                    state.step(dt)
                }
            }
        }
    }

    val density = LocalDensity.current.density
    val measurer = rememberTextMeasurer(cacheSize = 16)
    val painter = remember(density) { CoinSorterPainter(density) }
    var boxW by remember { mutableFloatStateOf(0f) }
    var boxH by remember { mutableFloatStateOf(0f) }
    var headH by remember { mutableFloatStateOf(0f) }
    fun relayout() {
        if (boxW > 0f && headH > 0f) state.layout(boxW / density, boxH / density, headH / density)
    }
    var root by remember { mutableStateOf<LayoutCoordinates?>(null) }

    fun DrawScope.tray(front: Boolean) {
        state.listVersion
        if (state.width <= 0f) return
        drawIntoCanvas { canvas ->
            val c = Canvas2D(canvas, size.width.toInt(), size.height.toInt())
            if (front) painter.drawFront(c, state, colors.dark)
            else painter.drawBack(c, state, colors.dark)
        }
    }

    Box(
        modifier
            .onSizeChanged {
                boxW = it.width.toFloat()
                boxH = it.height.toFloat()
                relayout()
            }
            .onPlaced { root = it },
    ) {
        Box(
            Modifier.fillMaxWidth().onSizeChanged {
                headH = it.height.toFloat()
                relayout()
            },
        ) {
            header()
        }
        Canvas(Modifier.fillMaxSize()) { tray(front = false) }
        // The tray is a second, bigger way into the menu, under the toolbar so the chip keeps
        // its whole area; screen readers have the chip.
        TrayTarget(state)
        Toolbar(state, text, colors, root)
        Canvas(Modifier.fillMaxSize()) {
            state.frame
            with(painter) { drawWells(state, colors.line) }
        }
        Rows(state, colors, text, rowLabel, row)
        Canvas(Modifier.fillMaxSize()) {
            state.frame
            with(painter) { drawCoins(state, measurer, fly = false) }
        }
        Canvas(Modifier.fillMaxSize()) { tray(front = true) }
        Canvas(Modifier.fillMaxSize()) {
            state.frame
            with(painter) { drawCoins(state, measurer, fly = true) }
        }
        if (state.menuOpen) {
            // A tap anywhere off the menu closes it.
            Box(
                Modifier.fillMaxSize().testTag("catcher").pointerInput(state) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false).consume()
                        state.closeMenu()
                    }
                },
            )
        }
        Menu(state, text, colors)
        Box(
            Modifier.size(1.dp).semantics {
                liveRegion = LiveRegionMode.Polite
                if (said.isNotEmpty()) contentDescription = said
            },
        )
    }
}

// The press on the chip or the tray, as the prototype's pointer handlers: down, moves, up, in
// the screen's dp; a touch cancelled finishes where it last was.
private suspend fun PointerInputScope.press(state: CoinSorterState, origin: () -> Offset) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        down.consume()
        val o = origin()
        fun x(p: Offset) = o.x + p.x / density
        fun y(p: Offset) = o.y + p.y / density
        state.pressDown(x(down.position), y(down.position))
        var ended = false
        try {
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) {
                    change.consume()
                    ended = true
                    state.pressUp(x(change.position), y(change.position))
                    break
                }
                if (change.positionChanged()) {
                    change.consume()
                    state.pressMove(x(change.position), y(change.position))
                }
            }
        } finally {
            if (!ended) state.pressCancel()
        }
    }
}

@Composable
private fun TrayTarget(state: CoinSorterState) {
    state.listVersion
    if (state.width <= 0f) return
    Box(
        Modifier.offset(state.trayX0.dp, state.hopTop.dp)
            .size((state.trayX1 - state.trayX0).dp, (state.trayTop + state.trayH - state.hopTop).dp)
            .testTag("tray")
            .clearAndSetSemantics {}
            .pointerInput(state) { press(state) { Offset(state.trayX0, state.hopTop) } },
    )
}

@Composable
private fun Toolbar(
    state: CoinSorterState,
    text: CoinSorterText,
    colors: CoinSorterScreenColors,
    root: LayoutCoordinates?,
) {
    val density = LocalDensity.current.density
    var chipAt by remember { mutableStateOf(Offset.Zero) }
    val chipFocus = remember { FocusRequester() }
    val turn by
        animateFloatAsState(
            if (state.menuOpen) 180f else 0f,
            if (state.reducedMotion) snap() else tween(160, easing = CssEase),
            label = "chevron",
        )
    // The menu hands focus back to the chip when it is closed by a key.
    LaunchedEffect(state) { state.refocusChip = { chipFocus.requestFocus() } }
    val label = if (state.running) text.sorting else text.labels[state.sort.ordinal]
    val name = text.chipLabel.replace(SortSlot, text.labels[state.sort.ordinal])
    Row(
        Modifier.offset(y = state.toolbarTop.dp)
            .fillMaxWidth()
            .height(CoinSorterState.ToolbarH.dp)
            .padding(start = 16.dp, end = 16.dp, top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text.count,
            color = colors.muted,
            fontSize = 13.sp,
            lineHeight = 20.15.sp,
            maxLines = 1,
        )
        Row(
            Modifier.heightIn(min = CoinSorterState.ChipH.dp)
                .onPlaced { c ->
                    val r = root ?: return@onPlaced
                    val p = r.localPositionOf(c, Offset.Zero)
                    val d = c.size
                    chipAt = p / density
                    state.chipX = p.x / density
                    state.chipW = d.width / density
                }
                .background(colors.card, RoundedCornerShape(50))
                .border(1.dp, colors.line, RoundedCornerShape(50))
                .testTag("chip")
                .semantics {
                    role = Role.DropdownList
                    contentDescription = name
                    if (state.running) disabled()
                    if (state.menuOpen)
                        collapse {
                            state.closeMenu()
                            true
                        }
                    else expand { state.openMenu().also { if (it) state.focusOnOpen = true } }
                    onClick {
                        if (state.menuOpen) state.closeMenu()
                        else if (state.openMenu()) state.focusOnOpen = true
                        true
                    }
                }
                .onKeyEvent { e ->
                    val press =
                        e.key == Key.Enter || e.key == Key.Spacebar || e.key == Key.NumPadEnter
                    if (!press || e.type != KeyEventType.KeyDown) return@onKeyEvent false
                    if (state.menuOpen) state.closeMenu()
                    else if (state.openMenu()) state.focusOnOpen = true
                    true
                }
                .focusRequester(chipFocus)
                .focusable()
                .pointerInput(state) {
                    press(state) { chipAt }
                }
                .padding(start = 12.dp, end = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                label,
                color = if (state.running) colors.muted else colors.ink,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            val ink = if (state.running) colors.muted else colors.ink
            Canvas(Modifier.size(14.dp)) {
                rotate(turn) { chevron(ink) }
            }
        }
    }
}

// M7 10l5 5 5-5 in a 24 box, 2.2 wide, round.
private fun DrawScope.chevron(color: Color) {
    val s = size.width / 24f
    val p =
        Path().apply {
            moveTo(7 * s, 10 * s)
            lineTo(12 * s, 15 * s)
            lineTo(17 * s, 10 * s)
        }
    drawPath(p, color, style = Stroke(2.2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

// M5 12.5l4.2 4.2L19 7 in a 24 box, 2.4 wide, round.
private fun DrawScope.check(color: Color) {
    val s = size.width / 24f
    val p =
        Path().apply {
            moveTo(5 * s, 12.5f * s)
            lineTo(9.2f * s, 16.7f * s)
            lineTo(19 * s, 7 * s)
        }
    drawPath(p, color, style = Stroke(2.4f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/**
 * The results, each a card on its line drawn under the prototype's `translate · perspective(800px)
 * rotateX · scaleX` while it tips or opens, and not at all while it travels as a coin.
 */
@Composable
private fun Rows(
    state: CoinSorterState,
    colors: CoinSorterScreenColors,
    text: CoinSorterText,
    rowLabel: (Int) -> String,
    row: @Composable (Int) -> Unit,
) {
    state.listVersion
    if (state.width <= 0f) return
    val shape = RoundedCornerShape(14.dp)
    val order = state.order
    Box(
        Modifier.fillMaxSize().semantics {
            isTraversalGroup = true
            contentDescription = text.list
        },
    ) {
        for (i in 0 until state.n) {
            key(i) {
                val pose = remember { FloatArray(3) }
                val h = remember { FloatArray(9) }
                val m = remember { Matrix() }
                val shadow = remember {
                    Paint().apply { color = Color(30, 24, 14).copy(alpha = .2f) }
                }
                var blurred by remember { mutableStateOf(0f) }
                val label = rowLabel(i)
                Box(
                    Modifier.offset {
                            state.listVersion
                            IntOffset(
                                (state.rowX() * density).roundToInt(),
                                (state.rowY(state.rows[i].row) * density).roundToInt(),
                            )
                        }
                        .size(state.rowW().dp, state.rowH.dp)
                        .testTag("row:$i")
                        .semantics {
                            traversalIndex = order.indexOf(i).toFloat()
                            contentDescription = label
                        }
                        .drawWithContent {
                            state.frame
                            val s = state.rows[i]
                            if (!s.asRow) return@drawWithContent
                            state.rowPose(i, pose)
                            val moving = s.mode != CoinMode.Row
                            if (!moving) {
                                drawContent()
                                return@drawWithContent
                            }
                            val w = size.width
                            val hh = size.height
                            val a = pose[0] * PI.toFloat() / 180
                            val ca = cos(a)
                            val sa = sin(a)
                            val sx = pose[2]
                            val lift = pose[1] * density
                            Homography.of(
                                w / 2 - sx * w / 2,
                                hh / 2 - lift - hh / 2 * ca,
                                hh / 2 * sa,
                                sx,
                                0f,
                                0f,
                                0f,
                                ca,
                                -sa,
                                w / 2,
                                hh / 2 - lift,
                                800 * density,
                                h,
                            )
                            Homography.toMatrix(h, m)
                            withTransform({ transform(m) }) {
                                // .row.moving: 0 10px 24px at 20 %.
                                if (blurred != density) {
                                    shadow.setBlur(12 * density)
                                    blurred = density
                                }
                                drawIntoCanvas {
                                    val r = 14 * density
                                    it.drawRoundRect(
                                        0f,
                                        10 * density,
                                        w,
                                        hh + 10 * density,
                                        r,
                                        r,
                                        shadow,
                                    )
                                }
                                this@drawWithContent.drawContent()
                            }
                        }
                        .background(colors.card, shape)
                        .border(1.dp, colors.line, shape)
                        .padding(start = 8.dp, end = 14.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    row(i)
                }
            }
        }
    }
}

/** The prototype's own menu card, anchored under the chip, popping in from .94 and −4. */
@Composable
private fun Menu(state: CoinSorterState, text: CoinSorterText, colors: CoinSorterScreenColors) {
    val open = state.menuOpen
    val pop = remember { Animatable(0f) }
    LaunchedEffect(open) {
        if (open) {
            pop.snapTo(if (state.reducedMotion) 1f else 0f)
            pop.animateTo(1f, tween(160, easing = CubicBezierEasing(0f, 0f, .58f, 1f)))
        }
    }
    if (!open) return
    val requesters = remember { List(CoinSort.entries.size) { FocusRequester() } }
    LaunchedEffect(Unit) {
        if (state.focusOnOpen) {
            state.focusOnOpen = false
            requesters[state.sort.ordinal].requestFocus()
        }
    }
    val shape = RoundedCornerShape(14.dp)
    val density = LocalDensity.current.density
    val shadow =
        remember(density) {
            Paint().apply {
                color = Color(20, 14, 8).copy(alpha = .26f)
                setBlur(16 * density)
            }
        }
    Column(
        Modifier.offset(state.menuX.dp, state.menuY.dp)
            .width(CoinSorterState.MenuW.dp)
            .graphicsLayer {
                val p = pop.value
                alpha = p
                val sc = .94f + .06f * p
                scaleX = sc
                scaleY = sc
                translationY = -4 * density * (1 - p)
                transformOrigin = TransformOrigin(1f, 0f)
            }
            .drawBehind { menuShadow(colors, shadow) }
            .background(colors.card, shape)
            .testTag("menu")
            .semantics { paneTitle = text.menu }
            .onKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (e.key) {
                    Key.Escape,
                    Key.Back -> {
                        state.closeMenu()
                        state.refocusChip?.invoke()
                        true
                    }
                    Key.Tab -> {
                        state.closeMenu()
                        false
                    }
                    else -> false
                }
            }
            .padding(CoinSorterState.MenuPad.dp),
    ) {
        CoinSort.entries.forEachIndexed { i, sort ->
            val checked = state.sort == sort
            var focused by remember { mutableStateOf(false) }
            val lit = state.hot == i || focused
            Row(
                Modifier.fillMaxWidth()
                    .height(CoinSorterState.OptH.dp)
                    .background(
                        if (lit) colors.chip else Color.Transparent,
                        RoundedCornerShape(9.dp),
                    )
                    .testTag("option:${sort.name.lowercase()}")
                    .semantics {
                        role = Role.RadioButton
                        selected = checked
                        contentDescription = text.labels[i]
                        onClick {
                            state.pick(i)
                            true
                        }
                    }
                    .onFocusChanged { focused = it.isFocused }
                    .onKeyEvent { e ->
                        if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                        when (e.key) {
                            Key.DirectionDown -> {
                                requesters[(i + 1) % requesters.size].requestFocus()
                                true
                            }
                            Key.DirectionUp -> {
                                requesters[(i + requesters.size - 1) % requesters.size]
                                    .requestFocus()
                                true
                            }
                            Key.Enter,
                            Key.Spacebar,
                            Key.NumPadEnter -> {
                                state.pick(i)
                                true
                            }
                            else -> false
                        }
                    }
                    .focusRequester(requesters[i])
                    .focusable()
                    .pointerInput(state, i) {
                        // A finger picks on release over the option.
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false).consume()
                            val up = waitForUpOrCancellation()
                            if (up != null) {
                                up.consume()
                                state.pick(i)
                            }
                        }
                    }
                    .padding(start = 8.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.size(18.dp).drawBehind { if (checked) check(colors.accent) })
                Text(
                    text.labels[i],
                    color = colors.ink,
                    fontSize = 14.5.sp,
                    fontWeight = if (checked) FontWeight(650) else FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}

// The menu's 0 14px 32px shadow at 26 % and its 1 px ring in the line colour.
private fun DrawScope.menuShadow(colors: CoinSorterScreenColors, paint: Paint) {
    val r = 14 * density
    drawIntoCanvas {
        it.drawRoundRect(0f, 14 * density, size.width, size.height + 14 * density, r, r, paint)
    }
    val o = 1 * density
    drawRoundRect(
        colors.line,
        Offset(-o, -o),
        Size(size.width + 2 * o, size.height + 2 * o),
        CornerRadius(r + o),
    )
}

// CSS's `ease`.
private val CssEase = CubicBezierEasing(.25f, .1f, .25f, 1f)
