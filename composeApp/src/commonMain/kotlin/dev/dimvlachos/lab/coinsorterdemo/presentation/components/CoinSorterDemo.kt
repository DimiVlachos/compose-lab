package dev.dimvlachos.lab.coinsorterdemo.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.dimvlachos.lab.coinsorterdemo.CoinSorterHand
import dev.dimvlachos.lab.coinsorterdemo.Glyph
import dev.dimvlachos.lab.coinsorterdemo.Lantern
import dev.dimvlachos.lab.coinsorterdemo.LanternResults
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.demo.demoInsets
import dev.dimvlachos.lab.core.platform.rememberReducedMotion
import dev.dimvlachos.lab.core.platform.rememberSoundPlayer
import dev.dimvlachos.lab.core.presentation.components.coinsorter.CoinSorterResults
import dev.dimvlachos.lab.core.presentation.components.coinsorter.CoinSorterSounds
import dev.dimvlachos.lab.core.presentation.components.coinsorter.CoinSorterState
import dev.dimvlachos.lab.core.presentation.components.coinsorter.CoinSorterText
import dev.dimvlachos.lab.core.presentation.components.coinsorter.SortSlot
import dev.dimvlachos.lab.core.presentation.components.touch.drawTouch
import dev.dimvlachos.lab.core.presentation.ui.CoinSorterScreenColors
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.coinsorter_already
import dev.dimvlachos.lab.resources.coinsorter_best
import dev.dimvlachos.lab.resources.coinsorter_cart
import dev.dimvlachos.lab.resources.coinsorter_chip
import dev.dimvlachos.lab.resources.coinsorter_count
import dev.dimvlachos.lab.resources.coinsorter_high
import dev.dimvlachos.lab.resources.coinsorter_list
import dev.dimvlachos.lab.resources.coinsorter_low
import dev.dimvlachos.lab.resources.coinsorter_menu
import dev.dimvlachos.lab.resources.coinsorter_reviews
import dev.dimvlachos.lab.resources.coinsorter_row
import dev.dimvlachos.lab.resources.coinsorter_search
import dev.dimvlachos.lab.resources.coinsorter_sorted_by
import dev.dimvlachos.lab.resources.coinsorter_sorting
import dev.dimvlachos.lab.resources.coinsorter_sorting_by
import dev.dimvlachos.lab.resources.coinsorter_subtitle
import dev.dimvlachos.lab.resources.coinsorter_title
import dev.dimvlachos.lab.resources.coinsorter_top
import kotlin.coroutines.cancellation.CancellationException
import org.jetbrains.compose.resources.stringResource

/** The prototype's search results for "lantern", the list in the store's own ranking. */
@Composable
internal fun rememberCoinSorter(): CoinSorterState = remember {
    CoinSorterState(LanternResults.map { it.coin })
}

/**
 * The prototype's camping store results, sorted through a chrome coin-sorting tray. The script's
 * taps go through the same press a finger makes.
 */
@Composable
internal fun CoinSorterDemo(
    state: DemoState,
    sorter: CoinSorterState = rememberCoinSorter(),
    reducedMotion: Boolean = rememberReducedMotion(),
) {
    val sounds = rememberSoundPlayer(CoinSorterSounds)
    SideEffect { sorter.reducedMotion = reducedMotion }
    val hand = remember(sorter) { CoinSorterHand(sorter) }
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
        // Puts the list back as it began if a stopped run left it sorted, so the clip loops.
        state.setFeedResetHandler {
            step { hand.hide() }
            if (!sorter.asBegun) sorter.reset()
        }
        onDispose {
            state.setControlTapHandler(null)
            state.setFeedResetHandler(null)
        }
    }
    // A script the user takes over leaves no fingertip behind.
    LaunchedEffect(state.replay) { if (!state.replay) hand.stop() }

    val colors = if (LabTheme.isDark) CoinSorterScreenColors.Dark else CoinSorterScreenColors.Light
    val labels =
        listOf(
            stringResource(Res.string.coinsorter_best),
            stringResource(Res.string.coinsorter_low),
            stringResource(Res.string.coinsorter_high),
            stringResource(Res.string.coinsorter_top),
        )
    val sorting = stringResource(Res.string.coinsorter_sorting)
    val count = stringResource(Res.string.coinsorter_count)
    val chip = stringResource(Res.string.coinsorter_chip, SortSlot)
    val sortingBy = stringResource(Res.string.coinsorter_sorting_by, SortSlot)
    val sortedBy = stringResource(Res.string.coinsorter_sorted_by, SortSlot)
    val already = stringResource(Res.string.coinsorter_already, SortSlot)
    val menu = stringResource(Res.string.coinsorter_menu)
    val list = stringResource(Res.string.coinsorter_list)
    // Made once for the strings, so the sorter's event listener isn't made again.
    val text =
        remember(labels, sorting, count, chip, sortingBy, sortedBy, already, menu, list) {
            CoinSorterText(labels, sorting, count, chip, sortingBy, sortedBy, already, menu, list)
        }
    val rowLabels = LanternResults.map {
        stringResource(
            Res.string.coinsorter_row,
            it.name,
            "${it.cents / 100}.${(it.cents % 100).toString().padStart(2, '0')}",
            CoinSorterState.rating(it.rating),
        )
    }
    val reviews = LanternResults.map { stringResource(Res.string.coinsorter_reviews, it.reviews) }
    val touch = LabTheme.colors.touch
    Box(Modifier.fillMaxSize().background(colors.background)) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(demoInsets(state))) {
            CoinSorterResults(
                sorter,
                text,
                colors,
                sounds,
                modifier = Modifier.fillMaxSize(),
                rowLabel = { rowLabels[it] },
                header = { Header(colors) },
            ) { i ->
                ResultRow(LanternResults[i], reviews[i], colors)
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
}

/** The store's bar and its search field, above the toolbar. */
@Composable
private fun Header(colors: CoinSorterScreenColors) {
    Column {
        Row(
            Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 6.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(Res.string.coinsorter_title),
                    // Held to its CSS line box: the platform font's own is taller than 1.05.
                    Modifier.height(29.4.dp).wrapContentHeight(unbounded = true).semantics {
                        heading()
                    },
                    style =
                        css(28f, 1.05f, colors.ink, FontWeight(750))
                            .copy(letterSpacing = (-.02).em),
                    maxLines = 1,
                )
                Text(
                    stringResource(Res.string.coinsorter_subtitle),
                    Modifier.padding(top = 3.dp),
                    style = css(12.5f, 1.55f, colors.muted, FontWeight.Medium),
                    maxLines = 1,
                )
            }
            Cart(colors)
        }
        Row(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 6.dp)
                .fillMaxWidth()
                .background(colors.chip, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 9.dp)
                .clearAndSetSemantics {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Canvas(Modifier.size(16.dp)) {
                val s = size.width / 24f
                drawCircle(colors.muted, 6.5f * s, Offset(11 * s, 11 * s), style = Stroke(2 * s))
                drawLine(
                    colors.muted,
                    Offset(16 * s, 16 * s),
                    Offset(20 * s, 20 * s),
                    2 * s,
                    StrokeCap.Round,
                )
            }
            Text(
                stringResource(Res.string.coinsorter_search),
                style = css(14f, 1.55f, colors.ink, FontWeight.Normal),
            )
        }
    }
}

@Composable
private fun Cart(colors: CoinSorterScreenColors) {
    val label = stringResource(Res.string.coinsorter_cart)
    Box(Modifier.size(36.dp).clearAndSetSemantics { contentDescription = label }) {
        Box(
            Modifier.fillMaxSize().background(colors.chip, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(19.dp)) { cart(colors.ink) }
        }
        Box(
            Modifier.offset(x = 21.dp, y = (-2).dp)
                .sizeIn(minWidth = 17.dp, minHeight = 17.dp)
                .background(colors.accent, RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("2", style = css(10.5f, 1f, colors.card, FontWeight.Bold))
        }
    }
}

private val cartPath =
    PathParser()
        .parsePathString(
            "M3 4h2.2l2.1 10.2a1.6 1.6 0 0 0 1.6 1.3h8.3a1.6 1.6 0 0 0 1.5-1.2L20.4 8H6.2",
        )
        .toPath()

private fun DrawScope.cart(ink: Color) {
    val s = size.width / 24f
    scale(s, s, pivot = Offset.Zero) {
        drawPath(
            cartPath,
            ink,
            style = Stroke(1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawCircle(ink, 1.5f, Offset(9.5f, 19.5f))
        drawCircle(ink, 1.5f, Offset(17f, 19.5f))
    }
}

/** A result's card content: its icon, its name and rating, and its price with raised cents. */
@Composable
private fun ResultRow(item: Lantern, reviews: String, colors: CoinSorterScreenColors) {
    Row(
        Modifier.fillMaxWidth().clearAndSetSemantics {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            Modifier.size(38.dp).background(item.tint, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(26.dp)) { glyph(item.glyph, item.ink) }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                item.name,
                style = css(14f, 1.55f, colors.ink, FontWeight(650)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = colors.star)) { append("★") }
                    append(" ${CoinSorterState.rating(item.rating)} · $reviews")
                },
                style = css(12.3f, 1.55f, colors.muted, FontWeight.Normal),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            buildAnnotatedString {
                append("$${item.cents / 100}")
                withStyle(
                    SpanStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        baselineShift = BaselineShift(.5f),
                    ),
                ) {
                    append(" " + (item.cents % 100).toString().padStart(2, '0'))
                }
            },
            style =
                css(15f, 1.55f, colors.ink, FontWeight(750))
                    .copy(letterSpacing = (-.01).em, fontFeatureSettings = "tnum"),
            maxLines = 1,
        )
    }
}

private val Glyphs: Map<String, androidx.compose.ui.graphics.Path> =
    listOf(
            "lanternHandle" to "M9 5.6a3 3 0 0 1 6 0",
            "oilSpout" to "M12 2.6v2.4",
            "oilGlass" to "M9.7 5.2h4.6l-.5 2.8c2.5 1.3 3.2 3.7 2.5 6.4H7.7c-.7-2.7 0-5.1 2.5-6.4z",
            "oilFlame" to "M12 9.6c1.3 1.4 1.4 2.8 0 3.9-1.4-1.1-1.3-2.5 0-3.9z",
            "stringWire" to "M2.5 6.5c3.5 3.4 6.4 3.6 9.5 1.8s6-3 9.5.4",
            "stringCaps" to "M5.3 9.4h1.8M11.1 8.8h1.8M16.9 9h1.8",
            "headBand" to "M3.2 14c0-5.4 3.9-9 8.8-9s8.8 3.6 8.8 9",
            "puckSide" to "M4.4 9v6.2c0 1.6 3.4 2.9 7.6 2.9s7.6-1.3 7.6-2.9V9",
            "puckLines" to "M8.2 8.4h7.6M8.2 10h7.6",
        )
        .associate { (k, d) -> k to PathParser().parsePathString(d).toPath() }

// The prototype's inline SVG icons, in a 24 box, in the item's ink.
private fun DrawScope.glyph(g: Glyph, ink: Color) {
    val s = size.width / 24f
    fun rr(x: Float, y: Float, w: Float, h: Float, r: Float, a: Float = 1f) =
        drawRoundRect(ink, Offset(x, y), Size(w, h), CornerRadius(r), alpha = a)
    scale(s, s, pivot = Offset.Zero) {
        when (g) {
            Glyph.Lantern -> {
                drawPath(Glyphs.getValue("lanternHandle"), ink, style = Stroke(1.6f))
                rr(7.4f, 6.2f, 9.2f, 2.2f, 1f)
                rr(8.3f, 8.6f, 7.4f, 9f, 2.6f, .42f)
                rr(10.6f, 10.4f, 2.8f, 5.4f, 1.4f)
                rr(7.4f, 17.8f, 9.2f, 2.4f, 1f)
            }
            Glyph.Oil -> {
                drawPath(
                    Glyphs.getValue("oilSpout"),
                    ink,
                    style = Stroke(1.6f, cap = StrokeCap.Round),
                )
                drawPath(Glyphs.getValue("oilGlass"), ink, alpha = .42f)
                drawPath(Glyphs.getValue("oilFlame"), ink)
                rr(6.4f, 14.8f, 11.2f, 5.6f, 2f)
            }
            Glyph.String -> {
                drawPath(
                    Glyphs.getValue("stringWire"),
                    ink,
                    style = Stroke(1.4f, cap = StrokeCap.Round),
                )
                drawOval(ink, Offset(4.2f, 9.2f), Size(4f, 5.6f))
                drawOval(ink, Offset(10f, 8.6f), Size(4f, 5.6f), alpha = .5f)
                drawOval(ink, Offset(15.8f, 8.8f), Size(4f, 5.6f))
                drawPath(Glyphs.getValue("stringCaps"), ink, style = Stroke(1.4f))
            }
            Glyph.Head -> {
                drawPath(
                    Glyphs.getValue("headBand"),
                    ink,
                    alpha = .42f,
                    style = Stroke(2.4f, cap = StrokeCap.Round),
                )
                rr(7.2f, 10f, 9.6f, 8f, 2.8f)
                drawCircle(Color.White, 2.4f, Offset(12f, 14f), alpha = .85f)
            }
            Glyph.Puck -> {
                drawPath(Glyphs.getValue("puckSide"), ink, alpha = .42f)
                drawOval(ink, Offset(4.4f, 6.2f), Size(15.2f, 5.6f))
                drawPath(
                    Glyphs.getValue("puckLines"),
                    Color.White,
                    alpha = .7f,
                    style = Stroke(.9f),
                )
            }
        }
    }
}

// Text in a CSS line box: [size] px type on a [line] × size line, the leading split above and
// below, at [weight] in [color].
private fun css(size: Float, line: Float, color: Color, weight: FontWeight) =
    TextStyle(
        color = color,
        fontSize = size.sp,
        lineHeight = line.em,
        fontWeight = weight,
        lineHeightStyle =
            LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
    )
