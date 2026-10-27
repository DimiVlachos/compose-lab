package dev.dimvlachos.lab.decoderringdemo.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.demo.demoInsets
import dev.dimvlachos.lab.core.platform.rememberReducedMotion
import dev.dimvlachos.lab.core.platform.rememberSoundPlayer
import dev.dimvlachos.lab.core.presentation.components.decoderring.DecoderMessage
import dev.dimvlachos.lab.core.presentation.components.decoderring.DecoderRingButton
import dev.dimvlachos.lab.core.presentation.components.decoderring.DecoderRingEffects
import dev.dimvlachos.lab.core.presentation.components.decoderring.DecoderRingSounds
import dev.dimvlachos.lab.core.presentation.components.decoderring.DecoderRingState
import dev.dimvlachos.lab.core.presentation.components.touch.drawTouch
import dev.dimvlachos.lab.core.presentation.ui.DecoderRingScreenColors
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.decoderringdemo.DecoderRingHand
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.decoderring_composer
import dev.dimvlachos.lab.resources.decoderring_dave
import dev.dimvlachos.lab.resources.decoderring_dave_says
import dev.dimvlachos.lab.resources.decoderring_dave_time
import dev.dimvlachos.lab.resources.decoderring_english
import dev.dimvlachos.lab.resources.decoderring_from_spanish
import dev.dimvlachos.lab.resources.decoderring_group
import dev.dimvlachos.lab.resources.decoderring_group_meta
import dev.dimvlachos.lab.resources.decoderring_landed_english
import dev.dimvlachos.lab.resources.decoderring_landed_spanish
import dev.dimvlachos.lab.resources.decoderring_lucia
import dev.dimvlachos.lab.resources.decoderring_lucia_time
import dev.dimvlachos.lab.resources.decoderring_marcy
import dev.dimvlachos.lab.resources.decoderring_marcy_says
import dev.dimvlachos.lab.resources.decoderring_marcy_time
import dev.dimvlachos.lab.resources.decoderring_mine_says
import dev.dimvlachos.lab.resources.decoderring_show_original
import dev.dimvlachos.lab.resources.decoderring_spanish
import dev.dimvlachos.lab.resources.decoderring_today
import dev.dimvlachos.lab.resources.decoderring_translate
import dev.dimvlachos.lab.resources.decoderring_translated
import dev.dimvlachos.lab.resources.decoderring_twist
import dev.dimvlachos.lab.resources.decoderring_twist_back
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import org.jetbrains.compose.resources.stringResource

/**
 * The prototype's neighborhood chat: Lucía posts in Spanish, and the Translate button under her
 * message is a brass decoder ring. The script's twists go through the same gesture a finger makes.
 */
@Composable
internal fun DecoderRingDemo(
    state: DemoState,
    ring: DecoderRingState = remember { DecoderRingState() },
    reducedMotion: Boolean = rememberReducedMotion(),
) {
    val sounds = rememberSoundPlayer(DecoderRingSounds)
    SideEffect { ring.reducedMotion = reducedMotion }
    DecoderRingEffects(ring, sounds)
    val density = LocalDensity.current.density
    // Where the screen and the ring are, read when the hand needs them, never recomposing.
    val places = remember { arrayOfNulls<LayoutCoordinates>(2) }
    val hand =
        remember(ring) {
            DecoderRingHand(
                ring,
                ring = {
                    val root = places[0]?.takeIf { it.isAttached }
                    val r = places[1]?.takeIf { it.isAttached }
                    if (root == null || r == null) {
                        null
                    } else {
                        val c =
                            root.localPositionOf(r, Offset(r.size.width / 2f, r.size.height / 2f))
                        c / density
                    }
                },
                screen = {
                    places[0]
                        ?.takeIf { it.isAttached }
                        ?.let {
                            Offset(it.size.width / density, it.size.height / density)
                        }
                },
            )
        }
    DisposableEffect(state, hand) {
        suspend fun step(block: suspend () -> Unit) {
            try {
                block()
            } catch (e: CancellationException) {
                hand.stop()
                throw e
            }
        }
        state.setControlTapHandler { control -> step { hand.play(control) } }
        // Back in Spanish as it began, so the clip loops.
        state.setFeedResetHandler {
            step { hand.hide() }
            if (ring.english || ring.g != 0f || ring.target != null) ring.reset()
        }
        onDispose {
            state.setControlTapHandler(null)
            state.setFeedResetHandler(null)
        }
    }
    // A script the user takes over leaves no fingertip and no twist behind.
    LaunchedEffect(state.replay) { if (!state.replay) hand.stop() }

    val dark = LabTheme.isDark
    val colors = if (dark) DecoderRingScreenColors.Dark else DecoderRingScreenColors.Light
    val touch = LabTheme.colors.touch
    Box(
        Modifier.fillMaxSize().background(colors.background).onGloballyPositioned {
            places[0] = it
        },
    ) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(demoInsets(state))) {
            AppBar(colors)
            // The thread is anchored to the bottom, above the composer, as a real chat is: a long
            // one is cut off at the top.
            Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
                Column(
                    Modifier.align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .wrapContentHeight(Alignment.Bottom, unbounded = true)
                        .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DayChip(colors)
                    Message(
                        stringResource(Res.string.decoderring_dave),
                        stringResource(Res.string.decoderring_dave_time),
                        "DK",
                        DecoderRingScreenColors.Dave,
                        colors,
                    ) {
                        Bubble(stringResource(Res.string.decoderring_dave_says), colors)
                    }
                    MyBubble(stringResource(Res.string.decoderring_mine_says), colors)
                    Message(
                        stringResource(Res.string.decoderring_lucia),
                        stringResource(Res.string.decoderring_lucia_time),
                        "LR",
                        DecoderRingScreenColors.Lucia,
                        colors,
                    ) {
                        Translated(ring, colors, dark) { places[1] = it }
                    }
                    Message(
                        stringResource(Res.string.decoderring_marcy),
                        stringResource(Res.string.decoderring_marcy_time),
                        "ML",
                        DecoderRingScreenColors.Marcy,
                        colors,
                    ) {
                        Bubble(stringResource(Res.string.decoderring_marcy_says), colors)
                    }
                }
            }
            Composer(colors)
        }
        Canvas(Modifier.fillMaxSize()) {
            val a = hand.shown
            if (a <= 0f) return@Canvas
            val at = hand.at * density
            val k = if (hand.down) .82f else 1f
            scale(k, k, pivot = at) { drawTouch(touch, at, a) }
        }
    }
}

/** Lucía's message and the ring under it, with what it does next and what is shown. */
@Composable
private fun Translated(
    ring: DecoderRingState,
    colors: DecoderRingScreenColors,
    dark: Boolean,
    onRing: (LayoutCoordinates) -> Unit,
) {
    val english = ring.english
    val shape = RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp)
    DecoderMessage(
        ring,
        stringResource(Res.string.decoderring_spanish),
        stringResource(Res.string.decoderring_english),
        colors.ink,
        dark,
        Modifier.fillMaxWidth(.86f),
        Modifier.testTag("message")
            .clip(shape)
            .background(colors.card)
            .border(1.dp, colors.line, shape),
    )
    Row(
        Modifier.padding(top = 5.dp).offset(x = (-6).dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        val sub =
            stringResource(
                if (english) Res.string.decoderring_translated
                else Res.string.decoderring_from_spanish,
            )
        DecoderRingButton(
            ring,
            label =
                stringResource(
                    if (english) Res.string.decoderring_show_original
                    else Res.string.decoderring_translate,
                ),
            description = sub,
            focusRing = colors.accent,
            modifier = Modifier.onGloballyPositioned(onRing),
        )
        Column(Modifier.clearAndSetSemantics {}) {
            BasicText(
                stringResource(
                    if (english) Res.string.decoderring_twist_back
                    else Res.string.decoderring_twist,
                ),
                style = css(13f, FontWeight(650), 1.2f).copy(color = colors.ink),
            )
            BasicText(
                sub,
                style = css(11.5f, FontWeight.Medium, 1.2f).copy(color = colors.muted),
            )
        }
    }
    // A polite live region says which language landed.
    val said =
        if (!ring.announced) ""
        else
            stringResource(
                if (english) Res.string.decoderring_landed_english
                else Res.string.decoderring_landed_spanish,
            )
    Box(
        Modifier.size(1.dp).testTag("live").semantics {
            liveRegion = LiveRegionMode.Polite
            contentDescription = said
        },
    )
}

/** The chat's bar: back, the group's badge, its name, and who and where it is. */
@Composable
private fun AppBar(colors: DecoderRingScreenColors) {
    Row(
        Modifier.fillMaxWidth()
            .drawBehind {
                val w = 1.dp.toPx()
                drawRect(colors.line, Offset(0f, size.height - w), size.copy(height = w))
            }
            .padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Canvas(Modifier.size(22.dp).clearAndSetSemantics {}) {
            // M15 5l-7 7 7 7 in a 24 box.
            val s = size.width / 24f
            val p =
                Path().apply {
                    moveTo(15f * s, 5f * s)
                    lineTo(8f * s, 12f * s)
                    lineTo(15f * s, 19f * s)
                }
            drawPath(
                p,
                colors.muted,
                style = Stroke(2.2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
        Box(
            Modifier.size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .drawBehind { drawGroupBadge() }
                .clearAndSetSemantics {},
        )
        Column(Modifier.weight(1f)) {
            BasicText(
                stringResource(Res.string.decoderring_group),
                Modifier.semantics { heading() },
                style =
                    css(17f, FontWeight.Bold, 1.15f)
                        .copy(color = colors.ink, letterSpacing = (-.01).em),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                stringResource(Res.string.decoderring_group_meta),
                style = css(12f, FontWeight.Medium).copy(color = colors.muted),
                maxLines = 1,
            )
        }
    }
}

/** CSS's `linear-gradient(150deg, …)` across the badge, and the white house on it. */
private fun DrawScope.drawGroupBadge() {
    val a = 150f * PI.toFloat() / 180f
    val dx = sin(a)
    val dy = -cos(a)
    val len = abs(size.width * dx) + abs(size.height * dy)
    val c = Offset(size.width / 2f, size.height / 2f)
    val half = Offset(dx * len / 2f, dy * len / 2f)
    drawRect(
        Brush.linearGradient(
            listOf(DecoderRingScreenColors.GroupFrom, DecoderRingScreenColors.GroupTo),
            start = c - half,
            end = c + half,
        ),
    )
    // M4 11.5 12 5l8 6.5V19a1 1 0 0 1-1 1h-4.5v-5h-5v5H5a1 1 0 0 1-1-1z, 22 dp in the middle.
    val s = 22.dp.toPx() / 24f
    val o = (size.width - 22.dp.toPx()) / 2f
    fun x(v: Float) = o + v * s
    val house =
        Path().apply {
            moveTo(x(4f), x(11.5f))
            lineTo(x(12f), x(5f))
            lineTo(x(20f), x(11.5f))
            lineTo(x(20f), x(19f))
            quadraticTo(x(20f), x(20f), x(19f), x(20f))
            lineTo(x(14.5f), x(20f))
            lineTo(x(14.5f), x(15f))
            lineTo(x(9.5f), x(15f))
            lineTo(x(9.5f), x(20f))
            lineTo(x(5f), x(20f))
            quadraticTo(x(4f), x(20f), x(4f), x(19f))
            close()
        }
    drawPath(house, Color.White)
}

@Composable
private fun DayChip(colors: DecoderRingScreenColors) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        BasicText(
            stringResource(Res.string.decoderring_today),
            Modifier.clip(RoundedCornerShape(999.dp))
                .background(colors.chip)
                .padding(horizontal = 10.dp, vertical = 3.dp),
            style = css(11.5f, FontWeight.SemiBold).copy(color = colors.muted),
        )
    }
}

/** A neighbor's message: their avatar, their name and time, and [body] under it. */
@Composable
private fun Message(
    who: String,
    time: String,
    initials: String,
    avatar: Color,
    colors: DecoderRingScreenColors,
    body: @Composable () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.padding(top = 16.dp)
                .size(30.dp)
                .clip(CircleShape)
                .background(avatar)
                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            BasicText(initials, style = css(11.5f, FontWeight.Bold).copy(color = Color.White))
        }
        Column(Modifier.weight(1f)) {
            Row(Modifier.padding(start = 4.dp, bottom = 3.dp)) {
                BasicText(
                    who,
                    style = css(12f, FontWeight(650)).copy(color = colors.muted),
                    maxLines = 1,
                )
                BasicText(
                    time,
                    Modifier.padding(start = 4.dp),
                    style =
                        css(12f, FontWeight.Medium).copy(color = colors.muted.copy(alpha = .85f)),
                    maxLines = 1,
                )
            }
            body()
        }
    }
}

/** A plain bubble, as wide as its text up to 86 % of the column. */
@Composable
private fun Bubble(text: String, colors: DecoderRingScreenColors) {
    val shape = RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp)
    Box(Modifier.fillMaxWidth(.86f)) {
        BasicText(
            text,
            Modifier.clip(shape)
                .background(colors.card)
                .border(1.dp, colors.line, shape)
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 9.dp),
            style = css(15f, lines = 21f / 15f).copy(color = colors.ink),
        )
    }
}

/**
 * Your own bubble, on the right, placed as the prototype's grid places it: its column is as wide as
 * the text on one line (up to the thread's width) and stands at the right, and the bubble, at most
 * 86 % of that column, sits at the column's left.
 */
@Composable
private fun MyBubble(text: String, colors: DecoderRingScreenColors) {
    val shape = RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp)
    BasicText(
        text,
        Modifier.fillMaxWidth()
            .layout { m, c ->
                val column = min(m.maxIntrinsicWidth(Constraints.Infinity), c.maxWidth)
                val p = m.measure(Constraints(maxWidth = (column * .86f).toInt()))
                layout(c.maxWidth, p.height) { p.place(c.maxWidth - column, 0) }
            }
            .clip(shape)
            .background(colors.mine)
            .padding(start = 13.dp, end = 13.dp, top = 9.dp, bottom = 10.dp),
        style = css(15f, lines = 21f / 15f).copy(color = Color.White),
    )
}

/** The composer: a field to type in and the send button, both only drawn. */
@Composable
private fun Composer(colors: DecoderRingScreenColors) {
    Row(
        Modifier.fillMaxWidth()
            .padding(start = 10.dp, end = 10.dp, bottom = 12.dp)
            .clearAndSetSemantics {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val shape = RoundedCornerShape(22.dp)
        BasicText(
            stringResource(Res.string.decoderring_composer),
            Modifier.weight(1f)
                .clip(shape)
                .background(colors.card)
                .border(1.dp, colors.line, shape)
                .padding(horizontal = 15.dp, vertical = 11.dp),
            style = css(14.5f).copy(color = colors.muted),
            maxLines = 1,
        )
        Canvas(Modifier.size(40.dp)) {
            drawCircle(colors.accent)
            // M4 12h13M12 6l6 6-6 6, 18 dp in the middle.
            val s = 18.dp.toPx() / 24f
            val o = (size.width - 18.dp.toPx()) / 2f
            val stroke = Stroke(2.2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
            drawLine(
                Color.White,
                Offset(o + 4f * s, o + 12f * s),
                Offset(o + 17f * s, o + 12f * s),
                2.2f * s,
                StrokeCap.Round,
            )
            val p =
                Path().apply {
                    moveTo(o + 12f * s, o + 6f * s)
                    lineTo(o + 18f * s, o + 12f * s)
                    lineTo(o + 12f * s, o + 18f * s)
                }
            drawPath(p, Color.White, style = stroke)
        }
    }
}

// The prototype's text sits on CSS lines, 1.55 times its size unless it says otherwise, with the
// spare height shared above and below.
private val CssLines = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

private fun css(size: Float, weight: FontWeight = FontWeight.Normal, lines: Float = 1.55f) =
    TextStyle(
        fontSize = size.sp,
        fontWeight = weight,
        lineHeight = (size * lines).sp,
        lineHeightStyle = CssLines,
    )
