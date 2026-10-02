package dev.dimvlachos.lab.planedemo.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.paperplane.LetterStage
import dev.dimvlachos.lab.core.presentation.components.paperplane.PaperPlane
import dev.dimvlachos.lab.core.presentation.components.paperplane.PaperPlaneIcon
import dev.dimvlachos.lab.core.presentation.components.paperplane.rememberLetterStream
import dev.dimvlachos.lab.core.presentation.components.paperplane.rememberPaperPlaneState
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.chat_placeholder
import dev.dimvlachos.lab.resources.chat_seed_1
import dev.dimvlachos.lab.resources.chat_seed_2
import dev.dimvlachos.lab.resources.chat_seed_3
import dev.dimvlachos.lab.resources.chat_seed_4
import dev.dimvlachos.lab.resources.chat_send
import kotlin.math.roundToInt
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

private val SendSize = 40.dp
private val IconSize = 26.dp
private val FieldShape = RoundedCornerShape(24.dp)
private val FieldPadding = PaddingValues(start = 18.dp, top = 12.dp, bottom = 12.dp)
// The text stops short of the button, there or not, so nothing typed ever runs under it.
private val ButtonRoom = SendSize + 10.dp
private const val FieldMaxLines = 4

// As each letter goes in, the button takes it with a small gulp.
private const val Gulp = 0.14f

// The scripted finger on the send button: down, a moment's press, and off.
private const val TapDownMs = 90
private const val TapHoldMs = 110L
private const val TapUpMs = 260
private const val TapFillAlpha = 0.28f
private const val TapRingAlpha = 0.7f
private val TapRadius = 18.dp
private val TapRing = 1.5.dp

/**
 * A chat whose messages are sent as paper planes: the send button appears with the first letter
 * typed; sent, the letters go into it one by one, the nearest first, and its paper plane takes off,
 * flies up the conversation and round, and comes in low over the message's place, dropping its
 * letters into it one by one as it passes before it flies on off the screen.
 */
@Composable
internal fun PlaneDemo(state: DemoState) {
    val seed =
        listOf(
            stringResource(Res.string.chat_seed_1) to false,
            stringResource(Res.string.chat_seed_2) to true,
            stringResource(Res.string.chat_seed_3) to false,
            stringResource(Res.string.chat_seed_4) to false,
        )
    val chat = remember { PlaneChat(seed) }
    val field = rememberTextFieldState()
    val plane = rememberPaperPlaneState()
    val letters = rememberLetterStream()
    val list = rememberLazyListState()
    val tap = remember { TapDot() }
    val gulp = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val send: suspend () -> Unit = {
        chat.send(
            field.text.toString(),
            letters,
            plane,
            list,
            cleared = { field.clearText() },
            arrived = {
                scope.launch {
                    gulp.snapTo(1f)
                    gulp.animateTo(
                        0f,
                        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                    )
                }
            },
        )
    }
    DisposableEffect(state, chat) {
        state.setSendHandler { text, typing ->
            val line = getString(text)
            // A character at a time, as a thumb types it.
            val perChar = typing / line.length.coerceAtLeast(1)
            for (count in 1..line.length) {
                field.setTextAndPlaceCursorAtEnd(line.take(count))
                delay(perChar)
            }
            coroutineScope {
                launch { tap.tap() }
                delay(TapDownMs.toLong())
                send()
            }
        }
        state.setClearHandler { chat.clear() }
        onDispose {
            state.setSendHandler(null)
            state.setClearHandler(null)
        }
    }
    val colors = LabTheme.colors
    val spacing = LabTheme.spacing
    Box(Modifier.fillMaxSize().background(colors.background)) {
        Column(
            Modifier.fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.ime
                        .union(WindowInsets.navigationBars)
                        .only(WindowInsetsSides.Bottom)
                )
        ) {
            // Newest at the foot, where the conversation is anchored: a new message pushes the
            // others up.
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                state = list,
                reverseLayout = true,
                contentPadding =
                    PaddingValues(horizontal = spacing.medium, vertical = spacing.small),
                verticalArrangement = Arrangement.Bottom,
            ) {
                items(chat.messages.asReversed(), key = { it.id }) { message ->
                    val inTheAir = message.id in chat.flying
                    // Each message brings its own gap above it, so a place that opens opens with
                    // its gap and nothing moves before it does.
                    Box(
                        Modifier.fillMaxWidth()
                            .animateItem()
                            .then(
                                if (inTheAir) {
                                    Modifier.opening { plane.opening(message.id) }
                                } else {
                                    Modifier
                                }
                            )
                            .padding(top = spacing.small),
                        contentAlignment =
                            if (message.mine) Alignment.CenterEnd else Alignment.CenterStart,
                    ) {
                        ChatBubble(
                            message.text,
                            message.mine,
                            Modifier.widthIn(max = BubbleMaxWidth),
                            delivered =
                                if (inTheAir) {
                                    { plane.delivered(message.id) }
                                } else {
                                    null
                                },
                            laidOut =
                                if (inTheAir) {
                                    { text, at -> chat.slots[message.id] = Slot(text, at) }
                                } else {
                                    null
                                },
                        )
                    }
                }
            }
            MessageField(
                field,
                chat,
                tap,
                gulp.value,
                onSend = { scope.launch { send() } },
                modifier = Modifier.padding(horizontal = spacing.medium, vertical = spacing.small),
            )
        }
        LetterStage(letters, Modifier.fillMaxSize())
        PaperPlane(plane, colors.accent, Modifier.fillMaxSize())
    }
}

// A plain field across the foot of the chat; the send button sits in its right-hand end once
// there is something typed, and stays while letters are still going into it.
@Composable
private fun MessageField(
    field: TextFieldState,
    chat: PlaneChat,
    tap: TapDot,
    gulp: Float,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LabTheme.colors
    val typed = field.text.isNotEmpty()
    val shown by
        animateFloatAsState(
            if (typed || chat.pouring > 0) 1f else 0f,
            spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
        )
    val iconIn by
        animateFloatAsState(
            if (chat.iconAway > 0) 0f else 1f,
            if (chat.iconAway > 0) tween(0) else spring(Spring.DampingRatioMediumBouncy),
        )
    Box(modifier.fillMaxWidth().clip(FieldShape).background(colors.surface)) {
        BasicTextField(
            state = field,
            // Centred in the field, as the button is: one line sits level with the button, and
            // as the text grows the button stays in the middle of it.
            modifier =
                Modifier.align(Alignment.CenterStart)
                    .fillMaxWidth()
                    .padding(FieldPadding)
                    .padding(end = ButtonRoom),
            textStyle = LabTheme.typography.body.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.accent),
            lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = FieldMaxLines),
            onTextLayout = { layout -> chat.draftLayout = layout },
            decorator = { inner ->
                Box(Modifier.onGloballyPositioned { chat.draftAt = it.positionInRoot() }) {
                    // Not while the letters of the last message are still going in.
                    if (field.text.isEmpty() && chat.pouring == 0) {
                        Text(
                            stringResource(Res.string.chat_placeholder),
                            color = colors.textMuted,
                            style = LabTheme.typography.body,
                        )
                    }
                    inner()
                }
            },
        )
        val label = stringResource(Res.string.chat_send)
        Box(
            Modifier.align(Alignment.CenterEnd)
                .padding(4.dp)
                .size(SendSize)
                .graphicsLayer {
                    val s = shown * (1f + Gulp * gulp)
                    scaleX = s
                    scaleY = s
                    alpha = shown.coerceIn(0f, 1f)
                }
                .then(with(tap) { Modifier.drawTap(colors.touch) })
                .clip(CircleShape)
                .clickable(enabled = typed, onClick = onSend)
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) {
            PaperPlaneIcon(
                colors.accent,
                modifier =
                    Modifier.size(IconSize)
                        .onGloballyPositioned {
                            chat.icon = Rect(it.positionInRoot(), it.size.toSize())
                        }
                        .graphicsLayer {
                            scaleX = iconIn
                            scaleY = iconIn
                        },
            )
        }
    }
}

/** The scripted fingertip on the send button, so a clip shows the tap. */
@Stable
private class TapDot {
    private val alpha = Animatable(0f)

    suspend fun tap() {
        try {
            alpha.animateTo(1f, tween(TapDownMs))
            delay(TapHoldMs)
            alpha.animateTo(0f, tween(TapUpMs))
        } finally {
            // Gone at once when the script is stopped mid-tap.
            withContext(NonCancellable) { alpha.snapTo(0f) }
        }
    }

    fun Modifier.drawTap(color: Color): Modifier = drawWithContent {
        drawContent()
        val a = alpha.value
        if (a <= 0f) return@drawWithContent
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(color.copy(alpha = TapFillAlpha * a), TapRadius.toPx(), center)
        drawCircle(
            color.copy(alpha = TapRingAlpha * a),
            TapRadius.toPx(),
            center,
            style = Stroke(TapRing.toPx()),
        )
    }
}

// A message's place opening as its plane comes down to it: laid out its full height, it takes up
// only [open]'s share of it, its foot where the foot of the place will be, so the message lies
// where it will stay while the conversation above it moves up to make room.
private fun Modifier.opening(open: () -> Float): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val height = (placeable.height * open()).roundToInt()
    layout(placeable.width, height) { placeable.place(0, height - placeable.height) }
}
