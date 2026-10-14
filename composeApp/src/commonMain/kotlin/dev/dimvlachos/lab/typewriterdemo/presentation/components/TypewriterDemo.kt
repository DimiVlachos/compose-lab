package dev.dimvlachos.lab.typewriterdemo.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.demo.demoInsets
import dev.dimvlachos.lab.core.platform.Sound
import dev.dimvlachos.lab.core.platform.rememberSoundPlayer
import dev.dimvlachos.lab.core.presentation.components.typewriter.GlyphAtlas
import dev.dimvlachos.lab.core.presentation.components.typewriter.KeyAction
import dev.dimvlachos.lab.core.presentation.components.typewriter.PaperSlip
import dev.dimvlachos.lab.core.presentation.components.typewriter.SentSheet
import dev.dimvlachos.lab.core.presentation.components.typewriter.SlipFlightStage
import dev.dimvlachos.lab.core.presentation.components.typewriter.SlipFlights
import dev.dimvlachos.lab.core.presentation.components.typewriter.TypeKey
import dev.dimvlachos.lab.core.presentation.components.typewriter.TypewriterComposer
import dev.dimvlachos.lab.core.presentation.components.typewriter.TypewriterSounds
import dev.dimvlachos.lab.core.presentation.components.typewriter.TypewriterState
import dev.dimvlachos.lab.core.presentation.components.typewriter.keysFor
import dev.dimvlachos.lab.core.presentation.components.typewriter.rememberGlyphAtlas
import dev.dimvlachos.lab.core.presentation.components.typewriter.rememberTypewriterState
import dev.dimvlachos.lab.core.presentation.components.typewriter.slipLanding
import dev.dimvlachos.lab.core.presentation.components.typewriter.slipTilt
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.core.presentation.ui.isWideStage
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.typewriterchat_badge
import dev.dimvlachos.lab.resources.typewriterchat_contact
import dev.dimvlachos.lab.resources.typewriterchat_contact_status
import dev.dimvlachos.lab.resources.typewriterchat_model
import dev.dimvlachos.lab.resources.typewriterchat_reply_1
import dev.dimvlachos.lab.resources.typewriterchat_reply_2
import dev.dimvlachos.lab.resources.typewriterchat_reply_3
import dev.dimvlachos.lab.resources.typewriterchat_seed_1
import dev.dimvlachos.lab.resources.typewriterchat_seed_2
import dev.dimvlachos.lab.resources.typewriterchat_seed_3
import dev.dimvlachos.lab.resources.typewriterchat_today
import dev.dimvlachos.lab.resources.typewriterchat_typing
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

// The thread keeps to this width in the middle of a wide stage.
private val ThreadMaxWidth = 640.dp

// Side by side, the machine takes this share of the stage's width, and no more than this.
private const val WideMachineShare = 0.5f
private val WideMachineWidth = 440.dp

// A reply's bubble is at most this share of the thread's width, and this round, tailed at its foot
// on the sender's side.
private const val BubbleShare = 0.76f
private val BubbleShape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)
private val AvatarSize = 36.dp

// In a scripted message, this stands for a backspace, so the script can make a typo and fix it.
private const val ScriptBackspace = '⌫'

// A scripted key comes this much sooner or later than an even pace, as a hand types.
private const val TypingJitter = 0.35f

// A sheet landing in the thread clicks this loud.
private const val LandVolume = 0.5f

// A frame longer than this is counted as this long by the scripted typing.
private const val LongestFrameNanos = 50_000_000L

/**
 * A chat with Eleni on Naxos, written on a typewriter typed on its own keys. Every letter strikes
 * the sheet in the roller, the bell rings near the margin and the carriage returns by itself; pull
 * the sheet up out of the roller and it trims down to a paper slip that flies up into the thread.
 * Eleni answers in plain bubbles. The script taps out two messages on the keys, a typo struck out
 * on the way, pulls each out, the first after a pull too short that slides back, and takes them
 * back at the end.
 */
@Composable
internal fun TypewriterDemo(state: DemoState) {
    val sound = rememberSoundPlayer(TypewriterSounds)
    val machine = rememberTypewriterState()
    val flights = remember { SlipFlights() }
    val seed =
        listOf(
            stringResource(Res.string.typewriterchat_seed_1) to false,
            stringResource(Res.string.typewriterchat_seed_2) to true,
            stringResource(Res.string.typewriterchat_seed_3) to false,
        )
    val replies =
        listOf(
            stringResource(Res.string.typewriterchat_reply_1),
            stringResource(Res.string.typewriterchat_reply_2),
            stringResource(Res.string.typewriterchat_reply_3),
        )
    val chat = remember { TypewriterThread(seed, replies) }
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val answers = remember { mutableListOf<Job>() }
    // A replay or a recording is the script's to play: a hand on the keys would type over it.
    val scripted = state.replay || state.recording
    val onSend: (SentSheet) -> Unit = { sheet ->
        val id = chat.nextId()
        // Its flight is under way before its place opens, so the place is never seen empty-handed.
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            flights.fly(id, sheet, slipTilt(id))
            sound.play(Sound.RatchetTick, volume = LandVolume)
            answers += scope.launch { chat.answer() }
        }
        chat.send(sheet.text, sheet.lines)
        list.requestScrollToItem(0)
    }
    // A new message or Eleni typing comes in at the foot: kept in sight if the thread was there,
    // rather than opening under it, as a list anchored to the item it showed would.
    LaunchedEffect(chat, list) {
        snapshotFlow { chat.messages.size to chat.typing }
            .collect { if (list.firstVisibleItemIndex <= 1) list.animateScrollToItem(0) }
    }
    // The script's steps take turns, in the order they started: in the background, where frames
    // stop and so does the typing, a step that comes due waits for the one before it, so the run
    // still types, sends and takes back in order, and ends where it began.
    val turns = remember { Mutex() }
    DisposableEffect(state, machine, chat) {
        state.setSendHandler { text, typing ->
            val line = getString(text)
            turns.withLock { tapOut(machine, line, typing) }
        }
        state.setTypewriterSheetPullHandler { up, duration ->
            turns.withLock { pullTypewriterSheet(machine, up, duration) }
        }
        state.setClearHandler {
            turns.withLock {
                // Once every sheet has landed, so none lands after its thread is put back.
                snapshotFlow { chat.messages.none { flights.flying(it.id) } }.first { it }
                answers.forEach { it.cancel() }
                answers.clear()
                chat.clear()
                machine.field.clearText()
            }
        }
        onDispose {
            state.setSendHandler(null)
            state.setTypewriterSheetPullHandler(null)
            state.setClearHandler(null)
        }
    }
    val colors = LabTheme.colors
    val spacing = LabTheme.spacing
    val insets = demoInsets(state)
    BoxWithConstraints(Modifier.fillMaxSize().background(colors.background)) {
        val unit = machine.unit
        val atlas = rememberGlyphAtlas(unit)
        val side = max(spacing.medium, (maxWidth - ThreadMaxWidth) / 2)
        val bubbleWidth = (maxWidth - side * 2) * BubbleShare
        // Wide, as a phone on its side, the thread and the machine stand side by side, so each
        // keeps its height; otherwise the thread sits over the machine.
        val wide = isWideStage(maxWidth, maxHeight)
        val machineWidth = min(maxWidth * WideMachineShare, WideMachineWidth)
        val thread: @Composable (Modifier) -> Unit = { modifier ->
            // Newest at the foot, just over the paper: a new message pushes the others up.
            LazyColumn(
                modifier,
                state = list,
                reverseLayout = true,
                contentPadding =
                    PaddingValues(
                        horizontal = if (wide) spacing.medium else side,
                        vertical = spacing.small,
                    ),
                verticalArrangement = Arrangement.spacedBy(9.dp, Alignment.Bottom),
            ) {
                if (chat.typing) {
                    item(key = "typing") { Typing(Modifier.animateItem()) }
                }
                items(chat.messages.asReversed(), key = { it.id }) { message ->
                    Message(message, flights, atlas, unit, bubbleWidth, Modifier.animateItem())
                }
                item(key = "today") { Today() }
            }
        }
        val composer: @Composable (Modifier) -> Unit = { modifier ->
            TypewriterComposer(
                machine,
                onSend = onSend,
                sound = sound,
                modifier = modifier,
                badge = stringResource(Res.string.typewriterchat_badge),
                model = stringResource(Res.string.typewriterchat_model),
                enabled = !scripted,
                insets = insets.only(WindowInsetsSides.Bottom),
                atlas = atlas,
            )
        }
        Column(Modifier.fillMaxSize()) {
            Header(Modifier.windowInsetsPadding(insets.only(WindowInsetsSides.Horizontal)))
            if (wide) {
                Row(
                    Modifier.weight(1f)
                        .fillMaxWidth()
                        .windowInsetsPadding(insets.only(WindowInsetsSides.Horizontal)),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    thread(Modifier.weight(1f).fillMaxHeight())
                    composer(Modifier.width(machineWidth))
                }
            } else {
                thread(Modifier.weight(1f).fillMaxWidth())
                composer(Modifier)
            }
        }
        SlipFlightStage(flights, atlas, unit)
    }
}

// A fingertip taps [line] out on the machine's own keys over about [typing], a character at a
// time: the page key and Shift first where a character needs them, Delete for a struck-out letter.
// It waits on frames, not the clock, so in the background, where frames stop, it stops too.
private suspend fun tapOut(machine: TypewriterState, line: String, typing: Duration) {
    val random = Random(line.hashCode())
    val pace = typing / line.length.coerceAtLeast(1)
    val keyboard = machine.keyboard
    for (char in line) {
        awaitFrames(pace * (1f + (random.nextFloat() * 2f - 1f) * TypingJitter).toDouble())
        val keys =
            if (char == ScriptBackspace) listOf(TypeKey(KeyAction.Backspace))
            else keysFor(char, keyboard.figures, keyboard.shifted)
        for ((i, key) in keys.withIndex()) {
            if (i > 0) awaitFrames(pace)
            machine.tap(key)
        }
    }
    // Back to the letters, as a typist turns back after a stop.
    if (keyboard.figures) {
        awaitFrames(pace)
        machine.tap(TypeKey(KeyAction.Page))
    }
}

// Waits [duration] of frames: no time passes while no frames come.
private suspend fun awaitFrames(duration: Duration) {
    val wanted = duration.inWholeNanoseconds
    var passed = 0L
    var last = withFrameNanos { it }
    while (passed < wanted) {
        withFrameNanos { now ->
            // A long gap between frames is the screen having been away: it counts as one frame.
            passed += (now - last).coerceAtMost(LongestFrameNanos)
            last = now
        }
    }
}

// The scripted fingertip pulls the sheet [up] over [duration]; the machine works in units.
private suspend fun pullTypewriterSheet(machine: TypewriterState, up: Dp, duration: Duration) {
    machine.pullScripted(up / machine.unit, duration.inWholeMilliseconds / 1000f)
}

@Composable
private fun Message(
    message: ThreadMessage,
    flights: SlipFlights,
    atlas: GlyphAtlas,
    unit: Dp,
    bubbleWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val colors = LabTheme.colors
    // Each message is read out as it arrives; those the thread began with are simply there.
    val announced = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
    when (message) {
        is ThreadMessage.Slip ->
            Box(modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                PaperSlip(
                    message.lines,
                    message.tilt,
                    atlas,
                    unit,
                    Modifier.slipLanding(flights, message.id).then(announced).clearAndSetSemantics {
                        contentDescription = message.text
                    },
                )
            }
        is ThreadMessage.Reply ->
            Box(modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                Text(
                    message.text,
                    style = LabTheme.typography.body.copy(fontSize = 15.sp, lineHeight = 20.sp),
                    color = colors.textPrimary,
                    modifier =
                        Modifier.widthIn(max = bubbleWidth)
                            .then(announced)
                            .background(colors.surface, BubbleShape)
                            .border(1.dp, colors.chipBorder, BubbleShape)
                            .padding(horizontal = 13.dp, vertical = 9.dp),
                )
            }
    }
}

// Who the chat is with, over the thread.
@Composable
private fun Header(modifier: Modifier = Modifier) {
    val colors = LabTheme.colors
    val spacing = LabTheme.spacing
    val name = stringResource(Res.string.typewriterchat_contact)
    Row(
        modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    colors.chipBorder,
                    Offset(0f, size.height),
                    Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(horizontal = spacing.medium, vertical = spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            Modifier.size(AvatarSize).background(colors.accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                name.take(1),
                color = colors.onAccent,
                style = LabTheme.typography.body.copy(fontWeight = FontWeight.Bold),
            )
        }
        Column {
            Text(
                name,
                style = LabTheme.typography.subtitle,
                color = colors.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(Res.string.typewriterchat_contact_status),
                style = LabTheme.typography.label,
                color = colors.textMuted,
            )
        }
    }
}

@Composable
private fun Today() {
    Box(Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.Center) {
        Text(
            stringResource(Res.string.typewriterchat_today).uppercase(),
            style = LabTheme.typography.label.copy(letterSpacing = 0.08.em),
            color = LabTheme.colors.textMuted,
        )
    }
}

// Three dots that rise and fade in turn while the other side types.
@Composable
private fun Typing(modifier: Modifier = Modifier) {
    val colors = LabTheme.colors
    val label = stringResource(Res.string.typewriterchat_typing)
    val transition = rememberInfiniteTransition()
    val phase by
        transition.animateFloat(0f, 1f, infiniteRepeatable(tween(DotCycleMs), RepeatMode.Restart))
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Row(
            Modifier.semantics { contentDescription = label }
                .background(colors.surface, BubbleShape)
                .border(1.dp, colors.chipBorder, BubbleShape)
                .padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(3) { dot ->
                Box(
                    Modifier.size(6.dp).drawBehind {
                        val t = ((phase - dot * DotLag) % 1f + 1f) % 1f
                        val lift = sin(t * PI).toFloat()
                        drawCircle(
                            colors.textMuted.copy(alpha = 1f - DotFade * lift),
                            center = center - Offset(0f, DotRise.toPx() * lift),
                        )
                    }
                )
            }
        }
    }
}

private const val DotCycleMs = 1000
private const val DotLag = 0.15f
private const val DotFade = 0.5f
private val DotRise = 3.dp

// The thread as it begins, its header over it.
@Composable
private fun TypewriterDemoPreview(dark: Boolean) {
    LabTheme(darkTheme = dark) { TypewriterDemo(DemoState()) }
}

@Preview @Composable private fun TypewriterDemoDarkPreview() = TypewriterDemoPreview(dark = true)

@Preview @Composable private fun TypewriterDemoLightPreview() = TypewriterDemoPreview(dark = false)
