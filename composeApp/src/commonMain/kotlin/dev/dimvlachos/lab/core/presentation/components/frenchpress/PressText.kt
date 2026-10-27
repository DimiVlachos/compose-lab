package dev.dimvlachos.lab.core.presentation.components.frenchpress

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import kotlin.math.round

/**
 * The story's words and the summary's letters, laid out as the browser lays out the prototype's
 * spans: each paragraph (and each point) with [TextMeasurer] at its width, and each word (and
 * letter) on its own once, so it can be drawn where the paragraph put it, or anywhere in flight,
 * from the same glyphs. Positions are in dp; layouts are in the device's pixels.
 *
 * [body] is the story's style (its line height a multiple of the font size, centred in the line as
 * CSS's half-leading), [point] the summary's.
 */
internal class PressText(
    measurer: TextMeasurer,
    density: Density,
    paragraphs: List<List<String>>,
    points: List<String>,
    body: TextStyle,
    point: TextStyle,
    bodyWidth: Float,
    pointWidth: Float,
    private val paragraphGap: Float,
    private val pointGap: Float,
) {
    /** The device pixels in a dp, which the layouts are in. */
    val density = density.density
    private val d = this.density

    // The browser's wrap: greedy, no hyphens, the half-leading on both sides of each line.
    private fun css(style: TextStyle) =
        style.copy(
            // Linear metrics, as the browser's fractional advances: lines wrap where its do.
            textMotion = TextMotion.Animated,
            lineBreak = LineBreak.Simple,
            hyphens = Hyphens.None,
            lineHeightStyle =
                LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
        )

    // A word or letter alone: its box is the font's ascent and descent, as a span's is.
    private fun natural(style: TextStyle) = style.copy(lineHeight = TextUnit.Unspecified)

    private val wordCount = paragraphs.sumOf { it.size }

    /** Each paragraph laid out, and where it starts below the body's top, dp. */
    val paragraphLayouts: List<TextLayoutResult>
    val paragraphTops: FloatArray

    /** Each word's left, its centre and its baseline, dp from the body's origin. */
    val wordLeft = FloatArray(wordCount)
    val wordCx = FloatArray(wordCount)
    val wordBaseline = FloatArray(wordCount)

    /** Each word on its own, and what moves its top-left so it is centred on its box's middle. */
    val wordLayouts: Array<TextLayoutResult>
    private val wordOffX = FloatArray(wordCount)

    /** The paragraph of each word, and the index of the first word of each paragraph. */
    val firstWord: IntArray

    /** How tall the body is, dp. */
    val bodyHeight: Float

    /**
     * How far a word's baseline is below the middle of its box (the font's ascent and descent): the
     * prototype's `wordBase`. A word flying is drawn on this baseline below its centre.
     */
    val wordBase: Float
    private val wordAscent: Float

    /** How tall a word's box is, dp. */
    val wordBoxHeight: Float

    val pointLayouts: List<TextLayoutResult>
    val pointTops: FloatArray
    val pointsHeight: Float
    private val letterCount = points.sumOf { it.length }
    val letterCx = FloatArray(letterCount)

    /** Each letter's left edge, where the point's own layout draws it. */
    val letterLeft = FloatArray(letterCount)
    val letterBaseline = FloatArray(letterCount)
    val letterLayouts: Array<TextLayoutResult?>
    private val letterOffX = FloatArray(letterCount)
    val letterBase: Float
    private val letterAscent: Float

    init {
        val bodyStyle = css(body)
        val pointStyle = css(point)
        val bodyC = Constraints(maxWidth = (bodyWidth * d).toInt().coerceAtLeast(1))
        val pointC = Constraints(maxWidth = (pointWidth * d).toInt().coerceAtLeast(1))
        val tops = FloatArray(paragraphs.size)
        val first = IntArray(paragraphs.size)
        var y = 0f
        var w = 0
        val unique = HashMap<String, TextLayoutResult>()
        fun alone(text: String, style: TextStyle) =
            unique.getOrPut(text + "\u0000" + style.hashCode()) {
                measurer.measure(
                    text,
                    natural(style),
                    softWrap = false,
                    maxLines = 1,
                    density = density,
                )
            }
        val probe = alone("Hg", bodyStyle)
        wordAscent = probe.firstBaseline / d
        val descent = probe.size.height / d - wordAscent
        // Rounded as the browser rounds a font's ascent and descent.
        wordBase = (round(wordAscent) - round(descent)) / 2
        wordBoxHeight = wordAscent + descent
        val layouts = ArrayList<TextLayoutResult>(wordCount)
        paragraphLayouts = paragraphs.mapIndexed { pi, ws ->
            val text = ws.joinToString(" ")
            val l = measurer.measure(text, bodyStyle, constraints = bodyC, density = density)
            tops[pi] = y
            first[pi] = w
            var at = 0
            for (word in ws) {
                val line = l.getLineForOffset(at)
                val left = l.getHorizontalPosition(at, true) / d
                val one = alone(word, bodyStyle)
                val width = one.size.width / d
                wordLeft[w] = left
                wordCx[w] = left + width / 2
                wordBaseline[w] = y + l.getLineBaseline(line) / d
                wordOffX[w] = -width / 2
                layouts += one
                at += word.length + 1
                w++
            }
            y += l.size.height / d + paragraphGap
            l
        }
        paragraphTops = tops
        firstWord = first
        bodyHeight = y
        wordLayouts = layouts.toTypedArray()

        val lp = alone("Hg", pointStyle)
        letterAscent = lp.firstBaseline / d
        val ld = lp.size.height / d - letterAscent
        letterBase = (round(letterAscent) - round(ld)) / 2
        val ptops = FloatArray(points.size)
        val letters = arrayOfNulls<TextLayoutResult>(letterCount)
        var py = 0f
        var c = 0
        pointLayouts = points.mapIndexed { pi, s ->
            val l = measurer.measure(s, pointStyle, constraints = pointC, density = density)
            ptops[pi] = py
            for (i in s.indices) {
                val box = l.getBoundingBox(i)
                val line = l.getLineForOffset(i)
                letterCx[c] = (box.left + box.right) / 2 / d
                letterLeft[c] = box.left / d
                letterBaseline[c] = py + l.getLineBaseline(line) / d
                if (s[i] != ' ') {
                    val one = alone(s[i].toString(), pointStyle)
                    letters[c] = one
                    letterOffX[c] = -one.size.width / d / 2
                }
                c++
            }
            py += l.size.height / d + pointGap
            l
        }
        pointTops = ptops
        pointsHeight = py - pointGap
        letterLayouts = letters
    }

    /** Word [i]'s box centre, dp from the body's origin, as the prototype's span's middle. */
    fun wordCy(i: Int) = wordBaseline[i] - wordBase

    fun letterCy(i: Int) = letterBaseline[i] - letterBase

    /**
     * Draws word [i] in [color] on [canvas] (in dp), centred across 0 with its baseline [base]
     * below 0: where it is in flight, after the caller has moved there.
     */
    fun paintWord(canvas: Canvas, i: Int, color: Color, base: Float) {
        canvas.translate(wordOffX[i], base - wordAscent)
        canvas.scale(1 / d, 1 / d)
        wordLayouts[i].multiParagraph.paint(canvas, color)
    }

    /**
     * Draws letter [i] in [color] where the point's own layout puts it, its baseline at 0 after the
     * caller has moved there, so nothing shifts when the whole point takes over.
     */
    fun paintLetterInPlace(canvas: Canvas, i: Int, color: Color) {
        val l = letterLayouts[i] ?: return
        canvas.translate(letterLeft[i], -letterAscent)
        canvas.scale(1 / d, 1 / d)
        l.multiParagraph.paint(canvas, color)
    }

    fun paintLetter(canvas: Canvas, i: Int, color: Color, base: Float) {
        val l = letterLayouts[i] ?: return
        canvas.translate(letterOffX[i], base - letterAscent)
        canvas.scale(1 / d, 1 / d)
        l.multiParagraph.paint(canvas, color)
    }
}
