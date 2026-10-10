package dev.dimvlachos.lab.popupdemo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import dev.dimvlachos.lab.core.presentation.components.popup.PieceMotion
import dev.dimvlachos.lab.core.presentation.components.popup.PopUpPiece
import dev.dimvlachos.lab.core.presentation.components.popup.PopUpSide
import dev.dimvlachos.lab.core.presentation.components.popup.PopUpSpread
import dev.dimvlachos.lab.core.presentation.components.popup.PullTab
import dev.dimvlachos.lab.core.presentation.ui.CycladesPaper as Ink
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.popup_cover_subtitle
import dev.dimvlachos.lab.resources.popup_cover_title
import dev.dimvlachos.lab.resources.popup_footer_1_line
import dev.dimvlachos.lab.resources.popup_footer_1_title
import dev.dimvlachos.lab.resources.popup_footer_2_line
import dev.dimvlachos.lab.resources.popup_footer_2_title
import dev.dimvlachos.lab.resources.popup_footer_3_line
import dev.dimvlachos.lab.resources.popup_footer_3_title
import dev.dimvlachos.lab.resources.popup_pull
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import org.jetbrains.compose.resources.stringResource

/** The Cyclades pop-up book: its cover and its three spreads. */
internal class CycladesBook(val cover: Painter, val spreads: List<PopUpSpread>)

/**
 * The book's art, ported from the HTML prototype: every page and piece is drawn in book units (a
 * page is 300 by 190) with paths, gradients and printed text.
 */
@Composable
internal fun rememberCycladesBook(): CycladesBook {
    val measurer = rememberTextMeasurer()
    val words =
        Words(
            coverTitle = stringResource(Res.string.popup_cover_title),
            coverSubtitle = stringResource(Res.string.popup_cover_subtitle),
            footers =
                listOf(
                    stringResource(Res.string.popup_footer_1_title) to
                        stringResource(Res.string.popup_footer_1_line),
                    stringResource(Res.string.popup_footer_2_title) to
                        stringResource(Res.string.popup_footer_2_line),
                    stringResource(Res.string.popup_footer_3_title) to
                        stringResource(Res.string.popup_footer_3_line),
                ),
            pull = stringResource(Res.string.popup_pull),
        )
    return remember(measurer, words) { cycladesBook(Print(measurer), words) }
}

private data class Words(
    val coverTitle: String,
    val coverSubtitle: String,
    val footers: List<Pair<String, String>>,
    val pull: String,
)

private const val PageW = 300f
private const val PageD = 190f
private const val BoardW = 314f
private const val BoardD = 198f

private fun cycladesBook(print: Print, words: Words): CycladesBook {
    val s1Far = Art(PageW, PageD) { w, h -> s1Far(w, h) }
    val s3Near = Art(PageW, PageD) { w, h -> s3Near(w, h, print, words) }
    val spreads =
        listOf(
            PopUpSpread(
                near = Art(PageW, PageD) { w, h -> s1Near(w, h, print, words) },
                // The cover's inside: the cloth board with the first far page pasted on.
                far = Art(BoardW, BoardD) { w, h -> boarded(w, h, s1Far, atTop = false) },
                pieces =
                    listOf(
                        PopUpPiece(
                            Art(280f, 92f) { w, h -> hills(w, h) },
                            PopUpSide.Far,
                            40f,
                            -140f,
                            280f,
                            92f,
                        ),
                        PopUpPiece(
                            Art(130f, 150f) { w, h -> chapel(w, h) },
                            PopUpSide.Near,
                            0f,
                            -112f,
                            130f,
                            150f,
                        ),
                        PopUpPiece(
                            Art(150f, 50f) { w, h -> wall(w, h) },
                            PopUpSide.Near,
                            70f,
                            -4f,
                            150f,
                            50f,
                        ),
                    ),
            ),
            PopUpSpread(
                near = Art(PageW, PageD) { w, h -> s2Near(w, h, print, words) },
                far = Art(PageW, PageD) { w, h -> s2Far(w, h) },
                pieces =
                    listOf(
                        PopUpPiece(
                            Art(280f, 80f) { w, h -> island(w, h) },
                            PopUpSide.Far,
                            36f,
                            -140f,
                            280f,
                            80f,
                        ),
                        PopUpPiece(
                            Art(100f, 150f) { w, h -> lighthouse(w, h) },
                            PopUpSide.Near,
                            0f,
                            -144f,
                            100f,
                            150f,
                        ),
                        PopUpPiece(
                            Art(90f, 74f) { w, h -> boat(w, h) },
                            PopUpSide.Near,
                            74f,
                            -122f,
                            90f,
                            74f,
                            motion = PieceMotion.RidesTab(from = -122f, to = 28f, bob = 1.6f),
                        ),
                        PopUpPiece(
                            Art(300f, 34f) { w, h -> waves(w, h) },
                            PopUpSide.Near,
                            84f,
                            -150f,
                            300f,
                            34f,
                        ),
                    ),
                tab = PullTab(),
            ),
            PopUpSpread(
                // The back board: the last near page pasted on the cloth.
                near = Art(BoardW, BoardD) { w, h -> boarded(w, h, s3Near, atTop = true) },
                far = Art(PageW, PageD) { w, h -> s3Far(w, h) },
                pieces =
                    listOf(
                        PopUpPiece(
                            Art(280f, 110f) { w, h -> sunset(w, h) },
                            PopUpSide.Far,
                            40f,
                            -140f,
                            280f,
                            110f,
                        ),
                        PopUpPiece(
                            Art(100f, 128f) { w, h -> windmill(w, h) },
                            PopUpSide.Near,
                            0f,
                            10f,
                            100f,
                            128f,
                        ),
                        PopUpPiece(
                            Art(124f, 124f) { w, _ -> sails(w) },
                            PopUpSide.Near,
                            3f,
                            -2f,
                            124f,
                            124f,
                            lift = 34f,
                            castsShadow = false,
                            motion = PieceMotion.Spins(pivotX = 62f, pivotY = 62f),
                        ),
                        PopUpPiece(
                            Art(140f, 50f) { w, h -> cactusWall(w, h) },
                            PopUpSide.Near,
                            72f,
                            -150f,
                            140f,
                            50f,
                        ),
                    ),
                tab = PullTab(),
            ),
        )
    val cover = Art(BoardW, BoardD) { w, h -> cover(w, h, print, words) }
    return CycladesBook(cover, spreads)
}

/** Art drawn in its own units, [w] by [h], scaled to whatever it is drawn into. */
private class Art(
    private val w: Float,
    private val h: Float,
    private val paint: DrawScope.(Float, Float) -> Unit,
) : Painter() {
    override val intrinsicSize: Size = Size(w, h)

    override fun DrawScope.onDraw() {
        scale(size.width / w, size.height / h, pivot = Offset.Zero) { paint(w, h) }
    }
}

/** Text on the paper, measured in the units the art is drawn in. */
private class Print(private val measurer: TextMeasurer) {
    fun DrawScope.write(
        text: String,
        x: Float,
        baseline: Float,
        units: Float,
        color: Color,
        weight: FontWeight = FontWeight.Normal,
        family: FontFamily = FontFamily.SansSerif,
        centred: Boolean = false,
        alignEnd: Boolean = false,
    ) {
        val style =
            TextStyle(
                color = color,
                fontSize = units.toSp(),
                fontWeight = weight,
                fontFamily = family,
            )
        val layout = measurer.measure(text, style, density = this)
        val left =
            when {
                centred -> x - layout.size.width / 2f
                alignEnd -> x - layout.size.width
                else -> x
            }
        drawText(layout, topLeft = Offset(left, baseline - layout.firstBaseline))
    }
}

// The prototype's seeded random numbers (mulberry32), so the art is the same every time.
private class Seeded(private var seed: Int) {
    fun next(): Float {
        seed += 0x6D2B79F5
        var t = (seed xor (seed ushr 15)) * (1 or seed)
        t = (t + (t xor (t ushr 7)) * (61 or t)) xor t
        return ((t xor (t ushr 14)).toUInt().toDouble() / 4294967296.0).toFloat()
    }
}

private val outline = Stroke(width = 0.9f, join = StrokeJoin.Round)

private fun DrawScope.inked(path: Path, fill: Color) {
    drawPath(path, fill)
    drawPath(path, Ink.outline, style = outline)
}

private fun DrawScope.inked(path: Path, fill: Brush) {
    drawPath(path, fill)
    drawPath(path, Ink.outline, style = outline)
}

private fun DrawScope.inkedRect(x: Float, y: Float, w: Float, h: Float, fill: Color) {
    drawRect(fill, Offset(x, y), Size(w, h))
    drawRect(Ink.outline, Offset(x, y), Size(w, h), style = outline)
}

private fun path(build: Path.() -> Unit) = Path().apply(build)

// An arc of a circle about (cx, cy), from [from] to [to] radians, clockwise on the page.
private fun Path.arc(
    cx: Float,
    cy: Float,
    r: Float,
    from: Float,
    to: Float,
    move: Boolean = false,
) = arcTo(Rect(cx - r, cy - r, cx + r, cy + r), degrees(from), degrees(to - from), move)

private fun degrees(radians: Float) = radians * 180f / PI.toFloat()

private val pi = PI.toFloat()

// Paper: a warm sheet with faint specks.
private fun DrawScope.paper(w: Float, h: Float, seed: Int) {
    drawRect(Ink.paper, size = Size(w, h))
    val r = Seeded(seed)
    repeat((w * h / 30f).toInt()) {
        val alpha = r.next() * 0.06f
        drawRect(
            Ink.speck.copy(alpha = alpha),
            Offset(r.next() * w, r.next() * h),
            Size(0.7f, 0.7f),
        )
    }
}

// The shade where a page runs into the gutter: along its top or its bottom.
private fun DrawScope.gutter(w: Float, h: Float, atTop: Boolean) {
    val from = if (atTop) 0f else h
    val to = if (atTop) h * 0.16f else h * 0.84f
    drawRect(
        Brush.verticalGradient(
            listOf(Ink.gutterShade, Color.Transparent),
            startY = from,
            endY = to,
        ),
        size = Size(w, h),
    )
}

private fun DrawScope.printed(print: Print, footer: Pair<String, String>, w: Float, h: Float) {
    drawRect(Ink.paper, Offset(0f, h - 44f), Size(w, 44f))
    with(print) {
        write(footer.first, 18f, h - 26f, 9f, Ink.printTitle, FontWeight.Bold)
        write(footer.second, 18f, h - 14f, 7.5f, Ink.printLine)
    }
}

// The slit a tab comes out of, and its label.
private fun DrawScope.slit(print: Print, words: Words, w: Float) {
    with(print) {
        write(words.pull, w - 8f, 152.5f, 6.5f, Ink.pullLabel, FontWeight.Bold, alignEnd = true)
    }
    drawLine(Ink.slit, Offset(w - 3f, 139f), Offset(w - 3f, 161f), strokeWidth = 0.8f)
}

private fun DrawScope.birds(points: List<Triple<Float, Float, Float>>, color: Color) {
    val wing = Stroke(width = 0.9f, cap = StrokeCap.Round)
    for ((bx, by, s) in points) {
        drawPath(
            path {
                moveTo(bx - 4 * s, by - 1.5f * s)
                quadraticTo(bx - 2 * s, by - 3 * s, bx, by)
                quadraticTo(bx + 2 * s, by - 3 * s, bx + 4 * s, by - 1.5f * s)
            },
            color,
            style = wing,
        )
    }
}

private fun DrawScope.weave(w: Float, h: Float) {
    drawRect(Ink.cloth, size = Size(w, h))
    var i = 0f
    while (i < w) {
        drawLine(Ink.clothThread, Offset(i, 0f), Offset(i, h), strokeWidth = 0.5f)
        i += 2f
    }
    var j = 0f
    while (j < h) {
        drawLine(Ink.clothThread, Offset(0f, j), Offset(w, j), strokeWidth = 0.5f)
        j += 2f
    }
}

private fun DrawScope.circle(color: Color, cx: Float, cy: Float, r: Float) =
    drawCircle(color, r, Offset(cx, cy))

// A page pasted on a cloth board, against the board's gutter edge.
private fun DrawScope.boarded(w: Float, h: Float, page: Art, atTop: Boolean) {
    weave(w, h)
    translate((w - PageW) / 2f, if (atTop) 0f else h - PageD) {
        with(page) { draw(Size(PageW, PageD)) }
    }
}

private fun DrawScope.sunburst(
    cx: Float,
    cy: Float,
    inner: Float,
    outer: Float,
    color: Color,
    width: Float,
) {
    for (i in 0 until 12) {
        val a = i / 12f * 2f * pi
        drawLine(
            color,
            Offset(cx + cos(a) * inner, cy + sin(a) * inner),
            Offset(cx + cos(a) * outer, cy + sin(a) * outer),
            strokeWidth = width,
            cap = StrokeCap.Round,
        )
    }
}

// Little arcs of foam in rows across the sea.
private fun DrawScope.ripples(
    w: Float,
    until: Float,
    rowStep: Float,
    colStep: Float,
    r: Float,
    color: Color,
) {
    val stroke = Stroke(width = 0.9f)
    var y = rowStep
    var k = 0
    while (y < until) {
        var i = (k % 2) * colStep / 2f
        while (i < w) {
            drawPath(
                path { arc(i + r, y + 4f, r, pi * 1.15f, pi * 1.85f, move = true) },
                color,
                style = stroke,
            )
            i += colStep
        }
        y += rowStep
        k++
    }
}

// ---------- Pages ----------

private fun DrawScope.s1Far(w: Float, h: Float) {
    paper(w, h, 11)
    drawRect(
        Brush.verticalGradient(
            0f to Ink.skyTop1,
            0.72f to Ink.skyMid1,
            1f to Ink.skyLow1,
            endY = h,
        ),
        size = Size(w, h),
    )
    circle(Ink.sun, w * 0.8f, h * 0.27f, 17f)
    sunburst(w * 0.8f, h * 0.27f, 22f, 29f, Ink.sunRay, 1.5f)
    birds(listOf(Triple(70f, 44f, 1f), Triple(86f, 36f, 0.8f), Triple(102f, 50f, 0.7f)), Ink.bird1)
    gutter(w, h, atTop = false)
}

private fun DrawScope.s1Near(w: Float, h: Float, print: Print, words: Words) {
    paper(w, h, 12)
    val sea = h * 0.34f
    drawRect(
        Brush.verticalGradient(listOf(Ink.seaDeep, Ink.seaShallow), endY = sea),
        size = Size(w, sea),
    )
    ripples(w, sea - 4f, 9f, 28f, 7f, Ink.foam)
    drawPath(
        path {
            moveTo(0f, sea)
            var i = 0f
            while (i <= w) {
                quadraticTo(i + 10f, sea - 3f, i + 20f, sea)
                i += 20f
            }
            lineTo(w, sea + 10f)
            lineTo(0f, sea + 10f)
            close()
        },
        Ink.sand,
    )
    drawRect(Ink.paving, Offset(0f, sea + 10f), Size(w, h - 44f - sea - 10f))
    val r = Seeded(5)
    val joint = Stroke(width = 1.5f)
    var y = sea + 12f
    var k = 0
    while (y < h - 50f) {
        var x = -(k % 2) * 12f
        while (x < w) {
            val sw = 18f + r.next() * 16f
            drawRoundRect(
                Ink.pavingLine,
                Offset(x + 1f, y + 1f),
                Size(sw - 2f, 13f),
                androidx.compose.ui.geometry.CornerRadius(4f),
                style = joint,
            )
            x += sw
        }
        y += 15f
        k++
    }
    printed(print, words.footers[0], w, h)
    gutter(w, h, atTop = true)
}

private fun DrawScope.s2Far(w: Float, h: Float) {
    paper(w, h, 21)
    drawRect(Brush.verticalGradient(listOf(Ink.skyTop2, Ink.skyLow2), endY = h), size = Size(w, h))
    for ((a, b, s) in
        listOf(Triple(60f, 50f, 1f), Triple(200f, 34f, 1.3f), Triple(250f, 80f, 0.8f))) {
        circle(Ink.cloud, a, b, 9 * s)
        circle(Ink.cloud, a + 11 * s, b - 4 * s, 11 * s)
        circle(Ink.cloud, a + 24 * s, b, 8 * s)
        drawRect(Ink.cloud, Offset(a, b), Size(24 * s, 8 * s))
    }
    birds(listOf(Triple(130f, 60f, 1f), Triple(146f, 52f, 0.8f)), Ink.bird2)
    gutter(w, h, atTop = false)
}

private fun DrawScope.s2Near(w: Float, h: Float, print: Print, words: Words) {
    paper(w, h, 22)
    drawRect(
        Brush.verticalGradient(listOf(Ink.openSeaTop, Ink.openSeaLow), endY = h - 44f),
        size = Size(w, h - 44f),
    )
    ripples(w, h - 50f, 10f, 22f, 6f, Ink.foamFaint)
    printed(print, words.footers[1], w, h)
    slit(print, words, w)
    gutter(w, h, atTop = true)
}

private fun DrawScope.s3Far(w: Float, h: Float) {
    paper(w, h, 31)
    drawRect(
        Brush.verticalGradient(0f to Ink.duskTop, 0.6f to Ink.duskMid, 1f to Ink.duskLow, endY = h),
        size = Size(w, h),
    )
    birds(listOf(Triple(60f, 70f, 1f), Triple(74f, 62f, 0.8f), Triple(228f, 48f, 0.9f)), Ink.bird3)
    gutter(w, h, atTop = false)
}

private fun DrawScope.s3Near(w: Float, h: Float, print: Print, words: Words) {
    paper(w, h, 32)
    drawRect(Ink.meadow, size = Size(w, h - 44f))
    drawPath(
        path {
            moveTo(w * 0.62f, 0f)
            cubicTo(w * 0.5f, 40f, w * 0.78f, 80f, w * 0.55f, h - 44f)
        },
        Ink.path,
        style = Stroke(width = 11f, cap = StrokeCap.Round),
    )
    val r = Seeded(9)
    repeat(70) {
        val color = if (r.next() > 0.4f) Ink.shrub else Ink.shrubLight
        circle(color, r.next() * w, r.next() * (h - 50f), 1.5f + r.next() * 3f)
    }
    repeat(40) { circle(Ink.pebble, r.next() * w, r.next() * (h - 50f), 0.8f + r.next() * 1.4f) }
    printed(print, words.footers[2], w, h)
    slit(print, words, w)
    gutter(w, h, atTop = true)
}

private fun DrawScope.cover(w: Float, h: Float, print: Print, words: Words) {
    weave(w, h)
    drawRect(Ink.gilt, Offset(10f, 10f), Size(w - 20f, h - 20f), style = Stroke(width = 0.8f))
    val sx = w / 2f
    val sy = h * 0.36f
    drawCircle(Ink.giltBright, 15f, Offset(sx, sy), style = Stroke(width = 1.4f))
    sunburst(sx, sy, 19f, 26f, Ink.giltBright, 1.4f)
    drawPath(
        path {
            for (i in 0..80) {
                val x = sx - 40f + i
                val y = sy + 36f + sin(i / 80f * pi * 4f) * 2.5f
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
        },
        Ink.giltBright,
        style = Stroke(width = 1.4f),
    )
    with(print) {
        write(
            words.coverTitle,
            sx,
            h * 0.66f,
            30f,
            Ink.coverTitle,
            FontWeight.ExtraBold,
            centred = true,
        )
        write(
            words.coverSubtitle,
            sx,
            h * 0.66f + 17f,
            6.5f,
            Ink.gilt,
            family = FontFamily.Monospace,
            centred = true,
        )
    }
    gutter(w, h, atTop = true)
}

// ---------- Pieces: drawn with their foot along the bottom edge ----------

private fun DrawScope.hills(w: Float, h: Float) {
    inked(
        path {
            moveTo(0f, h)
            lineTo(0f, 50f)
            quadraticTo(40f, 26f, 84f, 42f)
            quadraticTo(126f, 16f, 172f, 36f)
            quadraticTo(224f, 10f, 280f, 38f)
            lineTo(280f, h)
            close()
        },
        Ink.hillsFar,
    )
    for ((hx, hy) in listOf(150f to 38f, 158f to 34f, 166f to 37f, 174f to 33f, 160f to 42f)) {
        drawRect(Ink.whitewash, Offset(hx, hy), Size(7f, 6f))
        drawRect(Ink.aegean, Offset(hx + 2.5f, hy + 3f), Size(2f, 3f))
    }
    inked(
        path {
            moveTo(0f, h)
            lineTo(0f, 70f)
            quadraticTo(60f, 52f, 112f, 64f)
            quadraticTo(172f, 44f, 232f, 62f)
            quadraticTo(258f, 56f, 280f, 64f)
            lineTo(280f, h)
            close()
        },
        Ink.hillsNear,
    )
}

private fun DrawScope.chapel(w: Float, h: Float) {
    // The bell tower, with its bell in the arch.
    inked(
        path {
            moveTo(8f, h)
            lineTo(8f, 58f)
            quadraticTo(25f, 38f, 42f, 58f)
            lineTo(42f, h)
            close()
        },
        Ink.whitewash,
    )
    drawPath(
        path {
            moveTo(19f, 80f)
            lineTo(19f, 68f)
            quadraticTo(25f, 60f, 31f, 68f)
            lineTo(31f, 80f)
            close()
        },
        Ink.doorway,
    )
    circle(Ink.bell, 25f, 71f, 3.4f)
    // The nave, the drum and the blue dome with its cross.
    inkedRect(34f, h - 74f, 78f, 74f, Ink.whitewash)
    inkedRect(52f, h - 84f, 44f, 10f, Ink.whitewash)
    inked(
        path {
            arc(74f, h - 84f, 24f, pi, 2f * pi)
            close()
        },
        Brush.horizontalGradient(
            0f to Ink.domeDark,
            0.45f to Ink.domeLight,
            1f to Ink.domeDeep,
            startX = 50f,
            endX = 98f,
        ),
    )
    inkedRect(71.5f, h - 116f, 5f, 8f, Ink.whitewash)
    val cross = Stroke(width = 1.6f)
    drawLine(Ink.aegean, Offset(74f, h - 129f), Offset(74f, h - 116f), strokeWidth = cross.width)
    drawLine(Ink.aegean, Offset(70f, h - 125f), Offset(78f, h - 125f), strokeWidth = cross.width)
    // Its door and window, a shade down its side, and the steps.
    drawPath(
        path {
            moveTo(66f, h)
            lineTo(66f, h - 22f)
            quadraticTo(74f, h - 32f, 82f, h - 22f)
            lineTo(82f, h)
            close()
        },
        Ink.aegean,
    )
    drawPath(
        path {
            moveTo(94f, h - 42f)
            lineTo(94f, h - 52f)
            quadraticTo(98f, h - 57f, 102f, h - 52f)
            lineTo(102f, h - 42f)
            close()
        },
        Ink.aegean,
    )
    drawRect(Ink.wallShade, Offset(98f, h - 74f), Size(14f, 74f))
    inkedRect(0f, h - 6f, 34f, 6f, Ink.whitewash)
    inkedRect(4f, h - 11f, 30f, 5f, Ink.whitewash)
}

private fun DrawScope.wall(w: Float, h: Float) {
    inked(
        path {
            moveTo(0f, h)
            lineTo(0f, h - 22f)
            quadraticTo(0f, h - 26f, 4f, h - 26f)
            lineTo(w - 4f, h - 26f)
            quadraticTo(w, h - 26f, w, h - 22f)
            lineTo(w, h)
            close()
        },
        Ink.whitewashWarm,
    )
    val r = Seeded(3)
    repeat(16) {
        val cx = 10f + r.next() * 86f
        val cy = h - 30f - r.next() * 14f
        val turn = r.next() * pi
        rotateRad(turn, Offset(cx, cy)) {
            drawOval(Ink.leaf, Offset(cx - 4f, cy - 2.4f), Size(8f, 4.8f))
        }
    }
    for (i in 0 until 46) {
        val cx = 8f + r.next() * 92f
        val cy = h - 26f - r.next() * 18f + (r.next() - 0.5f) * 4f
        circle(Ink.bougainvillea[i % 4], cx, cy, 2.4f + r.next() * 2.4f)
    }
    inked(
        path {
            moveTo(116f, h - 26f)
            lineTo(136f, h - 26f)
            lineTo(133f, h - 40f)
            lineTo(119f, h - 40f)
            close()
        },
        Ink.aegean,
    )
    for (i in 0 until 7) {
        val cx = 126f + (i - 3) * 2.6f
        val cy = h - 44f - (i % 3) * 3f
        rotateRad((i - 3) * 0.3f, Offset(cx, cy)) {
            drawOval(Ink.leafDark, Offset(cx - 2.2f, cy - 5.5f), Size(4.4f, 11f))
        }
    }
}

private fun DrawScope.island(w: Float, h: Float) {
    inked(
        path {
            moveTo(0f, h)
            lineTo(0f, 46f)
            quadraticTo(50f, 26f, 100f, 36f)
            quadraticTo(150f, 8f, 200f, 30f)
            quadraticTo(240f, 22f, 280f, 40f)
            lineTo(280f, h)
            close()
        },
        Ink.islandFar,
    )
    for ((hx, hy) in
        listOf(
            138f to 26f,
            146f to 22f,
            154f to 25f,
            162f to 21f,
            170f to 26f,
            150f to 31f,
            160f to 30f,
        )) {
        drawRect(Ink.whitewash, Offset(hx, hy), Size(7f, 6f))
    }
    drawPath(
        path {
            arc(158f, 21f, 3.4f, pi, 2f * pi)
            close()
        },
        Ink.aegean,
    )
    inked(
        path {
            moveTo(0f, h)
            lineTo(0f, 64f)
            quadraticTo(90f, 52f, 160f, 62f)
            quadraticTo(220f, 56f, 280f, 64f)
            lineTo(280f, h)
            close()
        },
        Ink.islandNear,
    )
}

private fun DrawScope.lighthouse(w: Float, h: Float) {
    inked(
        path {
            moveTo(0f, h)
            lineTo(6f, h - 26f)
            lineTo(22f, h - 38f)
            lineTo(48f, h - 44f)
            lineTo(74f, h - 36f)
            lineTo(92f, h - 22f)
            lineTo(100f, h)
            close()
        },
        Ink.rock,
    )
    drawLine(Ink.rockCrack, Offset(12f, h - 18f), Offset(40f, h - 26f), strokeWidth = 0.9f)
    drawLine(Ink.rockCrack, Offset(56f, h - 30f), Offset(84f, h - 20f), strokeWidth = 0.9f)
    inked(
        path {
            moveTo(39f, h - 42f)
            lineTo(43f, 36f)
            lineTo(57f, 36f)
            lineTo(61f, h - 42f)
            close()
        },
        Ink.whitewash,
    )
    drawPath(
        path {
            moveTo(52f, h - 42f)
            lineTo(53f, 36f)
            lineTo(57f, 36f)
            lineTo(61f, h - 42f)
            close()
        },
        Ink.towerShade,
    )
    drawRect(Ink.doorway, Offset(38f, 30f), Size(24f, 6f))
    inkedRect(43f, 17f, 14f, 13f, Ink.lamp)
    drawPath(
        path {
            moveTo(41f, 17f)
            lineTo(50f, 8f)
            lineTo(59f, 17f)
            close()
        },
        Ink.doorway,
    )
    inkedRect(64f, h - 58f, 22f, 18f, Ink.whitewash)
    drawRect(Ink.aegean, Offset(72f, h - 50f), Size(6f, 10f))
}

private fun DrawScope.boat(w: Float, h: Float) {
    drawLine(Ink.mast, Offset(52f, 6f), Offset(52f, h - 26f), strokeWidth = 1.8f)
    for (i in 0 until 5) {
        drawRect(
            if (i % 2 == 1) Ink.whitewash else Ink.aegean,
            Offset(53f, 6f + i * 2.2f),
            Size(14f, 2.2f),
        )
    }
    inked(
        path {
            moveTo(53f, 20f)
            lineTo(53f, h - 30f)
            lineTo(80f, h - 30f)
            close()
        },
        Ink.whitewash,
    )
    inkedRect(26f, h - 40f, 26f, 14f, Ink.whitewash)
    drawRect(Ink.aegean, Offset(30f, h - 36f), Size(5f, 5f))
    drawRect(Ink.aegean, Offset(39f, h - 36f), Size(5f, 5f))
    val hull = path {
        moveTo(3f, h - 27f)
        lineTo(87f, h - 27f)
        quadraticTo(85f, h - 7f, 70f, h - 2f)
        lineTo(18f, h - 2f)
        quadraticTo(6f, h - 10f, 3f, h - 27f)
        close()
    }
    drawPath(hull, Ink.whitewash)
    clipPath(hull) {
        drawRect(Ink.aegean, Offset(0f, h - 21f), Size(w, 4.5f))
        drawRect(Ink.hullStripe, Offset(0f, h - 9f), Size(w, 8f))
    }
    drawPath(hull, Ink.outline, style = outline)
}

private fun DrawScope.waves(w: Float, h: Float) {
    inked(
        path {
            moveTo(0f, h)
            lineTo(0f, 14f)
            var i = 0f
            while (i < w) {
                quadraticTo(i + 12.5f, 0f, i + 25f, 14f)
                i += 25f
            }
            lineTo(w, h)
            close()
        },
        Brush.verticalGradient(listOf(Ink.waveTop, Ink.waveLow), endY = h),
    )
    val crest = Stroke(width = 1.6f, cap = StrokeCap.Round)
    val faint = Stroke(width = 1f, cap = StrokeCap.Round)
    var i = 0f
    while (i < w) {
        drawPath(
            path {
                moveTo(i + 4f, 10f)
                quadraticTo(i + 12.5f, 1.5f, i + 21f, 10f)
            },
            Ink.waveCrest,
            style = crest,
        )
        i += 25f
    }
    i = 12f
    while (i < w) {
        drawPath(
            path {
                moveTo(i + 4f, 24f)
                quadraticTo(i + 12.5f, 17f, i + 21f, 24f)
            },
            Ink.waveFaint,
            style = faint,
        )
        i += 25f
    }
}

private fun DrawScope.sunset(w: Float, h: Float) {
    circle(Ink.sunGlow, 190f, 54f, 42f)
    circle(Ink.sunsetSun, 190f, 54f, 33f)
    inked(
        path {
            moveTo(0f, h)
            lineTo(0f, 64f)
            quadraticTo(70f, 46f, 140f, 60f)
            quadraticTo(210f, 40f, 280f, 58f)
            lineTo(280f, h)
            close()
        },
        Ink.ridgeFar,
    )
    inked(
        path {
            moveTo(0f, h)
            lineTo(0f, 84f)
            quadraticTo(80f, 70f, 150f, 82f)
            quadraticTo(220f, 68f, 280f, 80f)
            lineTo(280f, h)
            close()
        },
        Ink.ridgeNear,
    )
}

private fun DrawScope.windmill(w: Float, h: Float) {
    inked(
        path {
            moveTo(22f, h)
            lineTo(26f, 38f)
            lineTo(74f, 38f)
            lineTo(78f, h)
            close()
        },
        Brush.horizontalGradient(
            0f to Ink.towerLeft,
            0.4f to Ink.towerLight,
            1f to Ink.towerRight,
            startX = 22f,
            endX = 78f,
        ),
    )
    inked(
        path {
            moveTo(17f, 42f)
            quadraticTo(50f, -6f, 83f, 42f)
            close()
        },
        Ink.thatch,
    )
    for (i in 1 until 7) drawLine(
        Ink.thatchLine,
        Offset(17f + i * 9.4f, 42f),
        Offset(50f, 6f),
        strokeWidth = 0.7f,
    )
    drawPath(
        path {
            moveTo(44f, h)
            lineTo(44f, h - 18f)
            quadraticTo(50f, h - 26f, 56f, h - 18f)
            lineTo(56f, h)
            close()
        },
        Ink.aegean,
    )
    drawRect(Ink.aegean, Offset(47f, 66f), Size(7f, 8f))
}

// Eight cloth sails round a hub at the middle; the book spins them.
private fun DrawScope.sails(w: Float) {
    val c = w / 2f
    for (i in 0 until 8) {
        val t = i * pi / 4f
        val t2 = t + 0.3f
        inked(
            path {
                moveTo(c + cos(t) * 10f, c + sin(t) * 10f)
                lineTo(c + cos(t) * 58f, c + sin(t) * 58f)
                lineTo(c + cos(t2) * 50f, c + sin(t2) * 50f)
                close()
            },
            Ink.sailCloth,
        )
        drawLine(
            Ink.mast,
            Offset(c, c),
            Offset(c + cos(t) * 61f, c + sin(t) * 61f),
            strokeWidth = 1.6f,
        )
    }
    circle(Ink.hub, c, c, 5f)
}

private fun DrawScope.cactusWall(w: Float, h: Float) {
    for ((cx, cy, rx, ry, turn) in
        listOf(
            Pad(112f, 26f, 9f, 13f, -0.2f),
            Pad(126f, 14f, 8f, 12f, 0.3f),
            Pad(104f, 10f, 7f, 10f, -0.5f),
            Pad(118f, 10f, 6f, 9f, 0.1f),
            Pad(132f, 30f, 7f, 10f, 0.5f),
        )) {
        rotateRad(turn, Offset(cx, cy)) {
            drawOval(Ink.cactus, Offset(cx - rx, cy - ry), Size(2 * rx, 2 * ry))
            drawOval(Ink.outline, Offset(cx - rx, cy - ry), Size(2 * rx, 2 * ry), style = outline)
        }
    }
    for ((fx, fy) in listOf(104f to 2f, 124f to 2f, 134f to 20f, 110f to 13f)) circle(
        Ink.cactusFlower,
        fx,
        fy + 2f,
        2.2f,
    )
    val spines = Seeded(4)
    repeat(30) {
        drawRect(
            Ink.spine,
            Offset(98f + spines.next() * 40f, 2f + spines.next() * 34f),
            Size(0.7f, 0.7f),
        )
    }
    val r = Seeded(7)
    inked(
        path {
            moveTo(0f, h)
            lineTo(0f, h - 20f)
            var i = 0f
            while (i <= 140f) {
                lineTo(i, h - 20f - r.next() * 6f)
                i += 14f
            }
            lineTo(140f, h)
            close()
        },
        Ink.drystone,
    )
    var y = h - 18f
    while (y < h - 2f) {
        var i = (y.toInt() % 2) * 6f
        while (i < 140f) {
            val stone = if (r.next() > 0.5f) Ink.stoneDark else Ink.stoneLight
            drawOval(stone, Offset(i, y), Size(12f, 6f))
            drawOval(Ink.outline, Offset(i, y), Size(12f, 6f), style = outline)
            i += 13f
        }
        y += 7f
    }
}

private data class Pad(val cx: Float, val cy: Float, val rx: Float, val ry: Float, val turn: Float)
