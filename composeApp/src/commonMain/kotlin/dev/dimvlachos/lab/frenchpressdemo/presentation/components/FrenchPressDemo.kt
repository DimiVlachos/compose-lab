package dev.dimvlachos.lab.frenchpressdemo.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.demo.demoInsets
import dev.dimvlachos.lab.core.platform.rememberReducedMotion
import dev.dimvlachos.lab.core.platform.rememberSoundPlayer
import dev.dimvlachos.lab.core.presentation.components.frenchpress.FrenchPressSounds
import dev.dimvlachos.lab.core.presentation.components.frenchpress.FrenchPressState
import dev.dimvlachos.lab.core.presentation.components.frenchpress.FrenchPressStory
import dev.dimvlachos.lab.core.presentation.components.frenchpress.FrenchPressText
import dev.dimvlachos.lab.core.presentation.components.frenchpress.PressSaid
import dev.dimvlachos.lab.core.presentation.components.frenchpress.rememberPressMono
import dev.dimvlachos.lab.core.presentation.components.frenchpress.rememberPressSerif
import dev.dimvlachos.lab.core.presentation.components.touch.drawTouch
import dev.dimvlachos.lab.core.presentation.ui.FrenchPressScreenColors
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.frenchpressdemo.FrenchPressHand
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.frenchpress_author
import dev.dimvlachos.lab.resources.frenchpress_by
import dev.dimvlachos.lab.resources.frenchpress_card_from
import dev.dimvlachos.lab.resources.frenchpress_card_title
import dev.dimvlachos.lab.resources.frenchpress_count
import dev.dimvlachos.lab.resources.frenchpress_date
import dev.dimvlachos.lab.resources.frenchpress_full
import dev.dimvlachos.lab.resources.frenchpress_headline
import dev.dimvlachos.lab.resources.frenchpress_kept
import dev.dimvlachos.lab.resources.frenchpress_kicker
import dev.dimvlachos.lab.resources.frenchpress_masthead
import dev.dimvlachos.lab.resources.frenchpress_note
import dev.dimvlachos.lab.resources.frenchpress_para_1
import dev.dimvlachos.lab.resources.frenchpress_para_2
import dev.dimvlachos.lab.resources.frenchpress_para_3
import dev.dimvlachos.lab.resources.frenchpress_para_4
import dev.dimvlachos.lab.resources.frenchpress_para_5
import dev.dimvlachos.lab.resources.frenchpress_para_6
import dev.dimvlachos.lab.resources.frenchpress_point_1
import dev.dimvlachos.lab.resources.frenchpress_point_2
import dev.dimvlachos.lab.resources.frenchpress_point_3
import dev.dimvlachos.lab.resources.frenchpress_pressing
import dev.dimvlachos.lab.resources.frenchpress_read
import dev.dimvlachos.lab.resources.frenchpress_ready
import dev.dimvlachos.lab.resources.frenchpress_show_full
import dev.dimvlachos.lab.resources.frenchpress_summarize
import dev.dimvlachos.lab.resources.frenchpress_summarize_article
import dev.dimvlachos.lab.resources.frenchpress_summary
import kotlin.coroutines.cancellation.CancellationException
import org.jetbrains.compose.resources.stringResource

/**
 * The prototype's local news story from The Harlow Courier, whose Summarize button is a French
 * press. The script's pushes and taps go through the same gesture a finger makes.
 */
@Composable
internal fun FrenchPressDemo(state: DemoState, reducedMotion: Boolean = rememberReducedMotion()) {
    val paragraphs =
        listOf(
                stringResource(Res.string.frenchpress_para_1),
                stringResource(Res.string.frenchpress_para_2),
                stringResource(Res.string.frenchpress_para_3),
                stringResource(Res.string.frenchpress_para_4),
                stringResource(Res.string.frenchpress_para_5),
                stringResource(Res.string.frenchpress_para_6),
            )
            .let { ps -> remember(ps) { ps.map { it.split(' ') } } }
    val points =
        listOf(
                stringResource(Res.string.frenchpress_point_1),
                stringResource(Res.string.frenchpress_point_2),
                stringResource(Res.string.frenchpress_point_3),
            )
            .let { remember(it) { it } }
    val press = remember(paragraphs, points) { FrenchPressState(paragraphs, points) }
    SideEffect { press.reducedMotion = reducedMotion }
    val sounds = rememberSoundPlayer(FrenchPressSounds)
    val hand = remember(press) { FrenchPressHand(press) }
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
        // Puts the story back as it began if a stopped run left it pressed, so the clip loops.
        state.setFeedResetHandler {
            step {
                hand.hide()
                hand.settle(2000)
            }
            if (!press.asBegun) press.reset()
        }
        onDispose {
            state.setControlTapHandler(null)
            state.setFeedResetHandler(null)
        }
    }
    // A script the user takes over leaves no fingertip behind.
    LaunchedEffect(state.replay) { if (!state.replay) hand.stop() }

    val colors =
        if (LabTheme.isDark) FrenchPressScreenColors.Dark else FrenchPressScreenColors.Light
    val said =
        mapOf(
            PressSaid.None to "",
            PressSaid.Pressing to stringResource(Res.string.frenchpress_pressing),
            PressSaid.Ready to stringResource(Res.string.frenchpress_ready),
            PressSaid.Kept to stringResource(Res.string.frenchpress_kept),
            PressSaid.Full to stringResource(Res.string.frenchpress_full),
        )
    val text =
        FrenchPressText(
            title = stringResource(Res.string.frenchpress_card_title),
            from = stringResource(Res.string.frenchpress_card_from),
            full = stringResource(Res.string.frenchpress_full),
            count = stringResource(Res.string.frenchpress_count, press.words.size),
            note = stringResource(Res.string.frenchpress_note),
            summary = stringResource(Res.string.frenchpress_summary),
            summarize = stringResource(Res.string.frenchpress_summarize),
            summarizeArticle = stringResource(Res.string.frenchpress_summarize_article),
            showFull = stringResource(Res.string.frenchpress_show_full),
            said = { said[it].orEmpty() },
        )
    val touch = LabTheme.colors.touch
    Box(Modifier.fillMaxSize().windowInsetsPadding(demoInsets(state))) {
        FrenchPressStory(
            press,
            paragraphs,
            points,
            text,
            colors,
            sounds,
            Modifier.fillMaxSize(),
            topBar = { AppBar(colors) },
            header = { Header(colors) },
            overlay = {
                val a = hand.shown
                if (a > 0f) {
                    val at = hand.at * density
                    val k = if (hand.down) .82f else 1f
                    scale(k, k, pivot = at) { drawTouch(touch, at, a) }
                }
            },
        )
    }
}

/** The paper's bar: back, the masthead, bookmark and share, as the prototype draws them. */
@Composable
private fun AppBar(colors: FrenchPressScreenColors) {
    val serif = rememberPressSerif()
    val line = colors.line
    Row(
        Modifier.fillMaxWidth()
            .drawBehind {
                val w = 1.dp.toPx()
                drawRect(line, Offset(0f, size.height - w), Size(size.width, w))
            }
            .padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(colors.ink, "M15 5l-7 7 7 7", 2f)
        BasicText(
            stringResource(Res.string.frenchpress_masthead),
            Modifier.weight(1f).semantics { heading() },
            style =
                TextStyle(
                    textMotion = TextMotion.Animated,
                    fontFamily = serif,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    fontSize = 19.sp,
                    lineHeight = 1.em,
                    letterSpacing = (-.19).sp,
                    textAlign = TextAlign.Center,
                    color = colors.ink,
                ),
            maxLines = 1,
        )
        Icon(colors.ink, "M7 4h10v16l-5-4-5 4z", 1.8f)
        Icon(colors.ink, "M12 15V4m0 0L8 8m4-4 4 4M5 13v6h14v-6", 1.8f)
    }
}

// A 21 dp line icon from the prototype's 24-box path, in a 40 box; the bar's icons are
// decoration, as the prototype's aria-hidden ones.
@Composable
private fun Icon(color: Color, d: String, width: Float) {
    val path = remember(d) { PathParser().parsePathString(d).toPath() }
    Box(Modifier.size(40.dp).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(21.dp)) {
            val s = size.width / 24f
            scale(s, s, pivot = Offset.Zero) {
                drawPath(
                    path,
                    color,
                    style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
    }
}

/** The story's kicker, headline and byline, over the summary card. */
@Composable
private fun Header(colors: FrenchPressScreenColors) {
    val serif = rememberPressSerif()
    val mono = rememberPressMono()
    Column {
        BasicText(
            stringResource(Res.string.frenchpress_kicker).uppercase(),
            style =
                TextStyle(
                    textMotion = TextMotion.Animated,
                    fontFamily = mono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    lineHeightStyle = CssLine,
                    letterSpacing = 1.1.sp,
                    color = colors.red,
                ),
        )
        BasicText(
            stringResource(Res.string.frenchpress_headline),
            Modifier.padding(top = 6.dp, bottom = 8.dp)
                .layout { m, c ->
                    // The browser lets a line box shorter than its font (1.17 against PT Serif
                    // Bold's 1.325) pull the first line up into the leading above it, and the
                    // box ends 1.5 above where Compose's does; Compose keeps the font's full
                    // ascent and descent. Moved up and cut by the differences, the lines and
                    // what follows sit where the prototype's do.
                    val p = m.measure(c)
                    val up = (23 * (1.325f - 1.17f)).dp.roundToPx()
                    val cut = up + 1.5.dp.roundToPx()
                    layout(p.width, p.height - cut) { p.place(0, -up) }
                }
                .semantics { heading() },
            style =
                TextStyle(
                    textMotion = TextMotion.Animated,
                    fontFamily = serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 23.sp,
                    lineHeight = 1.17.em,
                    letterSpacing = (-.23).sp,
                    lineBreak = LineBreak.Heading,
                    lineHeightStyle = CssLine,
                    color = colors.ink,
                ),
        )
        val by = stringResource(Res.string.frenchpress_by)
        val author = stringResource(Res.string.frenchpress_author)
        val small =
            TextStyle(
                textMotion = TextMotion.Animated,
                fontSize = 12.5.sp,
                lineHeight = 1.55.em,
                lineHeightStyle = CssLine,
                color = colors.muted,
            )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BasicText(
                buildAnnotatedString {
                    append(by)
                    append(' ')
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = colors.ink)) {
                        append(author)
                    }
                },
                style = small,
            )
            BasicText(stringResource(Res.string.frenchpress_date), style = small)
            BasicText(stringResource(Res.string.frenchpress_read), style = small)
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).drawBehind { drawRect(colors.line) })
        Spacer(Modifier.height(12.dp))
    }
}

private val CssLine = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)
