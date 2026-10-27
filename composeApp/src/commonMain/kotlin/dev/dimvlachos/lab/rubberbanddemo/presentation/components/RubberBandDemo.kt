package dev.dimvlachos.lab.rubberbanddemo.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.platform.rememberReducedMotion
import dev.dimvlachos.lab.core.platform.rememberSoundPlayer
import dev.dimvlachos.lab.core.presentation.components.rubberband.RubberBandChat
import dev.dimvlachos.lab.core.presentation.components.rubberband.RubberBandSounds
import dev.dimvlachos.lab.core.presentation.components.rubberband.RubberBandState
import dev.dimvlachos.lab.core.presentation.components.rubberband.RubberBandText
import dev.dimvlachos.lab.core.presentation.components.rubberband.Who
import dev.dimvlachos.lab.core.presentation.components.touch.drawTouch
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.core.presentation.ui.RubberBandScreenColors
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.rubberband_active
import dev.dimvlachos.lab.resources.rubberband_caption_pull
import dev.dimvlachos.lab.resources.rubberband_caption_rest
import dev.dimvlachos.lab.resources.rubberband_day
import dev.dimvlachos.lab.resources.rubberband_delivered
import dev.dimvlachos.lab.resources.rubberband_delivered_said
import dev.dimvlachos.lab.resources.rubberband_initials
import dev.dimvlachos.lab.resources.rubberband_message_1
import dev.dimvlachos.lab.resources.rubberband_message_2
import dev.dimvlachos.lab.resources.rubberband_message_3
import dev.dimvlachos.lab.resources.rubberband_message_4
import dev.dimvlachos.lab.resources.rubberband_name
import dev.dimvlachos.lab.resources.rubberband_placeholder
import dev.dimvlachos.lab.resources.rubberband_send
import dev.dimvlachos.lab.resources.rubberband_sent_said
import dev.dimvlachos.lab.resources.rubberband_typing
import dev.dimvlachos.lab.resources.rubberband_unsend
import dev.dimvlachos.lab.resources.rubberband_unsent_said
import dev.dimvlachos.lab.rubberbanddemo.RubberBandHand
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.stringResource

/** The prototype's opening thread with Jess. */
@Composable
internal fun rememberRubberBandChat(): RubberBandState {
    val opening =
        listOf(
            Who.Them to stringResource(Res.string.rubberband_message_1),
            Who.Me to stringResource(Res.string.rubberband_message_2),
            Who.Them to stringResource(Res.string.rubberband_message_3),
            Who.Me to stringResource(Res.string.rubberband_message_4),
        )
    return remember { RubberBandState(opening) }
}

/**
 * The prototype's chat with Jess Parker, where a message just sent is still tied back to the text
 * box by a rubber band. The script types, sends, pulls a message back, fixes it and sends again
 * through the same field and band a finger and the keyboard use.
 */
@Composable
internal fun RubberBandDemo(
    state: DemoState,
    chat: RubberBandState = rememberRubberBandChat(),
    field: TextFieldState = rememberTextFieldState(),
    reducedMotion: Boolean = rememberReducedMotion(),
) {
    val sounds = rememberSoundPlayer(RubberBandSounds)
    val hand = remember(chat, field) { RubberBandHand(chat, field) }
    // The script's moves take turns: on a slow device one can run past its slot, and the next
    // waits for it rather than steering the hand at the same time.
    val turn = remember { Mutex() }
    DisposableEffect(state, hand) {
        suspend fun step(block: suspend () -> Unit) {
            try {
                block()
            } catch (e: CancellationException) {
                hand.stop()
                throw e
            }
        }
        state.setControlTapHandler { control -> turn.withLock { step { hand.play(control) } } }
        state.setFeedResetHandler { turn.withLock { step { hand.fadeBack() } } }
        onDispose {
            state.setControlTapHandler(null)
            state.setFeedResetHandler(null)
        }
    }
    // A script the user takes over leaves no fingertip behind.
    LaunchedEffect(state.replay) { if (!state.replay) hand.stop() }

    val dark = LabTheme.isDark
    val colors = if (dark) RubberBandScreenColors.Dark else RubberBandScreenColors.Light
    val text =
        RubberBandText(
            placeholder = stringResource(Res.string.rubberband_placeholder),
            send = stringResource(Res.string.rubberband_send),
            unsend = stringResource(Res.string.rubberband_unsend),
            captionPull = stringResource(Res.string.rubberband_caption_pull),
            captionRest = stringResource(Res.string.rubberband_caption_rest),
            day = stringResource(Res.string.rubberband_day),
            delivered = stringResource(Res.string.rubberband_delivered),
            sent = stringResource(Res.string.rubberband_sent_said),
            deliveredSaid = stringResource(Res.string.rubberband_delivered_said),
            unsentSaid = stringResource(Res.string.rubberband_unsent_said),
            typing = stringResource(Res.string.rubberband_typing),
        )
    val touch = LabTheme.colors.touch
    // The sides and the foot, but not the keyboard: the chat pads for that itself.
    val insets =
        if (state.recording) WindowInsets(0)
        else
            WindowInsets.systemBars
                .union(WindowInsets.displayCutout)
                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
    Box(Modifier.fillMaxSize().background(colors.background).windowInsetsPadding(insets)) {
        RubberBandChat(
            chat,
            field,
            text,
            colors,
            dark,
            sounds,
            modifier = Modifier.fillMaxSize(),
            reducedMotion = reducedMotion,
            appBar = { AppBar(colors) },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val a = hand.shown
                if (a <= 0f) return@Canvas
                val at = hand.at * density
                val k = if (hand.down) .82f else 1f
                scale(k, k, pivot = at) { drawTouch(touch, at, a) }
            }
        }
    }
}

/** The chat's bar: back, Jess's avatar, name and that she is active, and call and video. */
@Composable
private fun AppBar(colors: RubberBandScreenColors) {
    Row(
        Modifier.fillMaxWidth()
            .background(colors.bar)
            .drawBehind {
                val w = 1.dp.toPx()
                drawRect(
                    colors.line,
                    topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - w),
                    size = Size(size.width, w),
                )
            }
            .padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(BackPath, colors.muted, 22, 2.2f, Modifier.clearAndSetSemantics {})
        Box(
            Modifier.size(36.dp).background(colors.avatar, CircleShape).clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                stringResource(Res.string.rubberband_initials),
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                color = { Color.White },
            )
        }
        Column {
            BasicText(
                stringResource(Res.string.rubberband_name),
                Modifier.semantics { heading() },
                style =
                    TextStyle(
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight(650),
                        lineHeight = 1.15.em,
                        lineHeightStyle = CssLines,
                    ),
                color = { colors.ink },
                maxLines = 1,
            )
            BasicText(
                stringResource(Res.string.rubberband_active),
                style =
                    TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 1.55.em,
                        lineHeightStyle = CssLines,
                    ),
                color = { colors.active },
                maxLines = 1,
            )
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(PhonePath, colors.muted, 21, 1.9f)
            Icon(VideoPath, colors.muted, 21, 1.9f)
        }
    }
}

/** One of the bar's line icons: an SVG path in a 24 box, stroked round. */
@Composable
private fun Icon(
    path: String,
    color: Color,
    size: Int,
    width: Float,
    modifier: Modifier = Modifier,
) {
    val p = remember(path) { PathParser().parsePathString(path).toPath() }
    Canvas(modifier.size(size.dp)) {
        val s = this.size.width / 24f
        scale(s, s, pivot = androidx.compose.ui.geometry.Offset.Zero) {
            drawPath(
                p,
                color,
                style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

private const val BackPath = "M15 5l-7 7 7 7"
private const val PhonePath =
    "M6.6 3.5h2.6l1.4 4-2 1.4a12 12 0 0 0 6.5 6.5l1.4-2 4 1.4v2.6a2 2 0 0 1-2.2 2A16.5 16.5 0 0 1 4.6 5.7a2 2 0 0 1 2-2.2z"
private const val VideoPath =
    "M5.5 6h7.5a2.5 2.5 0 0 1 2.5 2.5v7a2.5 2.5 0 0 1-2.5 2.5h-7.5A2.5 2.5 0 0 1 3 15.5v-7A2.5 2.5 0 0 1 5.5 6zM15.5 10.5 21 7.5v9l-5.5-3"
private val CssLines = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)
