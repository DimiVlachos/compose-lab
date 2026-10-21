package dev.dimvlachos.lab.stickersdemo.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.demo.demoInsets
import dev.dimvlachos.lab.core.platform.rememberReducedMotion
import dev.dimvlachos.lab.core.platform.rememberSoundPlayer
import dev.dimvlachos.lab.core.presentation.components.stickers.StarStickerRating
import dev.dimvlachos.lab.core.presentation.components.stickers.StarStickerState
import dev.dimvlachos.lab.core.presentation.components.stickers.StickerFinger
import dev.dimvlachos.lab.core.presentation.components.stickers.StickerHand
import dev.dimvlachos.lab.core.presentation.components.stickers.StickerInks
import dev.dimvlachos.lab.core.presentation.components.stickers.StickerSounds
import dev.dimvlachos.lab.core.presentation.components.stickers.rememberStarStickerState
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.core.presentation.ui.StarStickerArt
import dev.dimvlachos.lab.core.presentation.ui.StarStickerScreenColors
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.stickers_back
import dev.dimvlachos.lab.resources.stickers_delivered
import dev.dimvlachos.lab.resources.stickers_hint
import dev.dimvlachos.lab.resources.stickers_items
import dev.dimvlachos.lab.resources.stickers_note_hint
import dev.dimvlachos.lab.resources.stickers_note_label
import dev.dimvlachos.lab.resources.stickers_order
import dev.dimvlachos.lab.resources.stickers_question
import dev.dimvlachos.lab.resources.stickers_skip
import dev.dimvlachos.lab.resources.stickers_submit
import dev.dimvlachos.lab.resources.stickers_thanks_many
import dev.dimvlachos.lab.resources.stickers_thanks_one
import dev.dimvlachos.lab.resources.stickers_title
import dev.dimvlachos.lab.resources.stickers_truck
import dev.dimvlachos.lab.resources.stickers_value_many
import dev.dimvlachos.lab.resources.stickers_value_none
import dev.dimvlachos.lab.resources.stickers_value_one
import dev.dimvlachos.lab.resources.stickers_word_1
import dev.dimvlachos.lab.resources.stickers_word_2
import dev.dimvlachos.lab.resources.stickers_word_3
import dev.dimvlachos.lab.resources.stickers_word_4
import dev.dimvlachos.lab.resources.stickers_word_5
import dev.dimvlachos.lab.stickersdemo.StickerDemos
import dev.dimvlachos.lab.stickersdemo.play
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

// Stand-ins the descriptions are fetched with once, and filled in for each score outside
// composition.
private const val CountSlot = "\u0001"
private const val WordSlot = "\u0002"

/** How long the thank-you note stays up after Submit. */
private const val ThanksMs = 2_600L

/**
 * Rating a taco truck order with gold foil star stickers, as the prototype shows it: the order on a
 * card, the stickers under "How was everything?", a note, and Submit once there is a rating. The
 * script's drags and taps go through the same gesture a finger makes.
 */
@Composable
internal fun StickerDemo(
    state: DemoState,
    stickers: StarStickerState = rememberStarStickerState(),
    reducedMotion: Boolean = rememberReducedMotion(),
) {
    val colors =
        if (LabTheme.isDark) StarStickerScreenColors.Dark else StarStickerScreenColors.Light
    val inks = if (LabTheme.isDark) StickerInks.Dusk else StickerInks.Day
    val sounds = rememberSoundPlayer(StickerSounds)
    val hand = remember { StickerHand() }
    val finger = remember { StickerFinger() }
    var thanks by remember { mutableIntStateOf(0) }
    var thanked by remember { mutableIntStateOf(0) }

    DisposableEffect(state, hand) {
        // A step can leave the finger down for the next one, as the drag across the sheet does for
        // the drag back; only a step that is stopped lets go.
        suspend fun step(control: String) {
            try {
                hand.play(control)
            } catch (e: CancellationException) {
                hand.stop()
                throw e
            }
        }
        state.setControlTapHandler { step(it) }
        state.setFeedResetHandler { step(StickerDemos.ClearAll) }
        onDispose {
            state.setControlTapHandler(null)
            state.setFeedResetHandler(null)
        }
    }
    LaunchedEffect(thanks) {
        if (thanks == 0) return@LaunchedEffect
        delay(ThanksMs)
        thanks = 0
    }

    val words =
        listOf(
            stringResource(Res.string.stickers_word_1),
            stringResource(Res.string.stickers_word_2),
            stringResource(Res.string.stickers_word_3),
            stringResource(Res.string.stickers_word_4),
            stringResource(Res.string.stickers_word_5),
        )
    val hint = stringResource(Res.string.stickers_hint)
    val none = stringResource(Res.string.stickers_value_none)
    val oneFormat = stringResource(Res.string.stickers_value_one, WordSlot)
    val manyFormat = stringResource(Res.string.stickers_value_many, CountSlot, WordSlot)
    val question = stringResource(Res.string.stickers_question)
    val thanksOne = stringResource(Res.string.stickers_thanks_one)
    val thanksMany = stringResource(Res.string.stickers_thanks_many, thanked)

    Box(
        Modifier.fillMaxSize().background(colors.background).drawBehind {
            // The window light over the top left, as the prototype's radial-gradient(120% 60% at
            // 15% 0%): an ellipse 1.2 widths across and .6 heights down, gone by 60%.
            val rx = size.width * 1.2f
            val ry = size.height * .6f
            scale(1f, ry / rx, pivot = Offset(size.width * .15f, 0f)) {
                drawCircle(
                    Brush.radialGradient(
                        0f to StarStickerArt.glow,
                        .6f to Color.Transparent,
                        center = Offset(size.width * .15f, 0f),
                        radius = rx,
                    ),
                    radius = rx,
                    center = Offset(size.width * .15f, 0f),
                )
            }
        },
    ) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(demoInsets(state))) {
            AppBar(colors)
            Column(Modifier.weight(1f).padding(start = 18.dp, end = 18.dp, top = 4.dp)) {
                OrderCard(colors)
                // The prototype left all its spare height under the note; here it is shared above
                // the question and below the note, so a tall phone doesn't end in a blank band.
                Spacer(Modifier.heightIn(min = 22.dp).weight(1f))
                Text(
                    question,
                    Modifier.fillMaxWidth().padding(bottom = 2.dp),
                    color = colors.ink,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-.015 * 19).sp,
                )
                Text(
                    stringResource(Res.string.stickers_order),
                    Modifier.fillMaxWidth(),
                    color = colors.muted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                StarStickerRating(
                    stickers,
                    label = { n -> if (n == 0) hint else words[n - 1] },
                    valueText = { n ->
                        when (n) {
                            0 -> none
                            1 -> oneFormat.replace(WordSlot, words[0])
                            else ->
                                manyFormat
                                    .replace(CountSlot, n.toString())
                                    .replace(WordSlot, words[n - 1])
                        }
                    },
                    description = question,
                    labelColor = colors.muted,
                    labelSetColor = colors.accent,
                    inks = inks,
                    sounds = sounds,
                    reducedMotion = reducedMotion,
                    finger = finger,
                    hand = hand,
                )
                Note(colors)
                Spacer(Modifier.weight(1f))
            }
            val rated = stickers.target > 0
            Box(Modifier.padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 22.dp)) {
                Text(
                    stringResource(Res.string.stickers_submit),
                    Modifier.fillMaxWidth()
                        .testTag("submit")
                        .alpha(if (rated) 1f else .35f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.accent)
                        .clickable(enabled = rated, role = Role.Button) {
                            thanked = stickers.target
                            thanks++
                        }
                        .padding(14.dp),
                    color = StarStickerArt.onAccent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
        }
        AnimatedVisibility(
            thanks > 0,
            Modifier.align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = 92.dp),
            enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 6 },
            exit = fadeOut(tween(250)) + slideOutVertically(tween(250)) { it / 6 },
        ) {
            Thanks(if (thanked == 1) thanksOne else thanksMany)
        }
    }
}

@Composable
private fun AppBar(colors: StarStickerScreenColors) {
    val back = stringResource(Res.string.stickers_back)
    Row(
        Modifier.fillMaxWidth().padding(start = 8.dp, end = 14.dp, top = 18.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Canvas(Modifier.size(40.dp).semantics { contentDescription = back }) {
            // The prototype's chevron, M14.5 5.5 8 12l6.5 6.5 in a 24 box, 22 across.
            val k = 22.dp.toPx() / 24f
            val o = (size.width - 22.dp.toPx()) / 2
            val p =
                Path().apply {
                    moveTo(o + 14.5f * k, o + 5.5f * k)
                    lineTo(o + 8f * k, o + 12f * k)
                    lineTo(o + 14.5f * k, o + 18.5f * k)
                }
            drawPath(
                p,
                colors.ink,
                style = Stroke(2.1f * k, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
        Text(
            stringResource(Res.string.stickers_title),
            Modifier.weight(1f),
            color = colors.ink,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-.17).sp,
        )
        Text(
            stringResource(Res.string.stickers_skip),
            Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            color = colors.muted,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun OrderCard(colors: StarStickerScreenColors) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth()
            .shadow(10.dp, shape, ambientColor = colors.cardShadow, spotColor = colors.cardShadow)
            .clip(shape)
            .background(colors.card)
            .drawBehind {
                drawLine(
                    colors.line,
                    Offset(0f, size.height - .5f),
                    Offset(size.width, size.height - .5f),
                    1.dp.toPx(),
                )
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TacoAvatar()
        Column {
            Text(
                stringResource(Res.string.stickers_truck),
                color = colors.ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            for (line in listOf(Res.string.stickers_items, Res.string.stickers_delivered)) {
                Text(
                    stringResource(line),
                    color = colors.muted,
                    fontSize = 12.5.sp,
                    lineHeight = 17.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** The truck's avatar: a taco on an orange tile, the prototype's SVG in a 40 box, 34 across. */
@Composable
private fun TacoAvatar() {
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier.size(50.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(listOf(StarStickerArt.avatarTop, StarStickerArt.avatarBottom)),
            )
            .border(1.dp, StarStickerArt.avatarEdge, shape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(34.dp)) {
            val k = size.width / 40f
            fun shell() =
                Path().apply {
                    moveTo(5 * k, 27 * k)
                    arcTo(
                        androidx.compose.ui.geometry.Rect(5 * k, 12 * k, 35 * k, 42 * k),
                        180f,
                        180f,
                        false,
                    )
                }
            drawPath(shell().apply { close() }, StarStickerArt.tortilla)
            drawPath(shell(), StarStickerArt.tortillaEdge, style = Stroke(1.6f * k))
            val lettuce =
                Path().apply {
                    moveTo(9 * k, 25 * k)
                    cubicTo(11 * k, 19 * k, 15 * k, 16 * k, 20 * k, 16 * k)
                    cubicTo(25 * k, 16 * k, 29 * k, 19 * k, 31 * k, 25 * k)
                }
            drawPath(
                lettuce,
                StarStickerArt.lettuce,
                style = Stroke(3.2f * k, cap = StrokeCap.Round),
            )
            drawCircle(StarStickerArt.tomato, 2 * k, Offset(15 * k, 20.5f * k))
            drawCircle(StarStickerArt.tomato, 2 * k, Offset(22 * k, 18.5f * k))
            drawCircle(StarStickerArt.onion, 1.8f * k, Offset(27.5f * k, 21.5f * k))
            drawLine(
                StarStickerArt.shine,
                Offset(4 * k, 27 * k),
                Offset(36 * k, 27 * k),
                1.6f * k,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun Note(colors: StarStickerScreenColors) {
    var text by rememberSaveable { mutableStateOf("") }
    val shape = RoundedCornerShape(12.dp)
    Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            stringResource(Res.string.stickers_note_label),
            color = colors.muted,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
        )
        BasicTextField(
            text,
            { text = it },
            Modifier.fillMaxWidth()
                .height(64.dp)
                .clip(shape)
                .background(colors.card)
                .border(1.dp, colors.line, shape)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            textStyle = TextStyle(color = colors.ink, fontSize = 14.sp),
            cursorBrush = SolidColor(colors.accent),
            decorationBox = { inner ->
                Box {
                    if (text.isEmpty()) {
                        Text(
                            stringResource(Res.string.stickers_note_hint),
                            color = colors.muted.copy(alpha = .8f),
                            fontSize = 14.sp,
                        )
                    }
                    inner()
                }
            },
        )
    }
}

@Composable
private fun Thanks(text: String) {
    Row(
        Modifier.fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(StarStickerArt.toast)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(16.dp)) {
            val k = size.width / 16f
            val p =
                Path().apply {
                    moveTo(3 * k, 8.5f * k)
                    lineTo(6.2f * k, 11.7f * k)
                    lineTo(13 * k, 4.5f * k)
                }
            drawPath(
                p,
                StarStickerArt.toastCheck,
                style = Stroke(2 * k, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
        Text(text, color = StarStickerArt.toastInk, fontSize = 14.sp)
    }
}
