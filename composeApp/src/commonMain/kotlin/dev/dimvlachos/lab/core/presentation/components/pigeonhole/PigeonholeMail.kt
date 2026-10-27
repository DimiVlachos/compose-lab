package dev.dimvlachos.lab.core.presentation.components.pigeonhole

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
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
import dev.dimvlachos.lab.core.presentation.components.pigeonhole.PigeonholeState.Companion.Inbox
import dev.dimvlachos.lab.core.presentation.components.pigeonhole.PigeonholeState.Companion.ListPad
import dev.dimvlachos.lab.core.presentation.components.pigeonhole.PigeonholeState.Companion.RowH
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.core.presentation.ui.PigeonholeScreenColors
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.caveat_semibold
import dev.dimvlachos.lab.resources.jetbrains_mono_semibold
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.Font

/** The clips the rack plays. */
internal val PigeonholeSounds =
    setOf(
        Sound.PigeonholeLift,
        Sound.PigeonholeFold,
        Sound.PigeonholeSlide,
        Sound.PigeonholeTock,
        Sound.PigeonholeRack,
        Sound.PigeonholeTick,
    )

/**
 * What the mail screen says: the [inbox]'s title and its [account], the search field's [search],
 * the rack's [moveTo], [folders] and [done], the Back button's [back], the empty folder's [empty],
 * the list's [list] label and a row's [moveAction]; the announcements [choose], [moved] and
 * [notMoved] (with `%1$s` for a subject or folder), the tap [hint] and [inboxSaid]. [messages] is
 * "n messages", [opened] the folder opened, [cubby] a cubby's label.
 */
internal class PigeonholeText(
    val inbox: String,
    val account: String,
    val search: String,
    val moveTo: String,
    val folders: String,
    val done: String,
    val back: String,
    val empty: String,
    val list: String,
    val moveAction: String,
    val choose: String,
    val moved: String,
    val notMoved: String,
    val hint: String,
    val inboxSaid: String,
    val messages: @Composable (Int) -> String,
    val opened: @Composable (String, Int) -> String,
    val cubby: @Composable (String, Int, Boolean) -> String,
)

/**
 * A mail app's inbox whose Move to… is the pigeonhole rack from behind a hotel front desk, as the
 * prototype draws it. Pick up a message and the rack rises; as it nears the rack the email folds
 * into a letter, and you post it into a cubby. Let go over the list and it unfolds back into its
 * slot. The rack icon raises the rack alone: tap a cubby to open its folder, its letters unfolding
 * into rows; Back files them away. Drag a letter out of a cubby onto the list to move it back.
 *
 * [onPlacedFolders] and [onPlacedBack] say where the app bar's two buttons are, for a script.
 */
@Composable
internal fun PigeonholeMail(
    state: PigeonholeState,
    text: PigeonholeText,
    sounds: SoundPlayer,
    colors: PigeonholeScreenColors,
    modifier: Modifier = Modifier,
    onPlacedFolders: (Offset) -> Unit = {},
    onPlacedBack: (Offset) -> Unit = {},
) {
    val haptics = LocalHapticFeedback.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var said by remember { mutableStateOf<PigeonholeSay?>(null) }
    val rowFocus = remember { mutableMapOf<String, FocusRequester>() }
    val cubFocus = remember { List(6) { FocusRequester() } }
    val foldersFocus = remember { FocusRequester() }
    LaunchedEffect(state, sounds, haptics) {
        val onSound: (PigeonholeSound, Float) -> Unit = { kind, v ->
            when (kind) {
                PigeonholeSound.Lift -> {
                    sounds.playVaried(Sound.PigeonholeLift, v)
                    if (v >= 1f)
                        haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                }
                PigeonholeSound.Fold -> sounds.playVaried(Sound.PigeonholeFold, v)
                PigeonholeSound.Slide -> sounds.playVaried(Sound.PigeonholeSlide, v)
                PigeonholeSound.Tock -> {
                    sounds.playVaried(Sound.PigeonholeTock, v)
                    if (v >= 1f) haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                }
                PigeonholeSound.Rack -> {
                    sounds.playVaried(Sound.PigeonholeRack, v)
                    if (v >= 1f) haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                }
                PigeonholeSound.Tick -> {
                    sounds.playVaried(Sound.PigeonholeTick, v)
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                }
            }
        }
        val onSay: (PigeonholeSay) -> Unit = { said = it }
        val onFocus: (PigeonholeFocus) -> Unit = {
            runCatching {
                when (it) {
                    is PigeonholeFocus.Cub -> cubFocus[it.index].requestFocus()
                    is PigeonholeFocus.Row -> rowFocus[it.id]?.requestFocus()
                    PigeonholeFocus.Folders -> foldersFocus.requestFocus()
                }
            }
        }
        state.onSound = onSound
        state.onSay = onSay
        state.onFocus = onFocus
        try {
            awaitCancellation()
        } finally {
            if (state.onSound === onSound) state.onSound = null
            if (state.onSay === onSay) state.onSay = null
            if (state.onFocus === onFocus) state.onFocus = null
        }
    }
    LaunchedEffect(state, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var last = 0L
            while (true) {
                // At rest the loop sleeps, until a touch, a key or the script moves something.
                if (state.settled) {
                    val seen = state.frame
                    snapshotFlow { state.frame }.first { it != seen }
                    last = 0L
                }
                withFrameNanos { now ->
                    val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(.15f)
                    last = now
                    state.tick(dt)
                }
            }
        }
    }

    val density = LocalDensity.current.density
    val inks = if (LabTheme.isDark) LetterInks.Dusk else LetterInks.Day
    val pen = FontFamily(Font(Res.font.caveat_semibold, FontWeight.SemiBold))
    val mono = FontFamily(Font(Res.font.jetbrains_mono_semibold, FontWeight.SemiBold))
    val fonts =
        remember(pen, mono) {
            mapOf(SysFont to FontFamily.Default, MonoFont to mono, PenFont to pen)
        }
    val measurer = rememberTextMeasurer(cacheSize = 64)
    // Made again when the pen's face loads, so no letter keeps a fallback face baked in.
    val painter = remember(density, measurer, fonts) { PigeonholePainter(density, measurer, fonts) }

    var screen by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var boxW by remember { mutableFloatStateOf(0f) }
    var boxH by remember { mutableFloatStateOf(0f) }
    var listTop by remember { mutableFloatStateOf(0f) }
    var listH by remember { mutableFloatStateOf(0f) }
    fun relayout() {
        if (boxW > 0f && listH > 0f)
            state.layout(boxW / density, boxH / density, listTop / density, listH / density)
    }

    fun DrawScope.paint(rack: Boolean) {
        state.frame
        if (state.width <= 0f) return
        drawIntoCanvas { canvas ->
            val c = Canvas2D(canvas, size.width.toInt(), size.height.toInt())
            c.scale(density, density)
            if (rack) painter.drawRack(c, state, inks) else painter.drawFlying(c, state, inks)
        }
    }

    Box(
        modifier
            .background(colors.background)
            .onPlaced { screen = it }
            .onSizeChanged {
                boxW = it.width.toFloat()
                boxH = it.height.toFloat()
                relayout()
            }
            .onKeyEvent { e ->
                if (e.type == KeyEventType.KeyDown && e.key == Key.Escape) state.escape() else false
            },
    ) {
        Column(Modifier.fillMaxSize()) {
            AppBar(
                state,
                text,
                colors,
                foldersFocus,
                onPlacedFolders = { c ->
                    screen?.let { s ->
                        onPlacedFolders(s.localPositionOf(c, centerOf(c)) / density)
                    }
                },
                onPlacedBack = { c ->
                    screen?.let { s -> onPlacedBack(s.localPositionOf(c, centerOf(c)) / density) }
                },
            )
            Search(text.search, colors)
            MailList(
                state,
                text,
                colors,
                rowFocus,
                Modifier.weight(1f).fillMaxWidth().onPlaced { c ->
                    // The column fills the screen from its corner, so its place is the screen's.
                    listTop = c.positionInParent().y
                    listH = c.size.height.toFloat()
                    relayout()
                },
            ) {
                screen
            }
        }
        Canvas(Modifier.fillMaxSize()) { paint(rack = true) }
        RackUi(state, text, cubFocus, { screen })
        Canvas(Modifier.fillMaxSize()) { paint(rack = false) }
        val announcement = said?.let { announce(it, text) }.orEmpty()
        Box(
            Modifier.size(1.dp).semantics {
                liveRegion = LiveRegionMode.Polite
                if (announcement.isNotEmpty()) contentDescription = announcement
            },
        )
    }
}

private fun centerOf(c: LayoutCoordinates) = Offset(c.size.width / 2f, c.size.height / 2f)

@Composable
private fun announce(s: PigeonholeSay, text: PigeonholeText): String =
    when (s) {
        is PigeonholeSay.Choose -> text.choose.replace("%1\$s", s.subject)
        is PigeonholeSay.Moved -> text.moved.replace("%1\$s", s.folder ?: text.inbox)
        PigeonholeSay.NotMoved -> text.notMoved
        PigeonholeSay.DragHint -> text.hint
        is PigeonholeSay.Opened -> text.opened(s.folder, s.count)
        PigeonholeSay.Inbox -> text.inboxSaid
        PigeonholeSay.Folders -> text.folders
    }

// The prototype's 24-box icons, drawn at [size].
private fun DrawScope.icon(block: DrawScope.(Float) -> Unit) = block(size.width / 24f)

/** The app bar: Back (in a folder), the folder's title and count, and the rack's icon. */
@Composable
private fun AppBar(
    state: PigeonholeState,
    text: PigeonholeText,
    colors: PigeonholeScreenColors,
    foldersFocus: FocusRequester,
    onPlacedFolders: (LayoutCoordinates) -> Unit,
    onPlacedBack: (LayoutCoordinates) -> Unit,
) {
    state.listVersion
    val inFolder = state.view != Inbox
    val n = state.messages(state.view).size
    val title = if (inFolder) state.cubbies[state.cubIndex(state.view)].name else text.inbox
    val msgs = text.messages(n)
    Row(
        // In a folder, Back sits 12 into the bar's 18 margin and 6 into the title's 8 gap.
        Modifier.fillMaxWidth()
            .padding(
                start = if (inFolder) 6.dp else 18.dp,
                end = 14.dp,
                top = 18.dp,
                bottom = 6.dp,
            ),
        verticalAlignment = Alignment.Bottom,
    ) {
        if (inFolder) {
            IconButton(
                text.back,
                colors,
                Modifier.onPlaced(onPlacedBack).testTag("back"),
                onClick = {
                    state.releaseScripted()
                    state.back()
                },
            ) {
                // M15 5l-7 7 7 7
                Canvas(Modifier.size(24.dp)) {
                    icon { s ->
                        val w = 2.2f * s
                        drawLine(
                            colors.ink,
                            Offset(15 * s, 5 * s),
                            Offset(8 * s, 12 * s),
                            w,
                            StrokeCap.Round,
                        )
                        drawLine(
                            colors.ink,
                            Offset(8 * s, 12 * s),
                            Offset(15 * s, 19 * s),
                            w,
                            StrokeCap.Round,
                        )
                    }
                }
            }
        }
        Column(Modifier.weight(1f).padding(start = if (inFolder) 2.dp else 0.dp)) {
            Text(
                title,
                Modifier.semantics { heading() },
                color = colors.ink,
                fontSize = 28.sp,
                lineHeight = 1.05.em,
                fontWeight = FontWeight(750),
                letterSpacing = (-.02).em,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (inFolder) msgs else "${text.account} · $msgs",
                Modifier.padding(top = 3.dp).testTag("sub"),
                color = colors.muted,
                fontSize = 12.5.sp,
                lineHeight = 1.55.em,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
        IconButton(
            text.folders,
            colors,
            Modifier.padding(start = 8.dp)
                .focusRequester(foldersFocus)
                .onPlaced(onPlacedFolders)
                .testTag("folders")
                .semantics {
                    if (state.rackUp)
                        collapse {
                            state.toggleRack()
                            true
                        }
                    else
                        expand {
                            state.toggleRack()
                            true
                        }
                },
            onClick = {
                state.releaseScripted()
                state.toggleRack()
            },
        ) {
            Canvas(Modifier.size(24.dp)) {
                icon { s ->
                    val w = 1.8f * s
                    drawRoundRect(
                        colors.ink,
                        Offset(2.5f * s, 4 * s),
                        Size(19 * s, 16 * s),
                        CornerRadius(2 * s),
                        style = Stroke(w),
                    )
                    drawLine(colors.ink, Offset(2.5f * s, 12 * s), Offset(21.5f * s, 12 * s), w)
                    drawLine(colors.ink, Offset(8.8f * s, 4 * s), Offset(8.8f * s, 20 * s), w)
                    drawLine(colors.ink, Offset(15.2f * s, 4 * s), Offset(15.2f * s, 20 * s), w)
                    for (x in floatArrayOf(4.6f, 11f, 17.3f)) {
                        for (y in floatArrayOf(9.5f, 17.5f)) {
                            drawLine(
                                colors.ink,
                                Offset(x * s, y * s),
                                Offset((x + 2.2f) * s, y * s),
                                1.6f * s,
                                StrokeCap.Round,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** The prototype's `.iconbtn`: 44 square, its 24 icon centred, a focus ring inside it. */
@Composable
private fun IconButton(
    label: String,
    colors: PigeonholeScreenColors,
    modifier: Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .onFocusChanged { focused = it.isFocused }
            .clickable(remember { MutableInteractionSource() }, null, role = Role.Button) {
                onClick()
            }
            .semantics { contentDescription = label }
            .drawBehind { if (focused) ring(12.dp.toPx(), inset = true, color = colors.accent) },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** The prototype's `.search`: a quiet field under the bar. */
@Composable
private fun Search(label: String, colors: PigeonholeScreenColors) {
    Row(
        Modifier.fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 10.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.chip)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Canvas(Modifier.size(16.dp)) {
            icon { s ->
                drawCircle(colors.muted, 6.5f * s, Offset(11 * s, 11 * s), style = Stroke(2 * s))
                drawLine(
                    colors.muted,
                    Offset(16 * s, 16 * s),
                    Offset(20 * s, 20 * s),
                    2 * s,
                    StrokeCap.Round,
                )
            }
        }
        Text(label, color = colors.muted, fontSize = 14.sp, lineHeight = 1.55.em, maxLines = 1)
    }
}

private val Line19 =
    TextStyle(
        lineHeight = 19.sp,
        lineHeightStyle =
            LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
    )

/**
 * The list: rows at the state's slots, scrolled and gliding as the state says; or the empty note.
 */
@Composable
private fun MailList(
    state: PigeonholeState,
    text: PigeonholeText,
    colors: PigeonholeScreenColors,
    rowFocus: MutableMap<String, FocusRequester>,
    modifier: Modifier,
    screen: () -> LayoutCoordinates?,
) {
    val density = LocalDensity.current.density
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollableState { px -> state.scrollBy(px / density) * density }
    val fling = ScrollableDefaults.flingBehavior()
    var flinging by remember { mutableStateOf<Job?>(null) }
    var here by remember { mutableStateOf<LayoutCoordinates?>(null) }
    state.listVersion
    Box(
        modifier
            .clipToBounds()
            .onPlaced { here = it }
            .graphicsLayer {
                state.frame
                alpha = state.listAlpha()
            }
            .semantics {
                isTraversalGroup = true
                contentDescription = text.list
            }
            .pointerInput(state) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    flinging?.cancel()
                    state.releaseScripted()
                    val s = screen() ?: return@awaitEachGesture
                    val me = here ?: return@awaitEachGesture
                    fun at(c: PointerInputChange) = s.localPositionOf(me, c.position) / density
                    val p0 = at(down)
                    val id = state.rowAt(p0.x, p0.y) ?: return@awaitEachGesture
                    val touch = down.type == PointerType.Touch || down.type == PointerType.Stylus
                    state.rowDown(id, p0.x, p0.y, touch, down.uptimeMillis.toDouble())
                    val tracker = VelocityTracker()
                    var last = down.position
                    var ended = false
                    try {
                        while (true) {
                            val e = awaitPointerEvent()
                            val c = e.changes.firstOrNull { it.id == down.id } ?: break
                            if (!c.pressed) {
                                ended = true
                                if (state.scrolling) {
                                    val v = tracker.calculateVelocity().y
                                    flinging = scope.launch {
                                        scroll.scroll { with(fling) { performFling(-v) } }
                                    }
                                }
                                val p = at(c)
                                state.ptrUp(p.x, p.y)
                                break
                            }
                            if (c.positionChanged()) {
                                if (state.scrolling) {
                                    tracker.addPosition(c.uptimeMillis, c.position)
                                    scroll.dispatchRawDelta(-(c.position.y - last.y))
                                } else {
                                    val p = at(c)
                                    state.ptrMove(p.x, p.y, c.uptimeMillis.toDouble())
                                    if (state.scrolling)
                                        tracker.addPosition(c.uptimeMillis, c.position)
                                }
                                last = c.position
                                c.consume()
                            }
                        }
                    } finally {
                        if (!ended && !state.scriptedTouch) state.ptrCancel()
                    }
                }
            },
    ) {
        val rows = state.rows.toList()
        for (m in rows) {
            key(m.id) {
                val requester = remember { FocusRequester() }
                DisposableEffect(m.id) {
                    rowFocus[m.id] = requester
                    onDispose { if (rowFocus[m.id] === requester) rowFocus.remove(m.id) }
                }
                MailRow(state, m, text, colors, requester)
            }
        }
        if (rows.isEmpty()) {
            Text(
                text.empty,
                Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 40.dp),
                color = colors.muted,
                fontSize = 14.sp,
                lineHeight = 1.55.em,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

/** A row: the prototype's `.mail` card, sender and time, subject, and the preview's first line. */
@Composable
private fun MailRow(
    state: PigeonholeState,
    m: Mail,
    text: PigeonholeText,
    colors: PigeonholeScreenColors,
    requester: FocusRequester,
) {
    val hidden = state.isHidden(m.id)
    val pressed = state.pressed == m.id
    val bg by
        animateColorAsState(
            if (pressed) lerp(colors.card, colors.ink, .12f) else colors.card,
            tween(120),
            label = "press",
        )
    var focused by remember { mutableStateOf(false) }
    val item = m.item
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier.offset {
                state.frame
                val i = state.rows.indexOf(m)
                val y = state.rowTop(i) - state.scrollTop + state.rowShift(m.id)
                IntOffset((ListPad * density).roundToInt(), (y * density).roundToInt())
            }
            .graphicsLayer {
                state.frame
                alpha = if (state.isHidden(m.id)) 0f else state.rowAlpha()
            }
            .size(state.rowW.dp, RowH.dp)
            .testTag("row:${m.id}")
            .drawBehind {
                if (focused) {
                    val w = 2.dp.toPx()
                    val out = 2.dp.toPx() + w / 2
                    drawRoundRect(
                        colors.accent,
                        Offset(-out, -out),
                        Size(size.width + 2 * out, size.height + 2 * out),
                        CornerRadius(14.dp.toPx() + out),
                        style = Stroke(w),
                    )
                }
            }
            .clip(shape)
            .background(bg)
            .border(1.dp, colors.line, shape)
            .then(
                if (hidden) Modifier.clearAndSetSemantics {}
                else
                    Modifier.semantics(mergeDescendants = true) {
                        contentDescription = "${item.from}, ${item.subj}, ${item.time}"
                        customActions =
                            listOf(
                                CustomAccessibilityAction(text.moveAction) {
                                    state.kbPick(m.id)
                                    true
                                },
                            )
                    },
            )
            .onFocusChanged { focused = it.isFocused }
            .onKeyEvent { e ->
                val press = e.key == Key.Enter || e.key == Key.Spacebar || e.key == Key.NumPadEnter
                if (!press || e.type != KeyEventType.KeyDown) return@onKeyEvent false
                state.releaseScripted()
                state.kbPick(m.id)
                true
            }
            .focusRequester(requester)
            .focusable(enabled = !hidden)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Column(Modifier.padding(vertical = 1.dp).fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().height(19.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    item.from,
                    Modifier.weight(1f).alignByBaseline(),
                    color = colors.ink,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight(650),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = Line19,
                )
                Text(
                    item.time,
                    Modifier.alignByBaseline(),
                    color = colors.muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    style = Line19,
                )
            }
            Text(
                item.subj,
                Modifier.height(19.dp),
                color = colors.ink,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = Line19,
            )
            Text(
                item.prev,
                Modifier.height(19.dp),
                color = colors.muted,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = Line19,
            )
        }
    }
}

/** The rack's own buttons, riding with it: its title and Done, and the six cubbies. */
@Composable
private fun RackUi(
    state: PigeonholeState,
    text: PigeonholeText,
    cubFocus: List<FocusRequester>,
    screen: () -> LayoutCoordinates?,
) {
    state.listVersion
    val density = LocalDensity.current.density
    val up = state.rackUp
    val k = state.k
    if (state.width <= 0f) return
    val shadow = Shadow(Color(60, 28, 8, 140), Offset(0f, 1f * density), 0f)
    Box(
        Modifier.fillMaxWidth()
            .height(state.rackH.dp)
            .offset {
                state.frame
                IntOffset(0, (state.rackTop() * density).roundToInt())
            }
            .semantics {
                isTraversalGroup = true
                contentDescription = text.folders
            },
    ) {
        Row(
            Modifier.fillMaxWidth()
                .height((PigeonholeState.Head * k).dp)
                .padding(start = 20.dp, end = 10.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val from = state.rackFrom
            Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
                Text(
                    if (from == null) text.folders else text.moveTo,
                    Modifier.alignByBaseline(),
                    color = Color(0xFFFFF3E1),
                    fontSize = 15.sp,
                    fontWeight = FontWeight(650),
                    letterSpacing = .01.em,
                    maxLines = 1,
                    style = TextStyle(shadow = shadow),
                )
                if (from != null) {
                    Text(
                        from,
                        Modifier.alignByBaseline().padding(start = 6.dp).graphicsLayer {
                            alpha = .72f
                        },
                        color = Color(0xFFFFF3E1),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = .01.em,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(shadow = shadow),
                    )
                }
            }
            var doneFocused by remember { mutableStateOf(false) }
            Box(
                Modifier.sizeIn(minWidth = 60.dp, minHeight = 40.dp)
                    .testTag("done")
                    .onFocusChanged { doneFocused = it.isFocused }
                    .clickable(
                        remember { MutableInteractionSource() },
                        null,
                        enabled = up,
                        role = Role.Button,
                    ) {
                        state.releaseScripted()
                        state.done()
                    }
                    .then(if (up) Modifier else Modifier.clearAndSetSemantics {})
                    .drawBehind { if (doneFocused) ring(10.dp.toPx(), inset = true) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text.done,
                    color = Color(0xFFFFE3B8),
                    fontSize = 15.sp,
                    fontWeight = FontWeight(650),
                    maxLines = 1,
                    style = TextStyle(shadow = shadow),
                )
            }
        }
        for (i in 0 until 6) {
            val cub = state.cubbies[i]
            val n = state.cubCount(i)
            val label = text.cubby(cub.name, n, cub.id == state.view)
            var focused by remember(i) { mutableStateOf(false) }
            var here by remember(i) { mutableStateOf<LayoutCoordinates?>(null) }
            Box(
                Modifier.offset(state.cubX(i).dp, state.cubY(i, 0f).dp)
                    .size(state.cubW().dp, state.cubH().dp)
                    .onPlaced { here = it }
                    .testTag("cub:${cub.id}")
                    .then(
                        if (!up) Modifier.clearAndSetSemantics {}
                        else
                            Modifier.semantics {
                                role = Role.Button
                                contentDescription = label
                                onClick {
                                    state.cubTap(i)
                                    true
                                }
                            },
                    )
                    .onFocusChanged {
                        focused = it.isFocused
                        if (it.isFocused) state.focusCub(i)
                    }
                    .onKeyEvent { e ->
                        val press =
                            e.key == Key.Enter || e.key == Key.Spacebar || e.key == Key.NumPadEnter
                        if (!press || e.type != KeyEventType.KeyDown) return@onKeyEvent false
                        state.releaseScripted()
                        state.cubTap(i)
                        true
                    }
                    .then(
                        if (up)
                            Modifier.pointerInput(i) {
                                cubGesture(state, i, { screen() }, { here }, density)
                            }
                        else Modifier,
                    )
                    .focusRequester(cubFocus[i])
                    .focusable(enabled = up)
                    .drawBehind {
                        if (focused) ring(6.dp.toPx(), inset = true, color = Color(0xFFFFD58A))
                    },
            )
        }
    }
}

// The finger on a cubby, as the prototype's pointer handlers: down, moves, up; a touch cancelled
// lets go where it was last seen. A real touch takes over from the script's finger first.
private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.cubGesture(
    state: PigeonholeState,
    i: Int,
    screen: () -> LayoutCoordinates?,
    here: () -> LayoutCoordinates?,
    density: Float,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        state.releaseScripted()
        val s = screen() ?: return@awaitEachGesture
        val me = here() ?: return@awaitEachGesture
        fun at(c: PointerInputChange) = s.localPositionOf(me, c.position) / density
        val p0 = at(down)
        state.cubDown(i, p0.x, p0.y)
        var ended = false
        try {
            while (true) {
                val e = awaitPointerEvent()
                val c = e.changes.firstOrNull { it.id == down.id } ?: break
                if (!c.pressed) {
                    ended = true
                    val p = at(c)
                    state.ptrUp(p.x, p.y)
                    break
                }
                if (c.positionChanged()) {
                    val p = at(c)
                    state.ptrMove(p.x, p.y)
                    c.consume()
                }
            }
        } finally {
            if (!ended && !state.scriptedTouch) state.ptrCancel()
        }
    }
}

// The prototype's focus outline: 2 wide, drawn just inside the box, its corners rounded by
// [radius].
private fun DrawScope.ring(radius: Float, inset: Boolean, color: Color = Color(0xFFFFD58A)) {
    val w = 2.dp.toPx()
    val o = if (inset) w / 2 else -w / 2
    drawRoundRect(
        color,
        Offset(o, o),
        Size(size.width - 2 * o, size.height - 2 * o),
        CornerRadius(radius),
        style = Stroke(w),
    )
}
