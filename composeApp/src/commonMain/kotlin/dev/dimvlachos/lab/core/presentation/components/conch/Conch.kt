package dev.dimvlachos.lab.core.presentation.components.conch

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.dimvlachos.lab.core.platform.Sound
import dev.dimvlachos.lab.core.platform.SoundPlayer
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.ui.ConchScreenColors
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first

/** The clips the conch plays. */
internal val ConchSounds =
    setOf(
        Sound.ConchStart,
        Sound.ConchTick,
        Sound.ConchUntick,
        Sound.ConchWhoosh,
        Sound.ConchClack,
        Sound.ConchSand,
        Sound.ConchHum,
    )

/**
 * Where the conch's pieces are on the screen it is drawn over: the screen itself ([stage]), the
 * composer and the button, and each voice note's shell. They tell [state] its layout in dp as they
 * are laid out, and the flying shell where its bubble is.
 */
@Stable
internal class ConchAnchors(private val state: ConchState) {
    private var root: LayoutCoordinates? = null
    private var button: LayoutCoordinates? = null
    private var composer: LayoutCoordinates? = null
    private var density = 1f

    fun stage(coordinates: LayoutCoordinates, density: Float) {
        root = coordinates
        this.density = density
        update()
    }

    fun composer(coordinates: LayoutCoordinates) {
        composer = coordinates
        update()
    }

    fun button(coordinates: LayoutCoordinates) {
        button = coordinates
        update()
    }

    /** [note]'s shell canvas is laid out at [coordinates]; a flying shell aims for it. */
    fun noteShell(note: VoiceNote, coordinates: LayoutCoordinates) {
        if (state.fly?.note !== note) return
        val r = root?.takeIf { it.isAttached } ?: return
        if (!coordinates.isAttached) return
        val c =
            r.localPositionOf(
                coordinates,
                Offset(coordinates.size.width / 2f, coordinates.size.height / 2f),
            )
        state.flyTarget(c.x / density, c.y / density)
    }

    private fun update() {
        val r = root?.takeIf { it.isAttached } ?: return
        val b = button?.takeIf { it.isAttached } ?: return
        val c = composer?.takeIf { it.isAttached } ?: return
        val centre = r.localPositionOf(b, Offset(b.size.width / 2f, b.size.height / 2f))
        val top = r.localPositionOf(c, Offset.Zero).y
        state.layout(
            r.size.width / density,
            centre.x / density,
            centre.y / density,
            top / density,
        )
    }
}

/**
 * Runs the conch: its frame loop, which sleeps while everything is at rest; its sounds and haptics;
 * and the hum of a note playing, which swells with the recording. [announce] hears every event, for
 * the screen's live region.
 */
@Composable
internal fun ConchEffects(
    state: ConchState,
    sounds: SoundPlayer,
    announce: (ConchEvent, VoiceNote?) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val onAnnounce by rememberUpdatedState(announce)
    LaunchedEffect(state, sounds, haptics) {
        val listener: (ConchEvent, VoiceNote?) -> Unit = { event, note ->
            when (event) {
                ConchEvent.Start -> {
                    sounds.play(Sound.ConchStart)
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                ConchEvent.Arm -> {
                    sounds.play(Sound.ConchTick)
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                }
                ConchEvent.Disarm -> {
                    sounds.play(Sound.ConchUntick)
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                }
                ConchEvent.Sent -> sounds.play(Sound.ConchWhoosh)
                ConchEvent.Landed -> {
                    sounds.play(Sound.ConchClack)
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                }
                ConchEvent.Canceled -> {
                    sounds.play(Sound.ConchSand)
                    haptics.performHapticFeedback(HapticFeedbackType.Reject)
                }
                ConchEvent.Hint -> Unit
            }
            onAnnounce(event, note)
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
    // Playback: the shell hums like the sea in it, its loudness following the recording. Only
    // while the screen is resumed: in the background the clock stops, and the hum with it.
    LaunchedEffect(state, sounds, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            snapshotFlow { state.playing != null }
                .collectLatest { on ->
                    if (!on) return@collectLatest
                    var volume = HumFloor
                    val loop = sounds.loop(Sound.ConchHum, volume)
                    try {
                        var last = 0L
                        while (true) {
                            withFrameNanos { now ->
                                val dt = if (last == 0L) 0f else (now - last) / 1e9f
                                last = now
                                val target = (.02f + state.humLevel * .16f) / .18f
                                volume += (target - volume) * min(1f, dt / .05f)
                                loop.setVolume(volume)
                            }
                        }
                    } finally {
                        loop.stop()
                    }
                }
        }
    }
}

private const val HumFloor = .02f / .18f

/**
 * The prototype's fx canvas, over the whole screen: the shell in the composer's button, lifted into
 * the lamp light while recording with rings from its mouth, flying into the chat, and the sand of a
 * cancel. It draws in a layer of its own, as the prototype's canvas does, and never takes a touch.
 */
@Composable
internal fun ConchOverlay(
    state: ConchState,
    colors: ConchScreenColors,
    modifier: Modifier = Modifier,
) {
    val dark = LabTheme.isDark
    Canvas(
        modifier.fillMaxSize().graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        },
    ) {
        state.frame
        drawIntoCanvas { canvas ->
            val c = Canvas2D(canvas, size.width.toInt(), size.height.toInt())
            c.scale(density, density)
            c.drawConchStage(state, size.width / density, dark, colors.background, density)
        }
    }
}

/**
 * The composer's record button: a round well the conch rests in (the shell itself is drawn by
 * [ConchOverlay]). Hold it to record, slide left to cancel, let go to send; a quick tap only shows
 * the hint. Space or Enter starts and again sends, Escape cancels; a screen reader's tap does the
 * same, with Send and Cancel actions while it records.
 */
@Composable
internal fun ConchButton(
    state: ConchState,
    anchors: ConchAnchors,
    colors: ConchScreenColors,
    label: String,
    sendLabel: String,
    cancelLabel: String,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val recording = state.mode == ConchMode.Rec
    val held = remember { BooleanArray(1) }
    Box(
        modifier
            .size(52.dp)
            .onGloballyPositioned { anchors.button(it) }
            .drawBehind {
                if (focused) {
                    val w = 2.dp.toPx()
                    val o = 2.dp.toPx() + w / 2f
                    drawRoundRect(
                        colors.accent,
                        Offset(-o, -o),
                        Size(size.width + o * 2f, size.height + o * 2f),
                        CornerRadius(size.width / 2f + o),
                        style = Stroke(w),
                    )
                }
            }
            .clip(CircleShape)
            .background(colors.chip)
            .testTag("conch")
            .pointerInput(state) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (state.mode == ConchMode.Fly) return@awaitEachGesture
                    down.consume()
                    state.startRec()
                    val ox = down.position.x
                    var done = false
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            // The slide is measured from where the finger went down, so a few
                            // far-apart samples work as well as many.
                            state.dragTo((change.position.x - ox) / density)
                            change.consume()
                            if (!change.pressed) {
                                state.finishRec()
                                done = true
                                break
                            }
                        }
                    } finally {
                        if (!done) state.cancelRec()
                    }
                }
            }
            .onKeyEvent { e ->
                when {
                    e.key != Key.Spacebar && e.key != Key.Enter && e.key != Key.NumPadEnter ->
                        if (e.key == Key.Escape && e.type == KeyEventType.KeyDown && recording) {
                            state.cancelRec()
                            true
                        } else false
                    e.type == KeyEventType.KeyUp -> {
                        held[0] = false
                        true
                    }
                    e.type == KeyEventType.KeyDown -> {
                        if (!held[0]) {
                            held[0] = true
                            if (state.mode == ConchMode.Rec) state.finishRec() else state.startRec()
                        }
                        true
                    }
                    else -> false
                }
            }
            // A key let go elsewhere would leave the latch set and swallow the next press.
            .onFocusChanged { if (!it.isFocused) held[0] = false }
            .focusable(interactionSource = interaction)
            .semantics {
                role = Role.Button
                contentDescription = label
                onClick {
                    if (state.mode == ConchMode.Rec) state.finishRec() else state.startRec()
                    true
                }
                if (recording) {
                    customActions =
                        listOf(
                            CustomAccessibilityAction(sendLabel) {
                                state.finishRec()
                                true
                            },
                            CustomAccessibilityAction(cancelLabel) {
                                state.cancelRec()
                                true
                            },
                        )
                }
            },
    )
}

/**
 * A voice note in the chat: its little shell, which glows and hums as it plays, the recording's
 * bars filling in, and its length (counting down while it plays) beside when it was sent. Tap it to
 * play, again to pause. [label] names it for screen readers.
 */
@Composable
internal fun VoiceNoteBubble(
    note: VoiceNote,
    state: ConchState,
    anchors: ConchAnchors,
    colors: ConchScreenColors,
    label: String,
    modifier: Modifier = Modifier,
) {
    val mine = note.mine
    val dark = LabTheme.isDark
    val ink = if (mine) colors.mineInk else colors.ink
    val shape =
        RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 18.dp,
            bottomEnd = if (mine) 6.dp else 18.dp,
            bottomStart = if (mine) 18.dp else 6.dp,
        )
    val interaction = remember { MutableInteractionSource() }
    // The bars' heights, from the recording, as the prototype lays out its 26.
    val bars =
        remember(note) {
            val l = note.levels
            FloatArray(Bars) { i ->
                val a =
                    if (l.isEmpty()) .3f
                    else l[min(l.size - 1, floor(i / Bars.toFloat() * l.size).toInt())]
                (4f + a * 20f).roundToInt().toFloat()
            }
        }
    Row(
        modifier
            .widthIn(min = 196.dp)
            .graphicsLayer { alpha = if (note.hidden) 0f else 1f }
            .clip(shape)
            .background(if (mine) colors.mine else colors.card)
            .border(1.dp, if (mine) Color.Transparent else colors.line, shape)
            .then(
                // Out of reach while its shell is still flying in.
                if (note.hidden) Modifier.clearAndSetSemantics {}
                else
                    Modifier.clickable(
                            interactionSource = interaction,
                            indication = null,
                            role = Role.Button,
                        ) {
                            state.togglePlay(note)
                        }
                        .semantics { contentDescription = label },
            )
            .padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
    ) {
        Canvas(
            Modifier.size(40.dp, 44.dp)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .onGloballyPositioned { anchors.noteShell(note, it) },
        ) {
            // Redrawn every frame only while it plays or wobbles; at rest it stays as it is.
            if (note.playing || note.hidden || note.moving) state.frame
            drawIntoCanvas { canvas ->
                val c = Canvas2D(canvas, size.width.toInt(), size.height.toInt())
                c.scale(density, density)
                c.drawBubbleShell(note, state.time, dark, density)
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(
            Modifier.width(128.dp).height(44.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        ) {
            Canvas(Modifier.fillMaxWidth().height(24.dp)) {
                if (note.playing) state.frame
                val gap = 2.dp.toPx()
                val w = ((size.width - gap * (Bars - 1)) / Bars).coerceIn(2.dp.toPx(), 4.dp.toPx())
                val on = floor(note.pos * Bars).toInt()
                for (i in 0 until Bars) {
                    val h = bars[i].dp.toPx()
                    drawRoundRect(
                        ink.copy(alpha = if (i < on) 1f else .38f),
                        Offset(i * (w + gap), (size.height - h) / 2f),
                        Size(w, h),
                        CornerRadius(min(2.dp.toPx(), w / 2f)),
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(clockText(note.shownSeconds), style = MetaStyle, color = ink.copy(alpha = .7f))
                Text(note.at, style = MetaStyle, color = ink.copy(alpha = .7f))
            }
        }
    }
}

private const val Bars = 26

private val MetaStyle =
    TextStyle(fontSize = 10.5.sp, lineHeight = 1.35.em, fontFeatureSettings = "tnum")
