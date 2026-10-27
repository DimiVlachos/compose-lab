package dev.dimvlachos.lab.core.presentation.components.rubberband

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
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
import dev.dimvlachos.lab.core.presentation.ui.RubberBandScreenColors
import kotlin.math.roundToInt
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** The clips the rubber band plays. */
internal val RubberBandSounds =
    setOf(
        Sound.BandSend,
        Sound.BandGrab,
        Sound.BandTaut,
        Sound.BandBoing,
        Sound.BandYank,
        Sound.BandBurst,
        Sound.BandTick,
        Sound.BandTwang,
    )

/**
 * The chat's words: the field's, the send button's and the caption's, the day line and what is
 * said.
 */
internal class RubberBandText(
    val placeholder: String,
    val send: String,
    val unsend: String,
    val captionPull: String,
    val captionRest: String,
    val day: String,
    val delivered: String,
    val sent: String,
    val deliveredSaid: String,
    val unsentSaid: String,
    val typing: String,
)

/**
 * A chat thread and its composer where the newest message stays tied back to the text box by a
 * rubber band for nine seconds after it lands, as the prototype draws it. Pull the band (or the
 * bubble) and let go, and it yanks the message back down into [field], letter by letter, to edit;
 * leave it and the band thins and snaps, and it says Delivered. [appBar] sits above the thread. The
 * screen pads itself for the keyboard (`imePadding`), so the thread and composer sit above it and
 * drop back when it closes. [onTouch] hears a real finger take hold of the band or send, for a host
 * to stop a script; [overlay] is drawn over everything.
 */
@Composable
internal fun RubberBandChat(
    state: RubberBandState,
    field: TextFieldState,
    text: RubberBandText,
    colors: RubberBandScreenColors,
    dark: Boolean,
    sounds: SoundPlayer,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false,
    onTouch: () -> Unit = {},
    appBar: @Composable ColumnScope.() -> Unit = {},
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    SideEffect { state.reducedMotion = reducedMotion }
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val measurer = rememberTextMeasurer()
    val fieldStyle = remember(colors) { fieldStyle(colors) }
    val places = remember(state) { Places() }
    places.measurer = measurer
    places.fieldStyle = fieldStyle
    places.density = density
    val painter = remember { RubberBandPainter() }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var said by remember { mutableStateOf("") }
    val pending = remember { StringBuilder() }
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    LaunchedEffect(state, sounds, haptics, field, keyboard) {
        state.glyphs = places
        state.onEvent = { event, v ->
            when (event) {
                BandEvent.Send -> sounds.playVaried(Sound.BandSend, .7f)
                BandEvent.Grab -> {
                    sounds.playVaried(Sound.BandGrab, .8f)
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                }
                BandEvent.Taut -> {
                    sounds.playVaried(Sound.BandTaut, .7f)
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                }
                // Louder and longer the further it was pulled, as the prototype's boing.
                BandEvent.Boing -> sounds.playVaried(Sound.BandBoing, v)
                BandEvent.Yank -> {
                    sounds.playVaried(Sound.BandYank)
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                }
                BandEvent.Burst -> sounds.playVaried(Sound.BandBurst, .7f)
                // Each tick a little higher, as the prototype's + v·260 Hz.
                BandEvent.Tick -> sounds.playVaried(Sound.BandTick, .6f, 1f + v * .108f)
                BandEvent.Twang -> sounds.playVaried(Sound.BandTwang, .8f)
            }
        }
        state.onSay = {
            // What is said in one frame is said together (a band snapped by a new send, then Sent),
            // and
            // the live region is cleared for a frame first, so the same words are announced again.
            val words =
                when (it) {
                    BandSay.Sent -> text.sent
                    BandSay.Delivered -> text.deliveredSaid
                    BandSay.Unsent -> text.unsentSaid
                }
            val flush = pending.isEmpty()
            pending.append(if (pending.isEmpty()) words else " $words")
            if (flush) {
                scope.launch {
                    said = ""
                    withFrameNanos {}
                    said = pending.toString()
                    pending.clear()
                }
            }
        }
        state.onRestore = { message, byHand ->
            field.setTextAndPlaceCursorAtEnd(message)
            // Scrolled to its end, as the prototype's `scrollLeft = scrollWidth`, where its letters
            // landed.
            scope.launch {
                withFrameNanos {}
                scroll.scrollTo(scroll.maxValue)
            }
            if (byHand) {
                runCatching { focus.requestFocus() }
                keyboard?.show()
            }
        }
        try {
            awaitCancellation()
        } finally {
            state.onEvent = null
            state.onSay = null
            state.onRestore = null
            state.glyphs = null
        }
    }
    LaunchedEffect(state, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var last = 0L
            while (true) {
                // At rest the loop sleeps, until a send or a touch sets the band moving again.
                if (state.settled) {
                    val seen = state.wake
                    snapshotFlow { state.wake }.first { it != seen }
                    last = 0L
                }
                withFrameNanos { now ->
                    // The first frame moves nothing; a stall can't blow the springs up.
                    val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(.15f)
                    last = now
                    state.advance(dt)
                }
            }
        }
    }

    val send = {
        onTouch()
        if (state.send(field.text.toString())) field.clearText()
    }
    val caption =
        remember(text.captionPull, text.captionRest, colors) {
            captionText(text, colors.pull, colors.muted)
        }
    val captionHalo =
        remember(text.captionPull, text.captionRest, colors) {
            captionText(text, colors.background, colors.background)
        }
    val captionFill =
        remember(caption, measurer, density) {
            measurer.measure(caption, CaptionStyle, density = density)
        }
    val captionStroke =
        remember(captionHalo, measurer, density) {
            measurer.measure(captionHalo, CaptionStyle, density = density)
        }

    Box(
        modifier
            .onSizeChanged {
                // The prototype's scale, from the screen's size before the keyboard takes its
                // share.
                state.size(it.width / density.density, it.height / density.density)
            }
            .imePadding()
            .onGloballyPositioned { places.root = it },
    ) {
        Column(Modifier.fillMaxSize()) {
            appBar()
            Thread(
                state,
                places,
                text,
                caption,
                colors,
                reducedMotion,
                onTouch,
                Modifier.weight(1f),
            )
            Composer(state, places, field, text, colors, fieldStyle, focus, scroll, painter, send)
        }
        Canvas(Modifier.fillMaxSize()) {
            state.frame
            places.sync(state)
            with(painter) { drawBubble(state, places.bubbleText, colors.me, colors.meInk) }
            var c2: Canvas2D? = null
            drawIntoCanvas {
                val c = Canvas2D(it, size.width.toInt(), size.height.toInt())
                c.scale(density.density, density.density)
                with(painter) { c.drawBandLayer(state, dark) }
                c2 = c
            }
            with(painter) { drawCaption(state, captionFill, captionStroke) }
            c2?.let { c -> with(painter) { c.drawAfterLayer(state, dark, colors.meCss) } }
            with(painter) { drawLetters(state, places.letterLayouts(state), colors.me, colors.ink) }
        }
        Box(
            Modifier.size(1.dp).semantics {
                liveRegion = LiveRegionMode.Polite
                if (said.isNotEmpty()) contentDescription = said
            },
        )
        overlay()
    }
}

/**
 * Where the screen's parts are laid out, kept as their coordinates and read off as the canvas
 * draws, when everything is placed; and the text layout the burst's letters come from and go to.
 */
private class Places : GlyphPlaces {
    var root: LayoutCoordinates? = null
    var eye: LayoutCoordinates? = null
    var fieldBox: LayoutCoordinates? = null
    var fieldText: LayoutCoordinates? = null
    var send: LayoutCoordinates? = null
    var thread: LayoutCoordinates? = null
    var bubbleId = -1L
    var bubble: LayoutCoordinates? = null
    var bubbleText: TextLayoutResult? = null
    var captionId = -1L
    var caption: LayoutCoordinates? = null
    var measurer: TextMeasurer? = null
    var fieldStyle = TextStyle.Default
    var density: Density = Density(1f)
    private var layouts: List<TextLayoutResult> = emptyList()

    private fun rect(c: LayoutCoordinates?): Rect? {
        val r = root?.takeIf { it.isAttached } ?: return null
        val cc = c?.takeIf { it.isAttached } ?: return null
        val b = r.localBoundingBoxOf(cc, clipBounds = false)
        val d = density.density
        return Rect(b.left / d, b.top / d, b.right / d, b.bottom / d)
    }

    /** A point in the thread, in px, in the screen's dp. */
    fun inScreen(p: Offset): Offset? {
        val r = root?.takeIf { it.isAttached } ?: return null
        val t = thread?.takeIf { it.isAttached } ?: return null
        return r.localPositionOf(t, p) / density.density
    }

    fun sync(state: RubberBandState) {
        rect(eye)?.let { state.eyeAt(it.center) }
        val box = rect(fieldBox)
        val inner = rect(fieldText)
        if (box != null && inner != null) state.fieldAt(inner.left, inner.right, box.center.y)
        val button = rect(send)
        if (box != null && button != null) state.boxesAt(box, button)
        rect(thread)?.let { state.threadAt(it.top) }
        rect(bubble)?.let { state.bubbleAt(bubbleId, it) }
        rect(caption)?.let { state.captionAt(captionId, it.topLeft) }
    }

    override fun inBubble(text: String, starts: IntArray, out: FloatArray) {
        val l = bubbleText ?: return
        val d = density.density
        for (i in starts.indices) {
            val at = starts[i].coerceAtMost(l.layoutInput.text.length - 1)
            val b = if (at >= 0) l.getBoundingBox(at) else Rect.Zero
            out[i * 2] = b.center.x / d + BubblePadX
            out[i * 2 + 1] = b.center.y / d + BubblePadY
        }
    }

    override fun inField(text: String, starts: IntArray, out: FloatArray): Float {
        val m = measurer ?: return 0f
        val l = m.measure(text, fieldStyle, softWrap = false, maxLines = 1, density = density)
        val d = density.density
        for (i in starts.indices) out[i] = l.getBoundingBox(starts[i]).center.x / d
        // Each letter set on its own for its flight, now, not in the draw pass.
        layouts =
            List(starts.size) {
                val end = if (it + 1 < starts.size) starts[it + 1] else text.length
                m.measure(
                    text.substring(starts[it], end),
                    fieldStyle,
                    softWrap = false,
                    maxLines = 1,
                    density = density,
                )
            }
        return l.getLineRight(0) / d
    }

    /** Each landing letter set in the field's style, measured as it burst. */
    fun letterLayouts(state: RubberBandState): List<TextLayoutResult> =
        if (layouts.size == state.letters.size) layouts else emptyList()
}

/**
 * The thread: the day, then the rows, standing on the composer and clipped at the top, gliding when
 * a row comes or goes. A finger that comes down on the tethered bubble or near its band takes hold
 * of the band.
 */
@Composable
private fun Thread(
    state: RubberBandState,
    places: Places,
    text: RubberBandText,
    caption: AnnotatedString,
    colors: RubberBandScreenColors,
    reducedMotion: Boolean,
    onTouch: () -> Unit,
    modifier: Modifier,
) {
    Box(
        modifier
            .fillMaxWidth()
            .clipToBounds()
            .onGloballyPositioned { places.thread = it }
            .pointerInput(state) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val p = places.inScreen(down.position) ?: return@awaitEachGesture
                    if (!state.down(p.x, p.y)) return@awaitEachGesture
                    onTouch()
                    down.consume()
                    var lifted = false
                    try {
                        while (true) {
                            val e = awaitPointerEvent()
                            val c = e.changes.firstOrNull { it.id == down.id } ?: break
                            val q = places.inScreen(c.position) ?: break
                            c.consume()
                            if (!c.pressed) {
                                state.up(q.x, q.y)
                                lifted = true
                                break
                            }
                            state.move(q.x, q.y)
                        }
                    } finally {
                        // A gesture the system took finishes from the last point it saw.
                        if (!lifted) state.cancel()
                    }
                }
            }
            .graphicsLayer { alpha = state.threadAlpha }
            .padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().layout { measurable, constraints ->
                // Laid out from the bottom, as tall as its rows, gliding from where it stood.
                val p =
                    measurable.measure(
                        constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity),
                    )
                state.threadMeasured(p.height / density)
                layout(constraints.maxWidth, constraints.maxHeight) {
                    state.glideTick
                    p.place(
                        0,
                        constraints.maxHeight - p.height + (state.glideY() * density).roundToInt(),
                    )
                }
            },
        ) {
            BasicText(
                text.day,
                Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp, bottom = 4.dp),
                style =
                    TextStyle(
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = BodyLine,
                        lineHeightStyle = CssLines,
                    ),
                color = { colors.muted },
            )
            for (row in state.rows) {
                key(row.id) {
                    RowView(state, places, row, text, caption, colors, reducedMotion, onTouch)
                }
            }
        }
    }
}

@Composable
private fun RowView(
    state: RubberBandState,
    places: Places,
    row: ChatRow,
    text: RubberBandText,
    caption: AnnotatedString,
    colors: RubberBandScreenColors,
    reducedMotion: Boolean,
    onTouch: () -> Unit,
) {
    val me = row.who == Who.Me
    val tethered = state.tetheredId == row.id
    val receipt = state.receiptId == row.id
    Column(
        Modifier.fillMaxWidth().padding(top = if (row.gap) 12.dp else 6.dp),
        horizontalAlignment = if (me) Alignment.End else Alignment.Start,
    ) {
        Box(
            Modifier.fillMaxWidth(.76f),
            contentAlignment = if (me) Alignment.TopEnd else Alignment.TopStart,
        ) {
            when (row.who) {
                Who.Me ->
                    BasicText(
                        row.text,
                        // Tethered, the canvas draws it; it stays here, undrawn, for layout and
                        // for screen readers (a transparent node would be hidden from them).
                        Modifier.then(if (tethered) Modifier.drawWithContent {} else Modifier)
                            .then(
                                if (receipt) {
                                    Modifier.graphicsLayer {
                                        state.frame
                                        val j = state.jolt(row.id)
                                        translationX = j.x * density
                                        translationY = j.y * density
                                    }
                                } else Modifier,
                            )
                            .onGloballyPositioned {
                                if (state.tetheredId == row.id) {
                                    places.bubbleId = row.id
                                    places.bubble = it
                                }
                            }
                            .background(colors.me, MeShape)
                            .padding(horizontal = BubblePadX.dp, vertical = BubblePadY.dp),
                        style = BubbleStyle,
                        color = { colors.meInk },
                        onTextLayout = { if (state.tetheredId == row.id) places.bubbleText = it },
                    )
                Who.Them ->
                    BasicText(
                        row.text,
                        Modifier.themBubble(colors)
                            .padding(horizontal = BubblePadX.dp, vertical = BubblePadY.dp),
                        style = BubbleStyle,
                        color = { colors.ink },
                    )
                Who.Typing ->
                    Box(
                        Modifier.themBubble(colors).semantics { contentDescription = text.typing },
                    ) {
                        TypingDots(colors, reducedMotion)
                    }
            }
        }
        if (tethered) Caption(state, places, row, text, caption, colors, onTouch)
        if (receipt) Receipt(text, colors, reducedMotion)
    }
}

private fun Modifier.themBubble(colors: RubberBandScreenColors) = drawBehind {
    // box-shadow: 0 0 0 1px, a hairline ring just outside the bubble.
    val w = 1.dp.toPx()
    val r = 19.dp.toPx()
    drawRoundRect(
        colors.line,
        Offset(-w, -w),
        Size(size.width + 2 * w, size.height + 2 * w),
        CornerRadius(r + w),
    )
}
    .background(colors.card, ThemShape)

/**
 * The caption under the tethered bubble: a button that unsends. Its words are drawn on the canvas,
 * above the band and haloed; here they only take their place, transparent.
 */
@Composable
private fun Caption(
    state: RubberBandState,
    places: Places,
    row: ChatRow,
    text: RubberBandText,
    caption: AnnotatedString,
    colors: RubberBandScreenColors,
    onTouch: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val keys = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    Box(
        Modifier.padding(top = 4.dp)
            .drawBehind {
                if (focused && keys) {
                    val o = 2.dp.toPx()
                    drawRoundRect(
                        colors.me,
                        Offset(-o, -o),
                        Size(size.width + 2 * o, size.height + 2 * o),
                        CornerRadius(4.dp.toPx() + o),
                        style = Stroke(2.dp.toPx()),
                    )
                }
            }
            .clickable(interaction, indication = null, role = Role.Button) {
                onTouch()
                state.unsendNow()
            }
            .semantics { contentDescription = text.unsend }
            .testTag("unsend")
            .padding(2.dp),
    ) {
        BasicText(
            transparent(caption),
            Modifier.clearAndSetSemantics {}
                .onGloballyPositioned {
                    places.captionId = row.id
                    places.caption = it
                },
            style = CaptionStyle,
        )
    }
}

@Composable
private fun Receipt(text: RubberBandText, colors: RubberBandScreenColors, reducedMotion: Boolean) {
    val fade = remember { Animatable(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(Unit) { fade.animateTo(1f, tween(400, easing = EaseOut)) }
    BasicText(
        text.delivered,
        Modifier.padding(top = 3.dp).graphicsLayer { alpha = fade.value },
        style = TextStyle(fontSize = 11.5.sp, lineHeight = BodyLine, lineHeightStyle = CssLines),
        color = { colors.muted },
    )
}

/** Three dots that rise and brighten in turn, as the prototype's `.typing`. */
@Composable
private fun TypingDots(colors: RubberBandScreenColors, reducedMotion: Boolean) {
    val t =
        if (reducedMotion) null
        else
            rememberInfiniteTransition(label = "typing")
                .animateFloat(
                    0f,
                    1000f,
                    infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
                    label = "dots",
                )
    Row(
        Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(3) { i ->
            Box(
                Modifier.size(7.dp)
                    .graphicsLayer {
                        val v = t?.value?.let { dotLift(it - i * 150f) } ?: 0f
                        alpha = .5f + .5f * v
                        translationY = -2.dp.toPx() * v
                    }
                    .background(colors.muted, CircleShape),
            )
        }
    }
}

// The dot keyframes: at 30 % it is fully lit and 2 px up, back down by the end of each second.
private fun dotLift(ms: Float): Float {
    val p = (((ms % 1000f) + 1000f) % 1000f) / 1000f
    return if (p < .3f) cssEase(p / .3f) else 1 - cssEase((p - .3f) / .7f)
}

@Composable
private fun Composer(
    state: RubberBandState,
    places: Places,
    field: TextFieldState,
    text: RubberBandText,
    colors: RubberBandScreenColors,
    fieldStyle: TextStyle,
    focus: FocusRequester,
    scroll: ScrollState,
    painter: RubberBandPainter,
    send: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Row(
        Modifier.fillMaxWidth()
            .background(colors.bar)
            .drawBehind { drawRect(colors.line, size = Size(size.width, 1.dp.toPx())) }
            .padding(start = 10.dp, end = 10.dp, top = 9.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Canvas(Modifier.size(34.dp).onGloballyPositioned { places.eye = it }) {
            drawIntoCanvas {
                val c = Canvas2D(it, size.width.toInt(), size.height.toInt())
                c.scale(density, density)
                with(painter) { c.drawEyeSlot(34f) }
            }
        }
        val shape = RoundedCornerShape(20.dp)
        BasicTextField(
            state = field,
            modifier =
                Modifier.weight(1f)
                    .height(40.dp)
                    .focusRequester(focus)
                    .onGloballyPositioned { places.fieldBox = it }
                    .semantics { contentDescription = text.placeholder }
                    .testTag("message"),
            textStyle = fieldStyle,
            cursorBrush = SolidColor(colors.me),
            lineLimits = TextFieldLineLimits.SingleLine,
            keyboardOptions =
                KeyboardOptions(imeAction = ImeAction.Send, autoCorrectEnabled = false),
            onKeyboardAction = { send() },
            interactionSource = interaction,
            scrollState = scroll,
            decorator = { inner ->
                Box(
                    Modifier.fillMaxSize()
                        .clip(shape)
                        .background(colors.card)
                        .border(1.dp, if (focused) colors.me else colors.line, shape)
                        .padding(horizontal = 15.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    // Faded while a band is tied to the field, and blank while letters land in it.
                    if (field.text.isEmpty() && !state.landing) {
                        BasicText(
                            text.placeholder,
                            style = fieldStyle,
                            maxLines = 1,
                            color = {
                                state.frame
                                colors.muted.copy(alpha = colors.muted.alpha * state.ph)
                            },
                        )
                    }
                    Box(Modifier.fillMaxWidth().onGloballyPositioned { places.fieldText = it }) {
                        inner()
                    }
                }
            },
        )
        val enabled = field.text.isNotBlank() && !state.busy
        val alpha by
            animateFloatAsState(
                if (enabled) 1f else .35f,
                if (state.reducedMotion) snap() else tween(150),
                label = "send",
            )
        Box(
            Modifier.size(40.dp)
                .graphicsLayer { this.alpha = alpha }
                .clip(CircleShape)
                .background(colors.me)
                .onGloballyPositioned { places.send = it }
                .clickable(enabled = enabled, role = Role.Button) { send() }
                .semantics { contentDescription = text.send }
                .testTag("send"),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(19.dp)) {
                // M12 19V6 M6.5 11.5 12 6l5.5 5.5 in a 24 box.
                val s = size.width / 24f
                val st = Stroke(2.4f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
                val arrow =
                    androidx.compose.ui.graphics.Path().apply {
                        moveTo(12f * s, 19f * s)
                        lineTo(12f * s, 6f * s)
                        moveTo(6.5f * s, 11.5f * s)
                        lineTo(12f * s, 6f * s)
                        lineTo(17.5f * s, 11.5f * s)
                    }
                drawPath(arrow, Color.White, style = st)
            }
        }
    }
}

private fun fieldStyle(colors: RubberBandScreenColors) =
    TextStyle(
        color = colors.ink,
        fontSize = RubberBandState.FieldFont.sp,
        fontWeight = FontWeight.Normal,
    )

private fun captionText(text: RubberBandText, pull: Color, rest: Color) = buildAnnotatedString {
    withStyle(SpanStyle(color = pull, fontWeight = FontWeight(650))) { append(text.captionPull) }
    withStyle(SpanStyle(color = rest)) { append(" " + text.captionRest) }
}

private fun transparent(s: AnnotatedString) = buildAnnotatedString {
    append(s)
    addStyle(SpanStyle(color = Color.Transparent), 0, s.length)
}

private const val BubblePadX = 13f
private const val BubblePadY = 9f
private val BodyLine = 1.55.em
private val CssLines = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)
private val BubbleStyle =
    TextStyle(
        fontSize = RubberBandState.BubbleFont.sp,
        lineHeight = 1.32.em,
        lineHeightStyle = CssLines,
    )
private val CaptionStyle = TextStyle(fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
private val MeShape = RoundedCornerShape(19.dp, 19.dp, 6.dp, 19.dp)
private val ThemShape = RoundedCornerShape(19.dp, 19.dp, 19.dp, 6.dp)
