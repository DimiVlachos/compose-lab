package dev.dimvlachos.lab.conchdemo.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.dimvlachos.lab.conchdemo.ConchDemos
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.demo.demoInsets
import dev.dimvlachos.lab.core.platform.rememberReducedMotion
import dev.dimvlachos.lab.core.platform.rememberSoundPlayer
import dev.dimvlachos.lab.core.presentation.components.conch.ConchAnchors
import dev.dimvlachos.lab.core.presentation.components.conch.ConchButton
import dev.dimvlachos.lab.core.presentation.components.conch.ConchEffects
import dev.dimvlachos.lab.core.presentation.components.conch.ConchEvent
import dev.dimvlachos.lab.core.presentation.components.conch.ConchMode
import dev.dimvlachos.lab.core.presentation.components.conch.ConchOverlay
import dev.dimvlachos.lab.core.presentation.components.conch.ConchSounds
import dev.dimvlachos.lab.core.presentation.components.conch.ConchState
import dev.dimvlachos.lab.core.presentation.components.conch.VoiceNote
import dev.dimvlachos.lab.core.presentation.components.conch.VoiceNoteBubble
import dev.dimvlachos.lab.core.presentation.components.conch.clockText
import dev.dimvlachos.lab.core.presentation.components.conch.ease
import dev.dimvlachos.lab.core.presentation.components.conch.seconds
import dev.dimvlachos.lab.core.presentation.components.touch.ScriptedTouch
import dev.dimvlachos.lab.core.presentation.components.touch.drawTouch
import dev.dimvlachos.lab.core.presentation.ui.ConchScreenColors
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.conch_cancel
import dev.dimvlachos.lab.resources.conch_canceled
import dev.dimvlachos.lab.resources.conch_hint
import dev.dimvlachos.lab.resources.conch_message
import dev.dimvlachos.lab.resources.conch_messages
import dev.dimvlachos.lab.resources.conch_msg_bucket
import dev.dimvlachos.lab.resources.conch_msg_swell
import dev.dimvlachos.lab.resources.conch_msg_tide
import dev.dimvlachos.lab.resources.conch_name
import dev.dimvlachos.lab.resources.conch_note_mine
import dev.dimvlachos.lab.resources.conch_note_theirs
import dev.dimvlachos.lab.resources.conch_online
import dev.dimvlachos.lab.resources.conch_pause
import dev.dimvlachos.lab.resources.conch_play
import dev.dimvlachos.lab.resources.conch_record
import dev.dimvlachos.lab.resources.conch_recording
import dev.dimvlachos.lab.resources.conch_release
import dev.dimvlachos.lab.resources.conch_send
import dev.dimvlachos.lab.resources.conch_sent
import dev.dimvlachos.lab.resources.conch_slide
import dev.dimvlachos.lab.resources.conch_today
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/** A line in the chat after the ones it starts with: a sent voice note, or a typed message. */
private sealed interface ChatItem {
    class Voice(val note: VoiceNote) : ChatItem

    class Typed(val text: String) : ChatItem
}

/**
 * The prototype's chat with Maya Torres, whose composer's record button is a pink-lipped conch. The
 * script's fingertip plays her voice note, holds the conch to record and lets go to send, then
 * holds it again and slides off to cancel, through the same state a finger drives.
 */
@Composable
internal fun ConchDemo(
    state: DemoState,
    conch: ConchState = remember { ConchState().apply { reset() } },
    reducedMotion: Boolean = rememberReducedMotion(),
    field: TextFieldState = rememberTextFieldState(),
) {
    val sounds = rememberSoundPlayer(ConchSounds)
    SideEffect { conch.reducedMotion = reducedMotion }
    val extra = remember(conch) { mutableStateListOf<ChatItem>() }
    val anchors = remember(conch) { ConchAnchors(conch) }
    val finger = remember { ScriptFinger() }
    var status by remember { mutableStateOf("") }
    var root by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var mayaBubble by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val recording = stringResource(Res.string.conch_recording)
    val sentTemplate = stringResource(Res.string.conch_sent, Slot)
    val canceled = stringResource(Res.string.conch_canceled)
    val hintText = stringResource(Res.string.conch_hint)
    ConchEffects(conch, sounds) { event, note ->
        when (event) {
            ConchEvent.Start -> status = recording
            ConchEvent.Sent ->
                if (note != null) {
                    extra += ChatItem.Voice(note)
                    status = sentTemplate.replace(Slot, clockText(seconds(note.ms)))
                }
            ConchEvent.Canceled -> status = canceled
            ConchEvent.Hint -> status = hintText
            else -> Unit
        }
    }

    val density = LocalDensity.current.density
    DisposableEffect(state, conch) {
        state.setControlTapHandler { control ->
            val stage = root?.takeIf { it.isAttached } ?: return@setControlTapHandler
            val slot = Offset(conch.slotCenterX * density, conch.slotCenterY * density)
            when (control) {
                ConchDemos.PlayMaya -> {
                    val b = mayaBubble?.takeIf { it.isAttached } ?: return@setControlTapHandler
                    // The middle of the bubble's shell: its border, padding, and half its canvas.
                    val shell = stage.localPositionOf(b, Offset(27f * density, 29f * density))
                    finger.appear(Offset(stage.size.width * .72f, stage.size.height * .55f))
                    finger.glide(shell, 600)
                    delay(ScriptedTouch.TapHoldMs)
                    conch.notes.firstOrNull { !it.mine }?.let { conch.togglePlay(it) }
                }
                ConchDemos.Hold -> {
                    finger.appear(slot)
                    finger.glide(slot, if ((finger.at - slot).getDistance() < 1f) 400 else 700)
                    conch.startRec()
                }
                ConchDemos.Slide -> {
                    finger.slide(slot, -ConchDemos.SlideBy * density, 520) { dx ->
                        conch.dragTo(dx / density)
                    }
                }
                ConchDemos.Release -> conch.finishRec()
            }
        }
        // Takes the sent note back out and puts everything as it began, so the clip loops.
        state.setFeedResetHandler {
            finger.hide()
            if (conch.mode == ConchMode.Rec) conch.cancelRec()
            conch.reset()
            extra.clear()
            field.clearText()
            status = ""
        }
        onDispose {
            state.setControlTapHandler(null)
            state.setFeedResetHandler(null)
        }
    }

    val colors = if (LabTheme.isDark) ConchScreenColors.Dark else ConchScreenColors.Light
    val touch = LabTheme.colors.touch
    Box(
        Modifier.fillMaxSize().background(colors.background).onGloballyPositioned {
            root = it
            anchors.stage(it, density)
        },
    ) {
        Column(
            Modifier.fillMaxSize()
                .windowInsetsPadding(
                    demoInsets(state).union(WindowInsets.ime.only(WindowInsetsSides.Bottom)),
                ),
        ) {
            AppBar(colors)
            Chat(
                conch,
                extra,
                anchors,
                colors,
                onMaya = { mayaBubble = it },
                modifier = Modifier.weight(1f),
            )
            Composer(conch, anchors, colors, field, reducedMotion) {
                extra += ChatItem.Typed(it)
            }
        }
        ConchOverlay(conch, colors)
        Box(
            Modifier.size(1.dp).semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = status
            },
        )
        Canvas(Modifier.fillMaxSize()) { drawTouch(touch, finger.at, finger.alpha.value) }
    }
}

private const val Slot = "\u0000"

/** The chat's bar: back, Maya's avatar, her name and that she is online, and a call button. */
@Composable
private fun AppBar(colors: ConchScreenColors) {
    Row(
        Modifier.fillMaxWidth()
            .drawBehind {
                val w = 1.dp.toPx()
                drawRect(colors.line, Offset(0f, size.height - w), Size(size.width, w))
            }
            .padding(start = 4.dp, end = 10.dp, top = 14.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // The chat's own icons, drawn as the prototype's; the demo screen's bar goes back.
        BarIcon(BackPath, 2.1f, colors.ink)
        Box(
            Modifier.size(36.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFFE08A5C), Color(0xFFC4566A)))),
            contentAlignment = Alignment.Center,
        ) {
            Text("MT", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(Res.string.conch_name),
                color = colors.ink,
                fontSize = 15.5.sp,
                lineHeight = 1.2.em,
                fontWeight = FontWeight(650),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(Res.string.conch_online),
                color = colors.muted,
                fontSize = 12.sp,
                lineHeight = BodyLine,
            )
        }
        BarIcon(CallPath, 1.8f, colors.ink)
    }
}

/**
 * A 22 dp icon from the prototype's 24-unit SVG path, stroked [width] units wide, in a 44 dp box.
 */
@Composable
private fun BarIcon(path: androidx.compose.ui.graphics.Path, width: Float, ink: Color) {
    Canvas(Modifier.size(44.dp).clearAndSetSemantics {}) {
        val s = 22.dp.toPx() / 24f
        translate((size.width - 22.dp.toPx()) / 2f, (size.height - 22.dp.toPx()) / 2f) {
            scale(s, s, pivot = Offset.Zero) {
                drawPath(
                    path,
                    ink,
                    style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
    }
}

private val BackPath = PathParser().parsePathString("M14.5 5.5 8 12l6.5 6.5").toPath()

private val CallPath =
    PathParser()
        .parsePathString(
            "M6.6 4.5h2.6l1.3 3.6-1.7 1.2a10 10 0 0 0 5.9 5.9l1.2-1.7 3.6 1.3v2.6a1.6 1.6 0 0 " +
                "1-1.7 1.6A14.4 14.4 0 0 1 5 6.2 1.6 1.6 0 0 1 6.6 4.5Z",
        )
        .toPath()

/** The conversation, kept scrolled to its newest line at the foot of the screen. */
@Composable
private fun Chat(
    conch: ConchState,
    extra: List<ChatItem>,
    anchors: ConchAnchors,
    colors: ConchScreenColors,
    onMaya: (LayoutCoordinates) -> Unit,
    modifier: Modifier = Modifier,
) {
    val messages = stringResource(Res.string.conch_messages)
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val scroll = rememberScrollState()
        LaunchedEffect(scroll) { snapshotFlow { scroll.maxValue }.collect { scroll.scrollTo(it) } }
        val most = (maxWidth - 24.dp) * .8f
        val tall = maxHeight
        Column(
            Modifier.fillMaxSize().verticalScroll(scroll).semantics {
                contentDescription = messages
            },
        ) {
            Column(
                Modifier.fillMaxWidth()
                    .heightIn(min = tall)
                    .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom),
            ) {
                Text(
                    stringResource(Res.string.conch_today),
                    Modifier.align(Alignment.CenterHorizontally)
                        .padding(top = 2.dp, bottom = 6.dp)
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                    color = colors.muted,
                    fontSize = 11.5.sp,
                    lineHeight = BodyLine,
                )
                TextBubble(
                    stringResource(Res.string.conch_msg_tide),
                    "5:12 PM",
                    false,
                    most,
                    colors,
                )
                TextBubble(
                    stringResource(Res.string.conch_msg_bucket),
                    "5:14 PM",
                    true,
                    most,
                    colors,
                )
                TextBubble(
                    stringResource(Res.string.conch_msg_swell),
                    "5:15 PM",
                    false,
                    most,
                    colors,
                )
                conch.notes
                    .firstOrNull { !it.mine }
                    ?.let { maya ->
                        Voice(maya, conch, anchors, colors, Modifier.onGloballyPositioned(onMaya))
                    }
                for (item in extra) {
                    when (item) {
                        is ChatItem.Voice -> Voice(item.note, conch, anchors, colors)
                        is ChatItem.Typed -> TextBubble(item.text, SentAt, true, most, colors)
                    }
                }
            }
        }
    }
}

/** When this screen's sent messages say they were sent: just after Maya's voice note. */
private const val SentAt = "5:21 PM"

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.Voice(
    note: VoiceNote,
    conch: ConchState,
    anchors: ConchAnchors,
    colors: ConchScreenColors,
    modifier: Modifier = Modifier,
) {
    val length = clockText(seconds(note.ms))
    val action = stringResource(if (note.playing) Res.string.conch_pause else Res.string.conch_play)
    val label =
        stringResource(
            if (note.mine) Res.string.conch_note_mine else Res.string.conch_note_theirs,
            length,
            action,
        )
    VoiceNoteBubble(
        note,
        conch,
        anchors,
        colors,
        label,
        modifier
            .align(if (note.mine) Alignment.End else Alignment.Start)
            .testTag(
                if (note.mine) "note-mine" else "note-maya",
            ),
    )
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.TextBubble(
    text: String,
    time: String,
    mine: Boolean,
    most: androidx.compose.ui.unit.Dp,
    colors: ConchScreenColors,
) {
    val shape =
        RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 18.dp,
            bottomEnd = if (mine) 6.dp else 18.dp,
            bottomStart = if (mine) 18.dp else 6.dp,
        )
    val ink = if (mine) colors.mineInk else colors.ink
    Column(
        Modifier.align(if (mine) Alignment.End else Alignment.Start)
            .widthIn(max = most)
            .clip(shape)
            .background(if (mine) colors.mine else colors.card)
            .border(1.dp, if (mine) Color.Transparent else colors.line, shape)
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 7.dp),
    ) {
        Text(text, color = ink, fontSize = 14.5.sp, lineHeight = MsgLine)
        Text(
            time,
            Modifier.align(Alignment.End).padding(top = 2.dp),
            color = ink.copy(alpha = .62f),
            fontSize = 10.5.sp,
            lineHeight = MsgLine,
        )
    }
}

/**
 * The composer: the message field, where the recording's timer and "Slide to cancel" take the
 * placeholder's place one after the other (never both at once), the conch's button, and the hint
 * beside it, inside the composer so it never covers the chat.
 */
@Composable
private fun Composer(
    conch: ConchState,
    anchors: ConchAnchors,
    colors: ConchScreenColors,
    field: TextFieldState,
    reducedMotion: Boolean,
    onSend: (String) -> Unit,
) {
    val rec = conch.mode == ConchMode.Rec
    val ms = if (reducedMotion) 0 else 150
    // The outgoing text fades first, then the incoming one.
    val inputAlpha =
        animateFloatAsState(if (rec) 0f else 1f, tween(ms, if (rec) 0 else ms), label = "input")
    val recAlpha =
        animateFloatAsState(if (rec) 1f else 0f, tween(ms, if (rec) ms else 0), label = "rec")
    Box(
        Modifier.fillMaxWidth()
            .onGloballyPositioned { anchors.composer(it) }
            .background(colors.background)
            .drawBehind { drawRect(colors.line, Offset.Zero, Size(size.width, 1.dp.toPx())) },
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 10.dp, top = 9.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val shape = RoundedCornerShape(22.dp)
            Box(
                Modifier.weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(shape)
                    .background(colors.card)
                    .border(1.dp, colors.line, shape)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Input(field, colors, inputAlpha, onSend)
                RecInfo(conch, colors, recAlpha, reducedMotion)
            }
            ConchButton(
                conch,
                anchors,
                colors,
                label = stringResource(Res.string.conch_record),
                sendLabel = stringResource(Res.string.conch_send),
                cancelLabel = stringResource(Res.string.conch_cancel),
            )
        }
        Hint(
            conch.hint,
            colors,
            reducedMotion,
            Modifier.align(Alignment.TopEnd).padding(top = 20.dp, end = 72.dp),
        )
    }
}

@Composable
private fun Input(
    field: TextFieldState,
    colors: ConchScreenColors,
    alpha: State<Float>,
    onSend: (String) -> Unit,
) {
    val placeholder = stringResource(Res.string.conch_message)
    val style = TextStyle(color = colors.ink, fontSize = 16.sp)
    BasicTextField(
        state = field,
        modifier =
            Modifier.fillMaxWidth()
                .graphicsLayer { this.alpha = alpha.value }
                .semantics { contentDescription = placeholder }
                .testTag("message"),
        textStyle = style,
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        onKeyboardAction = {
            val text = field.text.toString().trim()
            if (text.isNotEmpty()) {
                onSend(text)
                field.clearText()
            }
        },
        cursorBrush = SolidColor(colors.ink),
        decorator = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (field.text.isEmpty())
                    Text(placeholder, style = style.copy(color = colors.muted))
                inner()
            }
        },
    )
}

/** The recording's line: a blinking red dot, the time so far, and how to cancel. */
@Composable
private fun RecInfo(
    conch: ConchState,
    colors: ConchScreenColors,
    alpha: State<Float>,
    reducedMotion: Boolean,
) {
    val armed = conch.armed
    Row(
        Modifier.fillMaxWidth().graphicsLayer { this.alpha = alpha.value }.clearAndSetSemantics {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Dot(colors.rec, blinking = conch.mode == ConchMode.Rec && !reducedMotion)
        Text(
            clockText(conch.recSeconds),
            color = colors.ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            style = TextStyle(fontFeatureSettings = "tnum"),
            maxLines = 1,
        )
        Spacer(Modifier.weight(1f))
        Text(
            stringResource(if (armed) Res.string.conch_release else Res.string.conch_slide),
            color = if (armed) colors.rec else colors.muted,
            fontSize = 14.sp,
            fontWeight = if (armed) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Dot(color: Color, blinking: Boolean) {
    val a =
        if (blinking) {
            // The CSS blink: down to a quarter at half a second, and back.
            rememberInfiniteTransition(label = "blink")
                .animateFloat(
                    1f,
                    1f,
                    infiniteRepeatable(
                        keyframes {
                            durationMillis = 1000
                            .25f at 500
                        },
                    ),
                    label = "dot",
                )
        } else null
    Canvas(Modifier.size(9.dp)) { drawCircle(color.copy(alpha = a?.value ?: 1f)) }
}

/** "Hold the shell to record", floating beside the conch with its arrow pointing at it. */
@Composable
private fun Hint(
    visible: Boolean,
    colors: ConchScreenColors,
    reducedMotion: Boolean,
    modifier: Modifier,
) {
    val shown =
        animateFloatAsState(
            if (visible) 1f else 0f,
            if (reducedMotion) snap() else tween(200),
            label = "hint",
        )
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier
            .graphicsLayer {
                alpha = shown.value
                translationX = (1f - shown.value) * 6.dp.toPx()
            }
            .clearAndSetSemantics {}
            .dropShadow(
                shape,
                Shadow(14.dp, Color(0f, 0f, 0f, .22f), offset = DpOffset(0.dp, 4.dp)),
            )
            .drawBehind {
                // The arrow: a 10 dp square turned 45°, half out of the right edge.
                val h = 10.dp.toPx()
                rotate(45f, pivot = Offset(size.width, size.height / 2f)) {
                    drawRoundRect(
                        colors.ink,
                        Offset(size.width - h / 2f, size.height / 2f - h / 2f),
                        Size(h, h),
                        CornerRadius(2.dp.toPx()),
                    )
                }
            }
            .background(colors.ink, shape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            stringResource(Res.string.conch_hint),
            color = colors.background,
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
            maxLines = 1,
        )
    }
}

/**
 * The scripted fingertip: it comes down, glides to each target as the prototype's does (eased, in a
 * straight line), holds and slides. Nothing but the script moves it.
 */
@Stable
private class ScriptFinger {
    val alpha = Animatable(0f)
    var at by mutableStateOf(Offset.Zero)
        private set

    private var placed = false

    suspend fun appear(from: Offset) {
        if (!placed) {
            at = from
            placed = true
        }
        if (alpha.value < 1f) alpha.animateTo(1f, tween(ScriptedTouch.DownMs))
    }

    suspend fun glide(to: Offset, ms: Int) {
        val from = at
        Animatable(0f).animateTo(1f, tween(ms, easing = Eased)) {
            at = from + (to - from) * value
        }
    }

    /** Slides [by] px across from [from] in [ms], telling [onMove] how far it has gone. */
    suspend fun slide(from: Offset, by: Float, ms: Int, onMove: (Float) -> Unit) {
        Animatable(0f).animateTo(1f, tween(ms, easing = Eased)) {
            val dx = by * value
            at = Offset(from.x + dx, from.y)
            onMove(dx)
        }
    }

    suspend fun hide() {
        placed = false
        alpha.animateTo(0f, tween(ScriptedTouch.UpMs))
    }
}

/** The prototype's ease: quadratic in and out. */
private val Eased = Easing { ease(it) }

private val BodyLine = 1.55.em
private val MsgLine = 1.35.em
