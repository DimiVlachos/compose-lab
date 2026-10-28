package dev.dimvlachos.lab.core.presentation.components.photocopier

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.scrollBy
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.verticalScrollAxisRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
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
import dev.dimvlachos.lab.core.presentation.ui.PhotocopierScreenColors
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.jetbrains_mono_semibold
import kotlin.math.roundToInt
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.Font

internal val PhotocopierSounds =
    setOf(
        Sound.CopierRise,
        Sound.CopierLidUp,
        Sound.CopierThunk,
        Sound.CopierSlap,
        Sound.CopierBeep,
        Sound.CopierHum,
        Sound.CopierPaper,
        Sound.CopierFeed,
        Sound.CopierLift,
        Sound.CopierSettle,
        Sound.CopierTick,
    )

/** Where a title goes in the [CopierWords] that name one. */
internal const val CopierTitleSlot = "%1\$s"

/**
 * The list's words: the menu's [duplicate] and [pin], the Undo bar's [duplicated] and [undo], the
 * menu's and the list's names for screen readers, a ⋯ button's label ([moreFor], with the title at
 * [CopierTitleSlot]), and what is announced ([saidDuplicated], [saidRemoved], [saidPinned]).
 */
internal class CopierWords(
    val duplicate: String,
    val pin: String,
    val duplicated: String,
    val undo: String,
    val menu: String,
    val list: String,
    val moreFor: String,
    val saidDuplicated: String,
    val saidRemoved: String,
    val saidPinned: String,
)

private fun String.withTitle(title: String) = replace(CopierTitleSlot, title)

/**
 * A list of cards whose Duplicate runs the card through a little office copier, as the prototype:
 * long-press a card (or tap its ⋯) for its menu, and Duplicate raises the copier from the foot of
 * the screen, lays the card face down on its glass, scans it and feeds a warm copy out of the slot
 * into the list right under it. While the copier still peeks, drag the copy back into the slot, or
 * tap Undo, and the rollers take it in. Pin to top moves a card to the top.
 *
 * Fills the screen it is given: [header] sits above the list, which scrolls under the copier.
 */
@Composable
internal fun PhotocopierList(
    state: PhotocopierState,
    words: CopierWords,
    colors: PhotocopierScreenColors,
    dark: Boolean,
    sounds: SoundPlayer,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false,
    header: @Composable () -> Unit = {},
) {
    SideEffect { state.reducedMotion = reducedMotion }
    val haptics = LocalHapticFeedback.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val input = LocalInputModeManager.current
    var announcement by remember { mutableStateOf("") }
    val said by rememberUpdatedState(words)
    LaunchedEffect(state, sounds, haptics) {
        state.onEvent = { e ->
            when (e) {
                CopierEvent.Rise -> sounds.playVaried(Sound.CopierRise, .8f)
                CopierEvent.LidUp -> sounds.playVaried(Sound.CopierLidUp, .7f)
                CopierEvent.Thunk -> {
                    sounds.playVaried(Sound.CopierThunk, 1f)
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                }
                CopierEvent.Slap -> sounds.playVaried(Sound.CopierSlap, .8f)
                CopierEvent.Beep -> sounds.play(Sound.CopierBeep, .6f)
                CopierEvent.Hum -> sounds.play(Sound.CopierHum, .9f)
                CopierEvent.Paper -> sounds.playVaried(Sound.CopierPaper, .8f)
                CopierEvent.Feed -> {
                    sounds.play(Sound.CopierFeed, .9f)
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                }
                CopierEvent.Lift -> sounds.playVaried(Sound.CopierLift, .6f)
                CopierEvent.Settle -> sounds.playVaried(Sound.CopierSettle, .7f)
                CopierEvent.Tick -> sounds.playVaried(Sound.CopierTick, .6f)
                CopierEvent.HeldOpen -> haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                CopierEvent.Landed -> haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                CopierEvent.SlotHot -> haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            }
        }
        state.onSay = { what, title ->
            val w = said
            announcement =
                when (what) {
                    CopierSaid.Duplicated -> w.saidDuplicated.withTitle(title)
                    CopierSaid.Removed -> w.saidRemoved
                    CopierSaid.Pinned -> w.saidPinned.withTitle(title)
                }
        }
        try {
            awaitCancellation()
        } finally {
            state.onEvent = null
            state.onSay = null
        }
    }
    LaunchedEffect(state, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var last = 0L
            while (true) {
                // At rest the loop sleeps, until a touch or a timer sets the copier moving again.
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
        isBackEnabled = state.menuFor != 0,
        onBackCompleted = { state.closeMenu() },
    )

    val painter = remember { PhotocopierPainter() }
    val mono = Font(Res.font.jetbrains_mono_semibold, FontWeight.SemiBold)
    val fonts = remember(mono) { mapOf(CopierMonoFont to FontFamily(mono)) }
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current.density
    val cardFocus = remember { HashMap<Int, FocusRequester>() }
    val dupFocus = remember { FocusRequester() }
    val pinFocus = remember { FocusRequester() }
    var focusMenu by remember { mutableStateOf(false) }
    fun focusCard(id: Int) {
        if (input.inputMode == InputMode.Keyboard) runCatching { cardFocus[id]?.requestFocus() }
    }
    LaunchedEffect(focusMenu) {
        if (!focusMenu) return@LaunchedEffect
        withFrameNanos {}
        runCatching { dupFocus.requestFocus() }
        focusMenu = false
    }

    Box(
        modifier

            // Any press outside the menu (and not on a ⋯), of any button, closes it first.
            .pointerInput(state) {
                awaitPointerEventScope {
                    while (true) {
                        val e = awaitPointerEvent(PointerEventPass.Initial)
                        if (e.type != PointerEventType.Press || state.menuFor == 0) continue
                        val at =
                            e.changes.firstOrNull { it.changedToDownIgnoreConsumed() } ?: continue
                        val x = at.position.x / density
                        val y = at.position.y / density
                        if (!state.onMenu(x, y) && !state.onMore(x, y)) state.closeMenu()
                    }
                }
            }
            .onPreviewKeyEvent {
                if (it.type == KeyEventType.KeyDown && it.key == Key.Escape && state.menuFor != 0) {
                    val id = state.menuFor
                    state.closeMenu()
                    focusCard(id)
                    true
                } else false
            },
    ) {
        Column(Modifier.fillMaxSize()) {
            header()
            val scrolling = rememberScrollableState { delta ->
                -state.scrollBy(-delta / density) * density
            }
            Box(
                Modifier.weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
                    .onPlaced {
                        // The list is the column's foot: the screen is as wide as it is and ends
                        // where it does.
                        val top = it.positionInParent().y
                        state.layout(
                            it.size.width / density,
                            (top + it.size.height) / density,
                            top / density,
                            it.size.height / density,
                        )
                    }
                    .semantics {
                        contentDescription = words.list
                        verticalScrollAxisRange =
                            ScrollAxisRange({ state.scroll }, { state.maxScroll })
                        scrollBy { _, dy ->
                            state.scrollBy(dy / density)
                            true
                        }
                    }
                    .scrollable(scrolling, Orientation.Vertical),
            ) {
                for (card in state.cards) {
                    key(card.item.id) {
                        val focus = remember { FocusRequester() }
                        cardFocus[card.item.id] = focus
                        DisposableEffect(card.item.id) {
                            onDispose { cardFocus.remove(card.item.id) }
                        }
                        CardView(
                            state,
                            card,
                            words,
                            colors,
                            dark,
                            painter,
                            focus,
                            onKeyMenu = {
                                state.openMenu(card.item.id)
                                focusMenu = true
                            },
                            onAction = { focusCard(it) },
                            onKeyMore = { focusMenu = true },
                        )
                    }
                }
            }
        }
        // The dimming while the copier is up.
        Box(
            Modifier.fillMaxSize()
                .clearAndSetSemantics {}
                .drawBehind {
                    state.frame
                    val a = state.scrim.value(state.now)
                    if (a > 0f) drawRect(colors.scrim.copy(alpha = colors.scrim.alpha * a))
                },
        )
        // The copier and every card in flight, in a layer of their own, so the light's
        // `lighter` adds to the canvas's own pixels as in the browser.
        Canvas(
            Modifier.fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .clearAndSetSemantics {},
        ) {
            state.frame
            drawIntoCanvas { canvas ->
                val c = Canvas2D(canvas, size.width.toInt(), size.height.toInt())
                c.textMeasurer = measurer
                c.fonts = fonts
                c.scale(density, density)
                painter.draw(c, state, colors, dark, density, fontScale)
            }
        }
        Snack(state, words, onUndo = { focusCard(state.undo()) })
        if (state.menuFor != 0) {
            Menu(
                state,
                words,
                colors,
                dupFocus,
                pinFocus,
                onDuplicate = {
                    val id = state.menuFor
                    state.duplicate(id)
                    focusCard(id)
                },
                onPin = {
                    val id = state.menuFor
                    state.pin(id)
                    focusCard(id)
                },
                onClose = {
                    val id = state.menuFor
                    state.closeMenu()
                    focusCard(id)
                },
            )
        }
        // What changed, for a screen reader.
        Box(
            Modifier.size(1.dp).semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = announcement
            },
        )
    }
}

private val CardShape = RoundedCornerShape(14.dp)
private val CssLines = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

/**
 * One list card, as the prototype's `.card`: placed from the state as the list lays out, and slid,
 * scaled and drawn from it each frame, so animating it neither recomposes nor re-measures.
 */
@Composable
private fun CardView(
    state: PhotocopierState,
    card: CopierCard,
    words: CopierWords,
    colors: PhotocopierScreenColors,
    dark: Boolean,
    painter: PhotocopierPainter,
    focus: FocusRequester,
    onKeyMenu: () -> Unit,
    onAction: (Int) -> Unit,
    onKeyMore: () -> Unit,
) {
    val item = card.item
    val id = item.id
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shadow = remember { Paint() }
    val speckClip = remember { Path() }
    // Chrome's dashes for a 1.5 px border: 4.5 on, 3 off.
    val density = LocalDensity.current.density
    val dash =
        remember(density) { PathEffect.dashPathEffect(floatArrayOf(4.5f * density, 3f * density)) }
    Box(
        Modifier.offset {
                val i = state.indexOf(id)
                IntOffset(
                    0,
                    ((state.cardTop(maxOf(0, i)) - state.scroll) * density).roundToInt(),
                )
            }
            .zIndex(if (state.menuFor == id) 1f else 0f)
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(PhotocopierState.CardH.dp)
            // A right click opens the menu at once, as the browser's context menu does, with no
            // hold first.
            .pointerInput(state, id) {
                awaitPointerEventScope {
                    while (true) {
                        val e = awaitPointerEvent()
                        val right =
                            e.type == PointerEventType.Press &&
                                e.buttons.isSecondaryPressed &&
                                !e.buttons.isPrimaryPressed &&
                                e.changes.all { it.type == PointerType.Mouse }
                        if (right && !state.pressing) state.openMenu(id)
                    }
                }
            }
            // Outside the card's layer, so a press is read in the list's own dp, neither scaled
            // with the pressed card nor slid with it.
            .pointerInput(state, id) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val k = density
                    // A mouse's other buttons press nothing (Android counts them as a down).
                    if (down.type == PointerType.Mouse && !currentEvent.buttons.isPrimaryPressed) {
                        return@awaitEachGesture
                    }
                    // The ⋯ takes its own presses.
                    if (state.onMoreOf(down.position.x / k, down.position.y / k)) {
                        return@awaitEachGesture
                    }
                    fun sx(o: Offset) = state.rectX() + o.x / k
                    fun sy(o: Offset) = state.rectY(id) + o.y / k
                    if (!state.cardDown(id, sx(down.position), sy(down.position))) {
                        return@awaitEachGesture
                    }
                    // While the copier peeks a copy is the finger's, not the list's: CSS's
                    // `touch-action: none` on it. Anything else lets the list scroll.
                    val feeding = state.canFeed(id)
                    if (feeding) down.consume()
                    var at = down.position
                    var ended = false
                    try {
                        while (true) {
                            val e = awaitPointerEvent()
                            val c = e.changes.firstOrNull { it.id == down.id }
                            if (c == null) {
                                state.cardCancel()
                                break
                            }
                            at = c.position
                            if (feeding) c.consume()
                            if (!c.pressed) {
                                state.cardUp(sx(at), sy(at))
                                break
                            }
                            state.cardMove(sx(at), sy(at))
                            if (!feeding) {
                                // The list took the pointer to scroll: the press ends where it was.
                                val f = awaitPointerEvent(PointerEventPass.Final)
                                if (f.changes.any { it.id == down.id && it.isConsumed }) {
                                    state.cardCancel()
                                    break
                                }
                            }
                        }
                        ended = true
                    } finally {
                        // A card that leaves mid-press (fed back by Undo) lets go of it.
                        if (!ended) state.cardCancel()
                    }
                }
            }
            .onFocusChanged { if (it.hasFocus) state.reveal(id) }
            .graphicsLayer {
                state.frame
                val now = state.now
                translationY = card.slide(now) * density
                val s = card.scale.value(now)
                scaleX = s
                scaleY = s
                alpha = if (card.ghost) 0f else 1f
            }
            .drawWithContent {
                state.frame
                drawCardBox(card, state.now, colors, focused, shadow, dash)
                drawContent()
                val sp = card.speck.value(state.now)
                if (item.isCopy && sp > 0f && !card.away) {
                    val inset = 1.dp.toPx()
                    val r = 13.dp.toPx()
                    val inner = speckClip
                    inner.reset()
                    inner.addRoundRect(
                        RoundRect(
                            inset,
                            inset,
                            size.width - inset,
                            size.height - inset,
                            CornerRadius(r),
                        ),
                    )
                    clipPath(inner) {
                        val img = painter.speck(dark, density)
                        drawImage(
                            img,
                            dstOffset = IntOffset(inset.roundToInt(), inset.roundToInt()),
                            dstSize =
                                androidx.compose.ui.unit.IntSize(
                                    (size.width - 2 * inset).roundToInt(),
                                    (size.height - 2 * inset).roundToInt(),
                                ),
                            alpha = sp,
                        )
                    }
                }
            }
            .testTag("card:${item.title}")
            .focusRequester(focus)
            .focusable(interactionSource = interaction)
            .onKeyEvent {
                if (it.type != KeyEventType.KeyDown) return@onKeyEvent false
                val opens =
                    it.key == Key.Enter ||
                        it.key == Key.NumPadEnter ||
                        it.key == Key.Spacebar ||
                        it.key == Key.Menu ||
                        (it.key == Key.F10 && it.isShiftPressed)
                if (opens) onKeyMenu()
                opens
            }
            .semantics {
                contentDescription = "${item.title}. ${item.summary}. ${item.meta}"
                onLongClick {
                    state.openMenu(id)
                    true
                }
                customActions =
                    listOf(
                        CustomAccessibilityAction(words.duplicate) {
                            state.duplicate(id)
                            true
                        },
                        CustomAccessibilityAction(words.pin) {
                            state.pin(id)
                            true
                        },
                    )
            },
    ) {
        Column(
            Modifier.padding(start = 15.dp, top = 12.dp, end = 45.dp)
                .graphicsLayer {
                    state.frame
                    alpha = if (card.away) 0f else 1f
                }
                .clearAndSetSemantics {},
        ) {
            CardLine(item.title, 14.5f, FontWeight(650), 18, colors.ink)
            CardLine(item.summary, 12.8f, FontWeight.Normal, 17, colors.muted)
            CardLine(item.meta, 11.5f, FontWeight.Medium, 16, colors.muted.copy(alpha = .85f))
        }
        MoreButton(
            state,
            card,
            words.moreFor.withTitle(item.title),
            colors,
            onKeyMore,
            Modifier.align(Alignment.TopEnd),
        )
    }
}

@Composable
private fun CardLine(text: String, size: Float, weight: FontWeight, line: Int, color: Color) {
    // The row keeps its height in dp, as the card's rows are fixed; a larger system font sets the
    // line at its own height, centred on the row and free to spill over it, rather than clipped.
    BasicText(
        text,
        Modifier.height(line.dp).wrapContentHeight(unbounded = true),
        style =
            TextStyle(
                color = color,
                fontSize = size.sp,
                fontWeight = weight,
                lineHeight = line.sp,
                lineHeightStyle = CssLines,
            ),
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
    )
}

/** The card's box: its shadows, fill and border, as `.card` and its classes paint them. */
private fun DrawScope.drawCardBox(
    card: CopierCard,
    now: Float,
    colors: PhotocopierScreenColors,
    focused: Boolean,
    shadow: Paint,
    dash: PathEffect,
) {
    val r = 14.dp.toPx()
    if (card.away) {
        // On the glass: a dashed outline holds its place.
        val w = 1.5.dp.toPx()
        drawRoundRect(
            colors.outline,
            Offset(w / 2, w / 2),
            Size(size.width - w, size.height - w),
            CornerRadius(r - w / 2),
            style = Stroke(w, pathEffect = dash),
        )
        return
    }
    val warm = card.warm.value(now)
    val lift = card.lift.value(now)
    // box-shadow's blur is twice its sigma.
    fun boxShadow(oy: Float, blur: Float, color: Color) {
        if (color.alpha <= 0f) return
        shadow.color = color
        shadow.setBlur(blur.dp.toPx() / 2)
        drawIntoCanvas {
            it.drawRoundRect(0f, oy.dp.toPx(), size.width, size.height + oy.dp.toPx(), r, r, shadow)
        }
    }
    boxShadow(10 * lift, 26 * lift, Color(30, 20, 10).copy(alpha = .16f * lift))
    boxShadow(6 * warm, 20 * warm, Color(236, 142, 64).copy(alpha = .26f * warm))
    val un = card.unaway.value(now)
    val fill = lerp(colors.card, colors.warmCard, warm)
    drawRoundRect(fill.copy(alpha = fill.alpha * (1 - un)), cornerRadius = CornerRadius(r))
    val border =
        lerp(lerp(colors.line, colors.outline, un), PhotocopierScreenColors.WarmBorder, warm)
    val bw = 1.dp.toPx()
    drawRoundRect(
        border,
        Offset(bw / 2, bw / 2),
        Size(size.width - bw, size.height - bw),
        CornerRadius(r - bw / 2),
        style = Stroke(bw),
    )
    if (focused) {
        // box-shadow: 0 0 0 2px accent, round the outside.
        val f = 2.dp.toPx()
        drawRoundRect(
            colors.accent,
            Offset(-f / 2, -f / 2),
            Size(size.width + f, size.height + f),
            CornerRadius(r + f / 2),
            style = Stroke(f),
        )
    }
}

/** The ⋯ in the card's top-right corner: a 40 dp button 4 dp in from its border. */
@Composable
private fun MoreButton(
    state: PhotocopierState,
    card: CopierCard,
    label: String,
    colors: PhotocopierScreenColors,
    onKeyOpen: () -> Unit,
    modifier: Modifier,
) {
    val id = card.item.id
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Canvas(
        modifier
            .offset(x = (-5).dp, y = 5.dp)
            .size(40.dp)
            .graphicsLayer {
                state.frame
                alpha = if (card.away) 0f else 1f
            }
            .focusable(interactionSource = interaction)
            .onKeyEvent {
                if (
                    it.type == KeyEventType.KeyDown &&
                        (it.key == Key.Enter || it.key == Key.Spacebar)
                ) {
                    state.tapMore(id)
                    if (state.menuFor == id) onKeyOpen()
                    true
                } else false
            }
            .semantics {
                role = Role.Button
                contentDescription = label
                onClick {
                    state.tapMore(id)
                    true
                }
            }
            .pointerInput(state, id) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    // Only the primary button clicks; the card opens the menu on a right click.
                    if (down.type == PointerType.Mouse && !currentEvent.buttons.isPrimaryPressed) {
                        return@awaitEachGesture
                    }
                    var tap = false
                    while (true) {
                        val e = awaitPointerEvent()
                        val c = e.changes.firstOrNull { it.id == down.id } ?: break
                        if (!c.pressed) {
                            val p = c.position
                            tap =
                                p.x in 0f..size.width.toFloat() && p.y in 0f..size.height.toFloat()
                            break
                        }
                        if (
                            (c.position - down.position).getDistance() > viewConfiguration.touchSlop
                        )
                            break
                        // The list took the pointer to scroll: no tap.
                        val f = awaitPointerEvent(PointerEventPass.Final)
                        if (f.changes.any { it.id == down.id && it.isConsumed }) break
                    }
                    if (tap) state.tapMore(id)
                }
            },
    ) {
        if (focused) {
            drawRoundRect(
                colors.accent,
                Offset(1.dp.toPx(), 1.dp.toPx()),
                Size(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
                CornerRadius(11.dp.toPx()),
                style = Stroke(2.dp.toPx()),
            )
        }
        // The 24-box dots at 6, 12 and 18, r 1.8, in a 20 dp icon centred in the button.
        val s = 20.dp.toPx() / 24f
        val ox = (size.width - 20.dp.toPx()) / 2
        val oy = (size.height - 20.dp.toPx()) / 2
        for (cx in floatArrayOf(6f, 12f, 18f)) {
            drawCircle(colors.muted, 1.8f * s, Offset(ox + cx * s, oy + 12 * s))
        }
    }
}

/** The Undo bar, 64 dp up from the foot, clear of the peeking copier's lip. */
@Composable
private fun Snack(state: PhotocopierState, words: CopierWords, onUndo: () -> Unit) {
    val density = LocalDensity.current.density
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        val shadow = remember { Paint() }
        Row(
            Modifier.padding(start = 16.dp, end = 16.dp, bottom = 64.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    state.frame
                    val a = state.snack.value(state.now)
                    alpha = a
                    translationY = (1 - a) * 10 * density
                }
                .drawBehind {
                    shadow.color = Color(0f, 0f, 0f, .28f)
                    shadow.setBlur(28.dp.toPx() / 2)
                    val r = 14.dp.toPx()
                    drawIntoCanvas {
                        it.drawRoundRect(
                            0f,
                            10.dp.toPx(),
                            size.width,
                            size.height + 10.dp.toPx(),
                            r,
                            r,
                            shadow,
                        )
                    }
                }
                .clip(RoundedCornerShape(14.dp))
                .background(PhotocopierScreenColors.SnackFill)
                .padding(start = 16.dp, top = 4.dp, end = 4.dp, bottom = 4.dp)
                .then(if (state.snackOn) Modifier else Modifier.clearAndSetSemantics {}),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BasicText(
                words.duplicated,
                Modifier.weight(1f),
                style =
                    TextStyle(
                        color = PhotocopierScreenColors.SnackInk,
                        fontSize = 14.sp,
                        lineHeight = 21.7.sp,
                        lineHeightStyle = CssLines,
                    ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val interaction = remember { MutableInteractionSource() }
            val focused by interaction.collectIsFocusedAsState()
            Box(
                Modifier.heightIn(min = 44.dp)
                    .widthIn(min = 64.dp)
                    .drawBehind {
                        if (focused) {
                            drawRoundRect(
                                PhotocopierScreenColors.UndoInk,
                                Offset(1.dp.toPx(), 1.dp.toPx()),
                                Size(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
                                CornerRadius(9.dp.toPx()),
                                style = Stroke(2.dp.toPx()),
                            )
                        }
                    }
                    .then(
                        if (state.snackOn) {
                            Modifier.focusable(interactionSource = interaction)
                                .onKeyEvent {
                                    if (
                                        it.type == KeyEventType.KeyDown &&
                                            (it.key == Key.Enter || it.key == Key.Spacebar)
                                    ) {
                                        onUndo()
                                        true
                                    } else false
                                }
                                .semantics {
                                    role = Role.Button
                                    contentDescription = words.undo
                                    onClick {
                                        onUndo()
                                        true
                                    }
                                }
                                .pointerInput(Unit) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown()
                                        down.consume()
                                        while (true) {
                                            val e = awaitPointerEvent()
                                            val c =
                                                e.changes.firstOrNull { it.id == down.id } ?: break
                                            c.consume()
                                            if (!c.pressed) {
                                                onUndo()
                                                break
                                            }
                                        }
                                    }
                                }
                        } else Modifier,
                    )
                    .testTag("undo"),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    words.undo,
                    style =
                        TextStyle(
                            color = PhotocopierScreenColors.UndoInk,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight(650),
                        ),
                )
            }
        }
    }
}

private val DupIcon =
    listOf(
        "M10.5 8h7a2.5 2.5 0 0 1 2.5 2.5v7a2.5 2.5 0 0 1-2.5 2.5h-7A2.5 2.5 0 0 1 8 17.5v-7A2.5 2.5 0 0 1 10.5 8z",
        "M16 5.5V5a1.5 1.5 0 0 0-1.5-1.5h-9A1.5 1.5 0 0 0 4 5v9a1.5 1.5 0 0 0 1.5 1.5H6",
    )
private val PinIcon = listOf("M12 15v6M8 4h8l-1 6 3 3H6l3-3z")

private fun svg(d: String): Path = PathParser().parsePathString(d).toPath()

/** The card's menu: Duplicate and Pin to top, popping in under (or over) its card. */
@Composable
private fun Menu(
    state: PhotocopierState,
    words: CopierWords,
    colors: PhotocopierScreenColors,
    dupFocus: FocusRequester,
    pinFocus: FocusRequester,
    onDuplicate: () -> Unit,
    onPin: () -> Unit,
    onClose: () -> Unit,
) {
    val density = LocalDensity.current.density
    val shadow = remember { Paint() }
    Column(
        Modifier.offset {
                IntOffset(
                    (state.menuX * density).roundToInt(),
                    (state.menuY * density).roundToInt(),
                )
            }
            .size(PhotocopierState.MenuW.dp, PhotocopierState.MenuH.dp)
            .graphicsLayer {
                state.frame
                // The pop: from 0, 4 dp up and .97, in .16 s on ease-out.
                val p = CssEaseOut.transform(((state.now - state.menuAt) / 160f).coerceIn(0f, 1f))
                alpha = p
                translationY = (1 - p) * -4 * density
                val s = .97f + .03f * p
                scaleX = s
                scaleY = s
            }
            .drawBehind {
                val r = 14.dp.toPx()
                shadow.color = Color(20, 14, 8).copy(alpha = .26f)
                shadow.setBlur(34.dp.toPx() / 2)
                drawIntoCanvas {
                    it.drawRoundRect(
                        0f,
                        14.dp.toPx(),
                        size.width,
                        size.height + 14.dp.toPx(),
                        r,
                        r,
                        shadow,
                    )
                }
                // 0 0 0 1px line: a ring just outside.
                val o = 1.dp.toPx()
                drawRoundRect(
                    colors.line,
                    Offset(-o / 2, -o / 2),
                    Size(size.width + o, size.height + o),
                    CornerRadius(r + o / 2),
                    style = Stroke(o),
                )
            }
            .clip(RoundedCornerShape(14.dp))
            .background(colors.card)
            .padding(6.dp)
            .semantics { contentDescription = words.menu }
            .testTag("menu"),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        MenuItem(words.duplicate, DupIcon, colors, dupFocus, pinFocus, onDuplicate, onClose)
        MenuItem(words.pin, PinIcon, colors, pinFocus, dupFocus, onPin, onClose)
    }
}

@Composable
private fun MenuItem(
    label: String,
    icon: List<String>,
    colors: PhotocopierScreenColors,
    focus: FocusRequester,
    other: FocusRequester,
    onChoose: () -> Unit,
    onClose: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val paths = remember(icon) { icon.map { svg(it) } }
    Row(
        Modifier.fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (focused) colors.chip else Color.Transparent)
            .focusRequester(focus)
            .focusable(interactionSource = interaction)
            .onKeyEvent {
                if (it.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (it.key) {
                    Key.DirectionDown,
                    Key.DirectionUp -> {
                        runCatching { other.requestFocus() }
                        true
                    }
                    // Escape is the screen's, which closes the menu before it gets here.
                    Key.Tab -> {
                        onClose()
                        true
                    }
                    Key.Enter,
                    Key.NumPadEnter,
                    Key.Spacebar -> {
                        onChoose()
                        true
                    }
                    else -> false
                }
            }
            .semantics {
                role = Role.Button
                contentDescription = label
                onClick {
                    onChoose()
                    true
                }
            }
            .pointerInput(onChoose) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    while (true) {
                        val e = awaitPointerEvent()
                        val c = e.changes.firstOrNull { it.id == down.id } ?: break
                        c.consume()
                        if (!c.pressed) {
                            val inside =
                                c.position.x in 0f..size.width.toFloat() &&
                                    c.position.y in 0f..size.height.toFloat()
                            if (inside) onChoose()
                            break
                        }
                    }
                }
            }
            .padding(horizontal = 12.dp)
            .testTag("menu:$label"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Canvas(Modifier.size(19.dp).clearAndSetSemantics {}) {
            val s = size.width / 24f
            scale(s, s, pivot = Offset.Zero) {
                for (p in paths) drawPath(p, colors.muted, style = IconStroke)
            }
        }
        BasicText(
            label,
            Modifier.clearAndSetSemantics {},
            style =
                TextStyle(
                    color = colors.ink,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight(550),
                ),
            maxLines = 1,
        )
    }
}

private val IconStroke = Stroke(1.9f, cap = StrokeCap.Round, join = StrokeJoin.Round)
