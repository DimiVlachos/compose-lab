package dev.dimvlachos.lab.core.presentation.components.frenchpress

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.Constraints
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
import dev.dimvlachos.lab.core.presentation.ui.FrenchPressScreenColors
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.jetbrains_mono_regular
import dev.dimvlachos.lab.resources.jetbrains_mono_semibold
import dev.dimvlachos.lab.resources.pt_serif_bold
import dev.dimvlachos.lab.resources.pt_serif_bold_italic
import dev.dimvlachos.lab.resources.pt_serif_italic
import dev.dimvlachos.lab.resources.pt_serif_regular
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.Font

/** The clips the press plays. */
internal val FrenchPressSounds =
    setOf(
        Sound.PressPour,
        Sound.PressRise,
        Sound.PressTick,
        Sound.PressTink,
        Sound.PressGlug,
        Sound.PressLift,
        Sound.PressHiss,
    )

/**
 * What the story's screen says: the summary card's [title], where it is [from], the [full] article
 * button, how many [count] words went in (with `%1$d` filled in), its closing [note]; the press's
 * [summarize] label and its two names for screen readers, the [summary] region's name, and what is
 * announced for each [PressSaid].
 */
internal class FrenchPressText(
    val title: String,
    val from: String,
    val full: String,
    val count: String,
    val note: String,
    val summary: String,
    val summarize: String,
    val summarizeArticle: String,
    val showFull: String,
    val said: (PressSaid) -> String,
)

/** PT Serif, the story's face, in the four cuts it is set in. */
@Composable
internal fun rememberPressSerif(): FontFamily {
    val regular = Font(Res.font.pt_serif_regular, FontWeight.Normal)
    val bold = Font(Res.font.pt_serif_bold, FontWeight.Bold)
    val italic = Font(Res.font.pt_serif_italic, FontWeight.Normal, FontStyle.Italic)
    val boldItalic = Font(Res.font.pt_serif_bold_italic, FontWeight.Bold, FontStyle.Italic)
    return remember(regular, bold, italic, boldItalic) {
        FontFamily(regular, bold, italic, boldItalic)
    }
}

/** JetBrains Mono at 400 and 600, the kicker's and the card's small caps. */
@Composable
internal fun rememberPressMono(): FontFamily {
    val regular = Font(Res.font.jetbrains_mono_regular, FontWeight.Normal)
    val semi = Font(Res.font.jetbrains_mono_semibold, FontWeight.SemiBold)
    return remember(regular, semi) { FontFamily(regular, semi) }
}

// Where the laid-out pieces are on the screen: each is placed relative to the screen's box.
private class Placer(val state: FrenchPressState) {
    var root: LayoutCoordinates? = null
    var body: LayoutCoordinates? = null
    var article: LayoutCoordinates? = null
    var summary: LayoutCoordinates? = null
    var section: LayoutCoordinates? = null
    var full: LayoutCoordinates? = null
    var density = 1f

    /**
     * The summary section's top on the screen, dp: where the body starts while the card is shut.
     */
    var sectionTop = 0f

    private fun at(c: LayoutCoordinates?): Offset? {
        val r = root ?: return null
        if (c == null || !c.isAttached || !r.isAttached) return null
        return r.localPositionOf(c, Offset.Zero) / density
    }

    fun push() {
        val a = at(article)
        at(body)?.let { state.placeBody(it.x, it.y, a?.y ?: 0f) }
        at(summary)?.let { state.placeSummary(it.x, it.y) }
        at(section)?.let { sectionTop = it.y }
        full?.let { f ->
            at(f)?.let {
                state.placeFull(
                    it.x + f.size.width / density / 2,
                    it.y + f.size.height / density / 2,
                )
            }
        }
    }
}

/**
 * A news story whose Summarize button is a little glass French press in the bottom-right corner, as
 * the prototype draws it. Push its knob and the story's own words lift off the page, line by line,
 * and pour into the spout, where they swirl as grounds; past halfway, let go and the mesh presses
 * them down, the coffee clears to amber, the press tips and the summary pours out letter by letter
 * into a card at the top of the story. Let go short and the words fly home. Tap the press and it
 * does the whole thing; tap it again, pull the knob up or tap Full article for the story back,
 * every word to its own place.
 *
 * [paragraphs] are the story's words, each paragraph a list, and [points] the summary's; [state]
 * must be made from the same. [topBar] is the screen's bar, [header] the story's kicker, headline
 * and byline over the card. [overlay] draws over everything (the script's fingertip).
 */
@Composable
internal fun FrenchPressStory(
    state: FrenchPressState,
    paragraphs: List<List<String>>,
    points: List<String>,
    text: FrenchPressText,
    colors: FrenchPressScreenColors,
    sounds: SoundPlayer,
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit,
    header: @Composable () -> Unit,
    overlay: DrawScope.() -> Unit = {},
) {
    val haptics = LocalHapticFeedback.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val dark = LabTheme.isDark
    LaunchedEffect(state, sounds, haptics) {
        state.onEvent = { e, i ->
            when (e) {
                FrenchPressEvent.Pour -> {
                    sounds.play(Sound.PressPour)
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                }
                FrenchPressEvent.Rise -> sounds.play(Sound.PressRise)
                FrenchPressEvent.Tick -> {
                    sounds.playVaried(Sound.PressTick, 1f, 1f + Random.nextFloat() * .27f)
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                }
                FrenchPressEvent.Tink -> {
                    sounds.play(Sound.PressTink)
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                }
                // The prototype's glugs step up in pitch: 150 + i·14 Hz.
                FrenchPressEvent.Glug -> sounds.play(Sound.PressGlug, 1f, (150f + i * 14) / 150f)
                FrenchPressEvent.Lift -> {
                    sounds.play(Sound.PressLift)
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                }
            }
        }
    }
    LaunchedEffect(state, lifecycle, sounds) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            // The plunger's hiss swells with how fast the mesh moves; it is stopped while nothing
            // moves, so a silent loop doesn't keep the audio awake.
            var hiss = sounds.loop(Sound.PressHiss, 0f)
            var volume = 0f
            var last = 0L
            try {
                while (true) {
                    // At rest the loop sleeps, until a touch, a key or the script moves something.
                    if (state.settled) {
                        volume = 0f
                        hiss.stop()
                        val seen = state.frame
                        snapshotFlow { state.frame }.first { it != seen }
                        hiss = sounds.loop(Sound.PressHiss, 0f)
                        last = 0L
                    }
                    withFrameNanos { now ->
                        val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(.15f)
                        last = now
                        state.advance(dt)
                        val target = (abs(state.speedP) * .09f).coerceIn(0f, .17f) / .17f
                        if (dt > 0f) volume += (target - volume) * (1 - exp(-dt / .04f))
                        hiss.setVolume(volume)
                    }
                }
            } finally {
                hiss.stop()
            }
        }
    }

    val density = LocalDensity.current
    val d = density.density
    val serif = rememberPressSerif()
    val mono = rememberPressMono()
    val measurer = rememberTextMeasurer(cacheSize = 0)
    val placer = remember(state) { Placer(state) }
    placer.density = d
    BoxWithConstraints(modifier) {
        val bodyStyle =
            TextStyle(
                textMotion = TextMotion.Animated,
                fontFamily = serif,
                fontSize = 15.5.sp,
                lineHeight = 1.56.em,
            )
        val pointStyle =
            TextStyle(
                textMotion = TextMotion.Animated,
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 14.5.sp,
                lineHeight = 1.4.em,
            )
        // The article has 20 a side; the card's points sit inside its 1 border, 14 padding and 18
        // for the bullets.
        // Measured in the same frame the width is known, so the story is laid out from the first.
        val widthDp = maxWidth.value
        val laid =
            remember(measurer, density, serif, widthDp, paragraphs, points) {
                if (widthDp <= 0f) null
                else
                    PressText(
                            measurer,
                            density,
                            paragraphs,
                            points,
                            bodyStyle,
                            pointStyle,
                            bodyWidth = widthDp - 40,
                            pointWidth = widthDp - 40 - 2 - 28 - 18,
                            paragraphGap = 10f,
                            pointGap = 6f,
                        )
                        .also { t ->
                            for (i in state.words.indices) state.setWord(
                                i,
                                t.wordCx[i],
                                t.wordCy(i),
                            )
                            state.setWordHeight(t.wordBoxHeight)
                            for (i in state.letters.indices) state.setLetter(
                                i,
                                18 + t.letterCx[i],
                                t.letterCy(i),
                            )
                        }
            }
        val painter = remember { FrenchPressPainter() }
        val ink =
            remember(colors.ink) {
                Rgb(colors.ink.red * 255, colors.ink.green * 255, colors.ink.blue * 255)
            }

        Box(
            Modifier.fillMaxSize()
                .background(colors.background)
                .onSizeChanged { state.layout(it.width / d, it.height / d) }
                .onGloballyPositioned {
                    placer.root = it
                    placer.push()
                },
        ) {
            Column(Modifier.fillMaxSize()) {
                topBar()
                Column(
                    Modifier.weight(1f)
                        .fillMaxWidth()
                        .clipToBounds()
                        .onGloballyPositioned {
                            placer.article = it
                            placer.push()
                        }
                        .padding(start = 20.dp, end = 20.dp, top = 14.dp),
                ) {
                    header()
                    if (laid != null) {
                        SummaryCard(state, laid, text, colors, mono, serif, placer, ink)
                        Body(state, laid, paragraphs, colors, placer)
                    }
                }
            }
            if (laid != null) {
                Canvas(Modifier.fillMaxSize()) {
                    state.frame
                    drawIntoCanvas { canvas ->
                        val c = Canvas2D(canvas, size.width.toInt(), size.height.toInt())
                        c.scale(d, d)
                        painter.draw(c, state, laid, dark, ink, d)
                    }
                }
            }
            PressButton(state, text, colors.amber)
            PressTag(state, text, colors, mono)
            Announcer(state, text)
            Canvas(Modifier.fillMaxSize()) { overlay() }
        }
    }
}

/** The story's body: every word drawn where its paragraph put it, unless it is off the page. */
@Composable
private fun Body(
    state: FrenchPressState,
    laid: PressText,
    paragraphs: List<List<String>>,
    colors: FrenchPressScreenColors,
    placer: Placer,
) {
    val brewed = state.mode == PressMode.Brewed
    val all = remember(paragraphs) { paragraphs.joinToString("\n\n") { it.joinToString(" ") } }
    val mask = remember { MaskCache() }
    val ink = colors.ink
    Spacer(
        Modifier.fillMaxWidth()
            .height(laid.bodyHeight.dp)
            .onGloballyPositioned {
                placer.body = it
                placer.push()
            }
            .then(
                if (brewed) Modifier.clearAndSetSemantics {}
                else Modifier.clearAndSetSemantics { text = AnnotatedString(all) },
            )
            .drawWithContent {
                state.frame
                val d = density
                // The press's corner is kept clear: the story ends in a 28 fade above the knob.
                // Only that band is drawn in a layer to fade it; nothing shows below it.
                val stop = (state.clearY - placer.sectionTop)
                val fade = stop - 28
                drawIntoCanvas { canvas ->
                    canvas.save()
                    canvas.clipRect(0f, 0f, size.width, fade * d)
                    canvas.scale(d, d)
                    drawWords(canvas, state, laid, ink, 0f, fade)
                    canvas.restore()
                    if (stop > 0f) {
                        val band = Rect(0f, fade * d, size.width, stop * d)
                        canvas.saveLayer(band, mask.paint)
                        canvas.clipRect(band)
                        canvas.save()
                        canvas.scale(d, d)
                        drawWords(canvas, state, laid, ink, fade, stop)
                        canvas.restore()
                        canvas.drawRect(band, mask.fade(fade * d, stop * d))
                        canvas.restore()
                    }
                }
            },
    )
}

// The body's words between [lo] and [hi] (dp from its top): a paragraph with none off the page is
// drawn whole, else word by word, each from its own layout where the paragraph put it.
private fun drawWords(
    canvas: Canvas,
    state: FrenchPressState,
    laid: PressText,
    ink: Color,
    lo: Float,
    hi: Float,
) {
    val d = laid.density
    for (p in laid.paragraphLayouts.indices) {
        val top = laid.paragraphTops[p]
        val bottom = top + laid.paragraphLayouts[p].size.height / d
        if (bottom < lo || top > hi) continue
        val first = laid.firstWord[p]
        val end = if (p + 1 < laid.firstWord.size) laid.firstWord[p + 1] else state.words.size
        var anyGone = false
        for (w in first until end) if (state.gone[w]) {
            anyGone = true
            break
        }
        if (!anyGone) {
            canvas.save()
            canvas.translate(0f, top)
            canvas.scale(1 / d, 1 / d)
            laid.paragraphLayouts[p].multiParagraph.paint(canvas, ink)
            canvas.restore()
        } else {
            for (w in first until end) {
                val b = laid.wordBaseline[w]
                if (state.gone[w] || b + 8 < lo || b - 24 > hi) continue
                canvas.save()
                canvas.translate(laid.wordCx[w], b)
                laid.paintWord(canvas, w, ink, 0f)
                canvas.restore()
            }
        }
    }
}

private class MaskCache {
    val paint = Paint()
    private val fadePaint = Paint().apply { blendMode = BlendMode.DstIn }

    private var from = Float.NaN
    private var to = Float.NaN
    private var made: Shader? = null

    /** A paint that keeps what is under it, opaque at [start] to clear at [end]. */
    fun fade(start: Float, end: Float): Paint {
        if (start != from || end != to || made == null) {
            from = start
            to = end
            made =
                LinearGradientShader(
                    Offset(0f, start),
                    Offset(0f, end),
                    listOf(Color.Black, Color.Transparent),
                )
            fadePaint.shader = made
        }
        return fadePaint
    }
}

/**
 * The prototype's summary card: shut (height 0) until the press opens it, its height
 * [FrenchPressState.cardOpen] of what it measures. Its letters are written in as they land.
 */
@Composable
private fun SummaryCard(
    state: FrenchPressState,
    laid: PressText,
    text: FrenchPressText,
    colors: FrenchPressScreenColors,
    mono: FontFamily,
    serif: FontFamily,
    placer: Placer,
    ink: Rgb,
) {
    val open by remember(state) { derivedStateOf { state.cardOpen > 0f } }
    var notePad by remember(state.layoutVersion) { mutableStateOf(-1f) }
    val inner = remember { floatArrayOf(0f) }
    val d = LocalDensity.current.density
    Box(
        Modifier.fillMaxWidth()
            .onGloballyPositioned {
                placer.section = it
                placer.push()
                // On a short screen the open card's closing note reaches the press's corner: it
                // wraps short of the press instead of under it.
                if (notePad < 0f && inner[0] > 0f && placer.root != null) {
                    notePad =
                        if (placer.sectionTop + inner[0] > state.clearY)
                            max(0f, state.width - state.ox + state.cw / 2 + 14 * state.kp) - 20
                        else 0f
                }
            }
            // Clipped to the height it reports: the clip goes outside the height.
            .clipToBounds()
            .layout { m, c ->
                val p = m.measure(c.copy(maxHeight = Constraints.Infinity))
                val f = state.cardOpen
                val h = if (f >= 1f) p.height else (f * p.height).roundToInt()
                layout(p.width, h.coerceAtMost(c.maxHeight)) { p.place(0, 0) }
            }
            .then(
                if (open) Modifier.semantics { paneTitle = text.summary }
                else Modifier.clearAndSetSemantics {},
            ),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(bottom = 14.dp).onSizeChanged {
                if (notePad < 0f) inner[0] = it.height / d
            },
        ) {
            Column(
                Modifier.fillMaxWidth()
                    .drawBehind { cardShadow() }
                    .background(colors.card, RoundedCornerShape(14.dp))
                    .border(1.dp, colors.amberLine, RoundedCornerShape(14.dp))
                    .padding(start = 15.dp, end = 15.dp, top = 13.dp, bottom = 9.dp),
            ) {
                Row {
                    BasicText(
                        text.title.uppercase(),
                        Modifier.alignByBaseline(),
                        style =
                            TextStyle(
                                textMotion = TextMotion.Animated,
                                fontFamily = mono,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.5.sp,
                                lineHeight = 14.sp,
                                lineHeightStyle = CssLine,
                                letterSpacing = 1.05.sp,
                                color = colors.amber,
                            ),
                        maxLines = 1,
                        softWrap = false,
                    )
                    Spacer(Modifier.weight(1f))
                    BasicText(
                        text.from,
                        Modifier.alignByBaseline(),
                        style =
                            TextStyle(
                                textMotion = TextMotion.Animated,
                                fontFamily = mono,
                                fontSize = 10.5.sp,
                                lineHeight = 14.sp,
                                lineHeightStyle = CssLine,
                                letterSpacing = .21.sp,
                                color = colors.muted,
                            ),
                        maxLines = 1,
                        softWrap = false,
                    )
                }
                Points(state, laid, colors, placer)
                Footer(state, text, colors, placer)
            }
            BasicText(
                text.note,
                // The browser fits a line that overflows its 383 by a hair (its widths are in 64ths
                // of
                // a px); a dp less margin fits the same words on a line here.
                Modifier.padding(top = 10.dp, start = 2.dp, end = 1.dp + max(0f, notePad).dp),
                style =
                    TextStyle(
                        textMotion = TextMotion.Animated,
                        fontFamily = serif,
                        fontStyle = FontStyle.Italic,
                        fontSize = 13.sp,
                        lineHeight = 1.45.em,
                        color = colors.muted,
                    ),
            )
        }
    }
}

// The card's box-shadow: 0 6 18 -10 rgba(120, 70, 20, .35), its corners 14 less the 10 spread.
private fun DrawScope.cardShadow() {
    drawIntoCanvas { canvas ->
        val c = Canvas2D(canvas, size.width.toInt(), size.height.toInt())
        val d = density
        c.scale(d, d)
        c.shadowColor = "rgba(120,70,20,.35)"
        c.shadowBlur = 18 * d
        c.shadowOffsetY = 6 * d
        c.fillStyle = "rgba(0,0,0,0)"
        c.beginPath()
        c.roundRect(10f, 10f, size.width / d - 20, size.height / d - 20, 4f)
        c.fill()
    }
}

/** The three points: a bullet each, and their letters as they are written in. */
@Composable
private fun Points(
    state: FrenchPressState,
    laid: PressText,
    colors: FrenchPressScreenColors,
    placer: Placer,
) {
    val amber = colors.amber
    val ink = colors.ink
    // Read whole, as the prototype's list items are; the card hides it while it is shut.
    val read = remember(laid) { laid.pointLayouts.joinToString("\n") { it.layoutInput.text.text } }
    Spacer(
        // The list's 8 above; its 4 below collapses into the footer's 8, as CSS margins do.
        Modifier.padding(top = 8.dp)
            .semantics { text = AnnotatedString(read) }
            .fillMaxWidth()
            .height(laid.pointsHeight.dp)
            .drawBehind {
                state.frame
                val d = density
                // The list's disc markers, in the bullets' 18 to the left of the text.
                for (p in laid.pointLayouts.indices) {
                    val l = laid.pointLayouts[p]
                    val y = (laid.pointTops[p] + l.getLineBaseline(0) / d - BulletRise) * d
                    drawCircle(amber, BulletRadius * d, Offset(BulletX * d, y))
                }
                drawIntoCanvas { canvas ->
                    canvas.save()
                    canvas.scale(d, d)
                    canvas.translate(18f, 0f)
                    var c = 0
                    for (p in laid.pointLayouts.indices) {
                        val l = laid.pointLayouts[p]
                        val n = l.layoutInput.text.length
                        var all = true
                        for (i in c until c + n) if (!state.shown[i]) {
                            all = false
                            break
                        }
                        if (all) {
                            canvas.save()
                            canvas.translate(0f, laid.pointTops[p])
                            canvas.scale(1 / d, 1 / d)
                            l.multiParagraph.paint(canvas, ink)
                            canvas.restore()
                        } else {
                            for (i in c until c + n) {
                                if (!state.shown[i] || state.letters[i] == ' ') continue
                                canvas.save()
                                canvas.translate(0f, laid.letterBaseline[i])
                                laid.paintLetterInPlace(canvas, i, ink)
                                canvas.restore()
                            }
                        }
                        c += n
                    }
                    canvas.restore()
                }
            }
            .onGloballyPositioned {
                placer.summary = it
                placer.push()
            },
    )
}

// The browser's disc marker for 14.5 px text: a dot about a third of the x-height round, sitting
// just
// above the middle of the x-height, its centre 12 left of the text.
private const val BulletRadius = 2.45f
private const val BulletRise = 4.6f
private const val BulletX = 5.5f

/** The card's foot: the Full article button and how many words went into the press. */
@Composable
private fun Footer(
    state: FrenchPressState,
    text: FrenchPressText,
    colors: FrenchPressScreenColors,
    placer: Placer,
) {
    val open by remember(state) { derivedStateOf { state.cardOpen > 0f } }
    val line = colors.line
    Row(
        Modifier.fillMaxWidth()
            .padding(top = 8.dp)
            .drawBehind { drawRect(line, size = Size(size.width, 1.dp.toPx())) }
            .padding(top = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.heightIn(min = 40.dp)
                .onGloballyPositioned {
                    placer.full = it
                    placer.push()
                }
                .clickable(
                    enabled = open,
                    role = Role.Button,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    state.takeOver()
                    state.showFull()
                }
                .padding(end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val amber = colors.amber
            Canvas(Modifier.size(16.dp)) {
                // M12 19V6m0 0-5 5m5-5 5 5 in a 24 box, 2 wide, round.
                val s = size.width / 24f
                val w = 2 * s
                drawLine(amber, Offset(12 * s, 19 * s), Offset(12 * s, 6 * s), w, StrokeCap.Round)
                drawLine(amber, Offset(12 * s, 6 * s), Offset(7 * s, 11 * s), w, StrokeCap.Round)
                drawLine(amber, Offset(12 * s, 6 * s), Offset(17 * s, 11 * s), w, StrokeCap.Round)
            }
            BasicText(
                text.full,
                style =
                    TextStyle(
                        textMotion = TextMotion.Animated,
                        fontWeight = FontWeight(650),
                        fontSize = 14.sp,
                        color = colors.amber,
                    ),
                maxLines = 1,
            )
        }
        Spacer(Modifier.weight(1f))
        BasicText(
            text.count.replace("%1\$d", state.words.size.toString()),
            style =
                TextStyle(
                    textMotion = TextMotion.Animated,
                    fontSize = 12.sp,
                    lineHeight = 1.55.em,
                    color = colors.muted,
                ),
            maxLines = 1,
        )
    }
}

/** The press's hit box: a toggle button, pushed and pulled by a finger, pressed with keys. */
@Composable
private fun PressButton(state: FrenchPressState, text: FrenchPressText, ring: Color) {
    state.layoutVersion
    val brewed = state.mode == PressMode.Brewed
    var focused by remember { mutableStateOf(false) }
    val label = if (brewed) text.showFull else text.summarizeArticle
    Box(
        Modifier.offset(state.hitX.dp, state.hitY.dp)
            .size(state.hitW.dp, state.hitH.dp)
            .testTag("press")
            .semantics {
                role = Role.Button
                contentDescription = label
                toggleableState = ToggleableState(brewed)
                onClick {
                    state.takeOver()
                    state.tap()
                    true
                }
            }
            .onFocusChanged { focused = it.isFocused }
            .onKeyEvent { e ->
                val press = e.key == Key.Enter || e.key == Key.Spacebar || e.key == Key.NumPadEnter
                if (!press || e.type != KeyEventType.KeyDown) return@onKeyEvent false
                state.takeOver()
                state.tap()
                true
            }
            .focusable()
            .pointerInput(state) { pressGesture(state) }
            .drawBehind {
                if (!focused) return@drawBehind
                val w = 2.dp.toPx()
                val o = 2.dp.toPx() + w / 2
                drawRoundRect(
                    ring,
                    topLeft = Offset(-o, -o),
                    size = Size(size.width + 2 * o, size.height + 2 * o),
                    cornerRadius = CornerRadius(20.dp.toPx() + o),
                    style = Stroke(w, join = StrokeJoin.Round),
                )
            },
    )
}

// The prototype's pointer handlers on the press: one pointer owns the drag and others are
// ignored; a real touch takes over from the script first; a touch cancelled lets go where the
// finger last was.
private suspend fun PointerInputScope.pressGesture(state: FrenchPressState) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        if (state.heldByFinger) return@awaitEachGesture
        // Only a left click drags, as the prototype ignores the other buttons.
        if (down.type == PointerType.Mouse && !currentEvent.buttons.isPrimaryPressed) {
            return@awaitEachGesture
        }
        state.takeOver()
        val ox = state.hitX
        val oy = state.hitY
        state.pressDown(ox + down.position.x / density, oy + down.position.y / density)
        var ended = false
        try {
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                val x = ox + change.position.x / density
                val y = oy + change.position.y / density
                if (!change.pressed) {
                    ended = true
                    state.pressUp(x, y)
                    break
                }
                if (change.positionChanged()) {
                    change.consume()
                    state.pressMove(x, y)
                }
            }
        } finally {
            if (!ended) state.pressCancel()
        }
    }
}

/** The press's label, "Summarize" or "Full article", to its left; hidden while it works. */
@Composable
private fun PressTag(
    state: FrenchPressState,
    text: FrenchPressText,
    colors: FrenchPressScreenColors,
    mono: FontFamily,
) {
    val mode = state.mode
    val busy = mode != PressMode.Idle && mode != PressMode.Brewed
    val alpha by
        animateFloatAsState(
            if (busy) 0f else 1f,
            if (state.reducedMotion) snap() else tween(200),
            label = "tag",
        )
    state.layoutVersion
    BasicText(
        (if (mode == PressMode.Brewed) text.full else text.summarize).uppercase(),
        Modifier.clearAndSetSemantics {}
            .layout { m, c ->
                val p = m.measure(c.copy(minWidth = 0, minHeight = 0))
                layout(0, 0) {
                    p.place(
                        (state.tagX * density - p.width).roundToInt(),
                        (state.tagY * density - p.height / 2f).roundToInt(),
                    )
                }
            }
            // Inside the placement, so the layer has the label's size, not the zero it is laid
            // out at.
            .graphicsLayer { this.alpha = alpha },
        style =
            TextStyle(
                textMotion = TextMotion.Animated,
                fontFamily = mono,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.5.sp,
                lineHeight = 14.sp,
                lineHeightStyle = CssLine,
                letterSpacing = .84.sp,
                color = colors.amber,
            ),
        maxLines = 1,
        softWrap = false,
    )
}

// CSS's line box: the half-leading on both sides, nothing trimmed.
private val CssLine = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

/** The polite live region: on its own, so an announcement recomposes only it. */
@Composable
private fun Announcer(state: FrenchPressState, text: FrenchPressText) {
    val said = text.said(state.said)
    Box(
        Modifier.size(1.dp).semantics {
            liveRegion = LiveRegionMode.Polite
            if (said.isNotEmpty()) contentDescription = said
        },
    )
}
