package dev.dimvlachos.lab.core.presentation.canvas2d

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class Canvas2DTest {
    private fun near(expected: Color, actual: Color, tolerance: Float = 0.03f) {
        val ok =
            abs(expected.red - actual.red) < tolerance &&
                abs(expected.green - actual.green) < tolerance &&
                abs(expected.blue - actual.blue) < tolerance &&
                abs(expected.alpha - actual.alpha) < tolerance
        assertTrue(ok, "expected $expected, got $actual")
    }

    @Test
    fun mulberryMatchesThePrototypesGenerator() {
        val seven = mulberry(7)
        assertEquals(0.011704753153026, seven(), 1e-12)
        assertEquals(0.061958257574588, seven(), 1e-12)
        assertEquals(0.976907632779330, seven(), 1e-12)
        assertEquals(0.699028705712408, seven(), 1e-12)
        val seventeen = mulberry(17)
        assertEquals(0.677150296047330, seventeen(), 1e-12)
        assertEquals(0.192656921222806, seventeen(), 1e-12)
    }

    @Test
    fun cssColoursParseAsTheBrowserReadsThem() {
        assertEquals(Color(red = 0xef, green = 0xe1, blue = 0xc8), cssColor("#efe1c8"))
        assertEquals(Color(red = 0xff, green = 0xff, blue = 0xff), cssColor("#fff"))
        assertEquals(
            Color(red = 20, green = 24, blue = 58, alpha = (0.34f * 255).toInt()),
            cssColor("rgba(20,24,58,.34)"),
        )
        assertEquals(Color(red = 255, green = 238, blue = 210), cssColor("rgb(255, 238, 210)"))
    }

    @Test
    fun coloursParseSafelyFromTwoThreadsAtOnce() = runBlocking {
        // The scenes paint on a background thread while the room draws on the main one.
        val jobs =
            List(4) { worker ->
                launch(Dispatchers.Default) {
                    repeat(4_000) { i ->
                        val c = cssColor("rgba(${worker},${i % 256},${i / 256},.5)")
                        assertEquals(worker / 255f, c.red, 0.01f)
                    }
                }
            }
        jobs.joinAll()
    }

    @Test
    fun aShadedImageDarkensItsPaperAndLeavesItsHolesClear() {
        // White paper on the left half, a hole on the right.
        val paper = Canvas2D.create(4, 2)
        paper.fillStyle = "#ffffff"
        paper.fillRect(0f, 0f, 2f, 2f)
        val c = Canvas2D.create(4, 2)
        c.fillStyle = "#0000ff"
        c.fillRect(0f, 0f, 4f, 2f)
        c.imageShade = Color(red = 0, green = 0, blue = 0, alpha = 128)
        c.drawImage(paper.bitmap, 0f, 0f)
        val px = c.bitmap.toPixelMap()
        near(Color(.5f, .5f, .5f), px[0, 0])
        near(Color.Blue, px[3, 0])
    }

    @Test
    fun aLayerKeepsSourceAtopToWhatWasPaintedInIt() {
        val paper = Canvas2D.create(4, 2)
        paper.fillStyle = "#ffffff"
        paper.fillRect(0f, 0f, 2f, 2f)
        val c = Canvas2D.create(4, 2)
        c.fillStyle = "#0000ff"
        c.fillRect(0f, 0f, 4f, 2f)
        c.beginLayer()
        c.drawImage(paper.bitmap, 0f, 0f)
        c.globalCompositeOperation = "source-atop"
        c.fillStyle = "#ff0000"
        c.fillRect(0f, 0f, 4f, 2f)
        c.endLayer()
        val px = c.bitmap.toPixelMap()
        near(Color.Red, px[0, 0])
        near(Color.Blue, px[3, 0])
    }

    @Test
    fun aFilledRectLandsUnderTheTransform() {
        val c = Canvas2D.create(8, 8)
        c.setTransform(4f, 0f, 0f, 4f, 4f, 4f) // [-1, 1] across the bitmap, as the prototype paints
        c.fillStyle = "#ff0000"
        c.fillRect(0f, 0f, 1f, 1f) // the bottom-right quarter
        val px = c.bitmap.toPixelMap()
        near(Color.Red, px[6, 6])
        near(Color.Transparent, px[1, 1])
    }

    @Test
    fun aShadowIsOffsetAcrossAndDown() {
        val c = Canvas2D.create(40, 40)
        c.shadowColor = "rgba(0,0,0,1)"
        c.shadowBlur = 0.5f
        c.shadowOffsetX = 12f
        c.shadowOffsetY = 12f
        c.fillStyle = "#ff0000"
        c.fillRect(4f, 4f, 10f, 10f)
        val px = c.bitmap.toPixelMap()
        near(Color.Red, px[8, 8])
        assertTrue(px[22, 22].alpha > .8f && px[22, 22].red < .2f, "shadow at ${px[22, 22]}")
        near(Color.Transparent, px[22, 8])
    }

    @Test
    fun aRoundJoinRoundsTheOuterCorner() {
        fun corner(join: String): Color {
            val c = Canvas2D.create(40, 40)
            c.lineJoin = join
            c.lineWidth = 12f
            c.strokeStyle = "#0000ff"
            c.beginPath()
            c.moveTo(30f, 10f)
            c.lineTo(10f, 10f)
            c.lineTo(10f, 30f)
            c.stroke()
            return c.bitmap.toPixelMap()[5, 5]
        }
        near(Color.Blue, corner("miter"))
        val round = corner("round")
        assertTrue(round.alpha < .35f, "round corner at $round")
    }

    @Test
    fun anArcFillsACircle() {
        val c = Canvas2D.create(20, 20)
        c.beginPath()
        c.arc(10f, 10f, 8f, 0f, TAU)
        c.fillStyle = "rgba(0,0,255,1)"
        c.fill()
        val px = c.bitmap.toPixelMap()
        near(Color.Blue, px[10, 10])
        near(Color.Transparent, px[1, 1])
    }

    @Test
    fun anArcAfterLinesContinuesTheSameShape() {
        // A box's top edge, then the bottom half of a circle: one D shape, as the browser fills it.
        val c = Canvas2D.create(20, 20)
        c.beginPath()
        c.moveTo(2f, 0f)
        c.lineTo(18f, 0f)
        c.arc(10f, 10f, 8f, 0f, PI.toFloat())
        c.closePath()
        c.fillStyle = "#0000ff"
        c.fill()
        val px = c.bitmap.toPixelMap()
        // Below the diagonal a separate arc contour would leave the box's lower left empty.
        near(Color.Blue, px[5, 8])
        near(Color.Blue, px[10, 15])
    }

    @Test
    fun aClipHoldsUntilRestore() {
        val c = Canvas2D.create(10, 10)
        c.save()
        c.beginPath()
        c.rect(0f, 0f, 5f, 10f)
        c.clip()
        c.fillStyle = "#00ff00"
        c.fillRect(0f, 0f, 10f, 10f)
        c.restore()
        c.fillStyle = "#0000ff"
        c.fillRect(8f, 0f, 2f, 10f)
        val px = c.bitmap.toPixelMap()
        near(Color.Green, px[2, 5])
        near(Color.Blue, px[9, 5])
        near(Color.Transparent, px[6, 5])
    }

    @Test
    fun aLinearGradientRunsBetweenItsPoints() {
        val c = Canvas2D.create(10, 2)
        val g = c.createLinearGradient(0f, 0f, 10f, 0f)
        g.addColorStop(0f, "#000000")
        g.addColorStop(1f, "#ffffff")
        c.fillStyle = g
        c.fillRect(0f, 0f, 10f, 2f)
        val px = c.bitmap.toPixelMap()
        assertTrue(px[1, 1].red < 0.2f && px[8, 1].red > 0.8f)
    }

    @Test
    fun aRadialGradientStartsFromItsInnerCircle() {
        // A highlight at the upper left inside a disc centred lower down, as the dome's is.
        val c = Canvas2D.create(40, 40)
        val g = c.createRadialGradient(12f, 12f, 1f, 20f, 20f, 18f)
        g.addColorStop(0f, "#ffffff")
        g.addColorStop(1f, "#000000")
        c.fillStyle = g
        c.fillRect(0f, 0f, 40f, 40f)
        val px = c.bitmap.toPixelMap()
        assertTrue(px[12, 12].red > .9f, "the highlight sits at the inner circle: ${px[12, 12]}")
        assertTrue(px[12, 12].red > px[20, 20].red + .1f, "brighter than the outer centre")
    }

    @Test
    fun globalAlphaAndLighterBehaveAsInTheBrowser() {
        val c = Canvas2D.create(4, 4)
        c.fillStyle = "#400000"
        c.fillRect(0f, 0f, 4f, 4f)
        c.globalCompositeOperation = "lighter"
        c.globalAlpha = 0.5f
        c.fillStyle = "#800000"
        c.fillRect(0f, 0f, 4f, 4f)
        val red = c.bitmap.toPixelMap()[2, 2].red
        // 0x40 + 0x80 * 0.5 = 0x80.
        assertEquals(0x80 / 255f, red, 0.03f)
    }

    @Test
    fun destinationOutErases() {
        val c = Canvas2D.create(4, 4)
        c.fillStyle = "#ffffff"
        c.fillRect(0f, 0f, 4f, 4f)
        c.globalCompositeOperation = "destination-out"
        c.fillRect(0f, 0f, 2f, 4f)
        val px = c.bitmap.toPixelMap()
        near(Color.Transparent, px[1, 2])
        near(Color.White, px[3, 2])
    }

    @Test
    fun aRoundRectLeavesItsCornersOpen() {
        val c = Canvas2D.create(20, 20)
        c.beginPath()
        c.roundRect(0f, 0f, 20f, 20f, 6f)
        c.fillStyle = "#ff0000"
        c.fill()
        val px = c.bitmap.toPixelMap()
        near(Color.Red, px[10, 10])
        near(Color.Red, px[10, 0])
        near(Color.Transparent, px[0, 0])
        near(Color.Transparent, px[19, 19])
    }

    @Test
    fun aRoundRectTakesItsCornersOneByOne() {
        // Rounded at the top only, as a divider's tab is.
        val c = Canvas2D.create(20, 20)
        c.beginPath()
        c.roundRect(0f, 0f, 20f, 20f, floatArrayOf(8f, 8f, 0f, 0f))
        c.fillStyle = "#ff0000"
        c.fill()
        val px = c.bitmap.toPixelMap()
        near(Color.Transparent, px[0, 0])
        near(Color.Transparent, px[19, 0])
        near(Color.Red, px[0, 19])
        near(Color.Red, px[19, 19])
    }

    private fun tile(): ImageBitmap {
        val t = Canvas2D.create(2, 1)
        t.fillStyle = "#ff0000"
        t.fillRect(0f, 0f, 1f, 1f)
        t.fillStyle = "#0000ff"
        t.fillRect(1f, 0f, 1f, 1f)
        return t.bitmap
    }

    @Test
    fun aPatternRepeatsItsImage() {
        val c = Canvas2D.create(8, 2)
        c.fillStyle = c.createPattern(tile())
        c.fillRect(0f, 0f, 8f, 2f)
        val px = c.bitmap.toPixelMap()
        near(Color.Red, px[4, 1])
        near(Color.Blue, px[5, 1])
        near(Color.Blue, px[7, 0])
    }

    @Test
    fun aPatternScalesWithTheTransform() {
        val c = Canvas2D.create(8, 2)
        c.scale(2f, 2f)
        c.fillStyle = c.createPattern(tile())
        c.fillRect(0f, 0f, 4f, 1f)
        val px = c.bitmap.toPixelMap()
        near(Color.Red, px[1, 1])
        near(Color.Blue, px[2, 1])
        near(Color.Red, px[4, 0])
    }

    @OptIn(ExperimentalTestApi::class)
    private fun withMeasurer(block: (TextMeasurer) -> Unit) = runComposeUiTest {
        var measurer: TextMeasurer? = null
        setContent { measurer = rememberTextMeasurer() }
        waitForIdle()
        block(checkNotNull(measurer))
    }

    private fun textCanvas(measurer: TextMeasurer, w: Int, h: Int) =
        Canvas2D.create(w, h).apply {
            textMeasurer = measurer
            fonts = mapOf("MONO" to FontFamily.Monospace)
        }

    private fun inkRows(c: Canvas2D): List<Int> {
        val px = c.bitmap.toPixelMap()
        return (0 until c.height).filter { y ->
            (0 until c.width).any { x -> px[x, y].alpha > .3f }
        }
    }

    private fun inkColumns(c: Canvas2D): List<Int> {
        val px = c.bitmap.toPixelMap()
        return (0 until c.width).filter { x ->
            (0 until c.height).any { y -> px[x, y].alpha > .3f }
        }
    }

    @Test
    fun wideTextMeasuresWider() = withMeasurer { m ->
        val c = textCanvas(m, 10, 10)
        c.font = "700 20px MONO"
        val one = c.measureText("W")
        assertTrue(one > 6f, "a 20px W is more than 6px wide: $one")
        assertEquals(one * 3, c.measureText("WWW"), one * .2f)
        c.font = "700 40px MONO"
        assertEquals(one * 2, c.measureText("W"), one * .2f)
    }

    @Test
    fun textSitsOnItsBaseline() = withMeasurer { m ->
        val c = textCanvas(m, 80, 50)
        c.font = "700 20px MONO"
        c.fillStyle = "#000000"
        c.fillText("HE", 4f, 30f)
        val rows = inkRows(c)
        assertTrue(rows.isNotEmpty(), "the text is drawn")
        assertTrue(rows.max() <= 31, "nothing below the baseline: ${rows.max()}")
        assertTrue(rows.min() in 12..22, "capitals rise about 14px: ${rows.min()}")
    }

    @Test
    fun textAlignedRightEndsAtItsX() = withMeasurer { m ->
        val c = textCanvas(m, 100, 50)
        c.font = "700 20px MONO"
        c.fillStyle = "#000000"
        c.textAlign = "right"
        c.fillText("HH", 80f, 30f)
        val cols = inkColumns(c)
        assertTrue(cols.max() <= 81, "ends at x: ${cols.max()}")
        assertTrue(cols.min() >= 50, "starts a word's width before it: ${cols.min()}")
    }

    @Test
    fun textCentredOnMiddleStraddlesItsPoint() = withMeasurer { m ->
        val c = textCanvas(m, 100, 60)
        c.font = "700 20px MONO"
        c.fillStyle = "#000000"
        c.textAlign = "center"
        c.textBaseline = "middle"
        c.fillText("H", 50f, 30f)
        val rows = inkRows(c)
        val cols = inkColumns(c)
        assertTrue(rows.min() < 30 && rows.max() > 30, "rows straddle 30: $rows")
        assertTrue(cols.min() < 50 && cols.max() > 50, "columns straddle 50: $cols")
    }

    @Test
    fun textTurnsWithTheTransform() = withMeasurer { m ->
        val c = textCanvas(m, 80, 80)
        c.font = "700 20px MONO"
        c.fillStyle = "#000000"
        c.translate(40f, 40f)
        c.scale(2f, 2f)
        c.fillText("H", 0f, 0f)
        val rows = inkRows(c)
        // Doubled, the capital rises about 28px above the baseline at y 40.
        assertTrue(rows.min() < 20 && rows.max() <= 41, "scaled: $rows")
    }

    @Test
    fun textLaidOutAtAHigherResolutionKeepsItsUserUnits() = withMeasurer { m ->
        val c = textCanvas(m, 200, 120)
        c.font = "700 10px MONO"
        val plain = c.measureText("HH HH")
        c.textResolution = 4f
        assertEquals(plain, c.measureText("HH HH"), plain * .1f)
        // Drawn under the scale it was laid out for, it lands where the plain layout would.
        c.fillStyle = "#000000"
        c.textAlign = "center"
        c.textBaseline = "middle"
        c.scale(4f, 4f)
        c.fillText("H", 25f, 15f)
        val rows = inkRows(c)
        val cols = inkColumns(c)
        assertTrue(rows.min() < 60 && rows.max() > 60, "rows straddle 60: $rows")
        assertTrue(cols.min() < 100 && cols.max() > 100, "columns straddle 100: $cols")
        c.textResolution = 0f
        assertEquals(1f, c.textResolution)
    }

    @Test
    fun anImageDrawsFromAFractionalSourceRectangle() {
        // Four rows, red green blue white; the source starts half way down the green row.
        val src = Canvas2D.create(1, 4)
        listOf("#ff0000", "#00ff00", "#0000ff", "#ffffff").forEachIndexed { i, c ->
            src.fillStyle = c
            src.fillRect(0f, i.toFloat(), 1f, 1f)
        }
        val c = Canvas2D.create(4, 40)
        c.drawImage(src.bitmap, 0f, 1.5f, 1f, 2f, 0f, 0f, 4f, 40f)
        val px = c.bitmap.toPixelMap()
        // Rows 1.5 to 3.5 stretched over 40: green's lower half, all of blue, white's upper half.
        assertTrue(px[2, 3].green > .6f, "green at the top: ${px[2, 3]}")
        near(Color.Blue, px[2, 20], .08f)
        assertTrue(px[2, 37].red > .6f && px[2, 37].blue > .6f, "white at the foot: ${px[2, 37]}")
    }

    @Test
    fun arcToRoundsACornerAsTheBrowserDoes() {
        // A square with its top-right corner rounded by 8: the corner pixel is left out, its
        // neighbour along the edge and the square's middle are filled.
        val c = Canvas2D.create(20, 20)
        c.beginPath()
        c.moveTo(0f, 0f)
        c.arcTo(20f, 0f, 20f, 20f, 8f)
        c.lineTo(20f, 20f)
        c.lineTo(0f, 20f)
        c.closePath()
        c.fillStyle = "#0000ff"
        c.fill()
        val px = c.bitmap.toPixelMap()
        near(Color.Transparent, px[19, 0])
        near(Color.Blue, px[5, 1])
        near(Color.Blue, px[18, 15])
        near(Color.Blue, px[10, 10])
    }

    @Test
    fun transformAppliesAfterTheTransformAlreadySet() {
        val c = Canvas2D.create(20, 20)
        c.translate(10f, 0f)
        c.transform(2f, 0f, 0f, 2f, 0f, 4f) // scale by two, then down 4, in the translated frame
        c.fillStyle = "#ff0000"
        c.fillRect(0f, 0f, 2f, 2f) // lands at (10, 4) to (14, 8)
        val px = c.bitmap.toPixelMap()
        near(Color.Red, px[12, 6])
        near(Color.Transparent, px[2, 6])
        near(Color.Transparent, px[12, 1])
    }

    @Test
    fun aDashedStrokeLeavesGaps() {
        val c = Canvas2D.create(40, 4)
        c.setLineDash(floatArrayOf(4f, 4f))
        c.lineWidth = 2f
        c.strokeStyle = "#ff0000"
        c.beginPath()
        c.moveTo(0f, 2f)
        c.lineTo(40f, 2f)
        c.stroke()
        val px = c.bitmap.toPixelMap()
        near(Color.Red, px[1, 2])
        near(Color.Transparent, px[6, 2])
        near(Color.Red, px[9, 2])
    }

    @Test
    fun aShadowFallsByBothOffsets() {
        val c = Canvas2D.create(30, 30)
        c.shadowColor = "rgba(0,0,255,1)"
        c.shadowBlur = 1f
        c.shadowOffsetX = 12f
        c.shadowOffsetY = 12f
        c.fillStyle = "#ff0000"
        c.fillRect(2f, 2f, 8f, 8f)
        val px = c.bitmap.toPixelMap()
        near(Color.Red, px[6, 6])
        assertTrue(px[18, 18].blue > .6f, "the shadow, down and right: ${px[18, 18]}")
        near(Color.Transparent, px[6, 18])
    }

    @Test
    fun anOddDashListIsDoubledAsTheBrowserDoes() {
        // [4] dashes as [4, 4]; Skia would refuse an odd list outright.
        val c = Canvas2D.create(40, 4)
        c.setLineDash(floatArrayOf(4f))
        c.lineWidth = 2f
        c.strokeStyle = "#ff0000"
        c.beginPath()
        c.moveTo(0f, 2f)
        c.lineTo(40f, 2f)
        c.stroke()
        val px = c.bitmap.toPixelMap()
        near(Color.Red, px[1, 2])
        near(Color.Transparent, px[6, 2])
        near(Color.Red, px[9, 2])
    }

    @Test
    fun arcToAfterAnArcGoesOnFromTheArcsEnd() {
        // A quarter circle from (0, 0) to (20, 20), then down the side and round towards (0, 40):
        // one shape, so the fill reaches the lower right. Begun anew at (20, 40), it wouldn't.
        val c = Canvas2D.create(24, 44)
        c.beginPath()
        c.arc(0f, 20f, 20f, (-PI / 2).toFloat(), 0f)
        c.arcTo(20f, 40f, 0f, 40f, 1f)
        c.closePath()
        c.fillStyle = "#0000ff"
        c.fill()
        val px = c.bitmap.toPixelMap()
        near(Color.Blue, px[16, 30])
        near(Color.Transparent, px[4, 36])
    }

    @Test
    fun strokeShadowDrawsTheShadowAloneInAColourSetDirectly() {
        val c = Canvas2D.create(40, 20)
        c.setShadowColor(Color(0f, 0f, 1f, 1f))
        c.shadowBlur = 1f
        c.shadowOffsetX = 20f
        c.lineWidth = 6f
        c.beginPath()
        c.moveTo(4f, 10f)
        c.lineTo(14f, 10f)
        c.strokeShadow()
        val px = c.bitmap.toPixelMap()
        near(Color.Transparent, px[9, 10])
        assertTrue(px[29, 10].blue > .6f, "the shadow, moved right: ${px[29, 10]}")
    }

    @Test
    fun destinationInKeepsOnlyWhatItCovers() {
        val c = Canvas2D.create(4, 4)
        c.fillStyle = "#ff0000"
        c.fillRect(0f, 0f, 4f, 4f)
        c.globalCompositeOperation = "destination-in"
        c.fillStyle = "rgba(0,0,0,0.5)"
        c.fillRect(0f, 0f, 2f, 4f)
        val px = c.bitmap.toPixelMap()
        near(Color.Red.copy(alpha = .5f), px[1, 2])
        near(Color.Red, px[3, 2])
    }

    @Test
    fun destinationOverPaintsBehind() {
        val c = Canvas2D.create(4, 4)
        c.fillStyle = "#ff0000"
        c.fillRect(0f, 0f, 2f, 4f)
        c.globalCompositeOperation = "destination-over"
        c.fillStyle = "#0000ff"
        c.fillRect(0f, 0f, 4f, 4f)
        val px = c.bitmap.toPixelMap()
        near(Color.Red, px[1, 2])
        near(Color.Blue, px[3, 2])
    }

    @Test
    fun aLayerIsLaidDownAtItsAlpha() {
        val c = Canvas2D.create(4, 4)
        c.beginLayer(.5f)
        c.fillStyle = "#ff0000"
        c.fillRect(0f, 0f, 4f, 2f)
        c.fillRect(0f, 0f, 4f, 4f)
        c.endLayer()
        val px = c.bitmap.toPixelMap()
        // Two fills overlap in the layer, which is faded as one: still half, not three quarters.
        near(Color.Red.copy(alpha = .5f), px[1, 1])
        near(Color.Red.copy(alpha = .5f), px[1, 3])
    }

    @Test
    fun aShadowTakesItsColourAsAColor() {
        val c = Canvas2D.create(40, 40)
        c.setShadowColor(Color(0f, 1f, 0f, 1f))
        c.shadowBlur = 0.5f
        c.shadowOffsetY = 14f
        c.fillStyle = "#ff0000"
        c.fillRect(4f, 4f, 10f, 10f)
        val px = c.bitmap.toPixelMap()
        near(Color.Red, px[8, 8])
        assertTrue(px[8, 24].green > .8f && px[8, 24].red < .2f, "shadow at ${px[8, 24]}")
    }

    @Test
    fun theTransformScaleFollowsScaleAndRotation() {
        val c = Canvas2D.create(4, 4)
        assertEquals(1f, c.transformScale, 1e-5f)
        c.scale(2.5f, 2.5f)
        c.save()
        c.rotate(.7f)
        c.scale(.4f, .4f)
        assertEquals(1f, c.transformScale, 1e-5f)
        c.restore()
        assertEquals(2.5f, c.transformScale, 1e-5f)
        c.setTransform(0f, 3f, -3f, 0f, 5f, 5f)
        assertEquals(3f, c.transformScale, 1e-5f)
    }

    @Test
    fun drawUnderPaintsUnderTheTransformAndClipAndLeavesThemAsTheyWere() {
        val c = Canvas2D.create(20, 20)
        c.save()
        c.beginPath()
        c.rect(0f, 0f, 20f, 10f)
        c.clip()
        c.translate(10f, 5f)
        c.drawUnder { canvas ->
            // A square at (0, 0) under the transform: (10, 5) on the canvas, cut at y = 10.
            canvas.translate(-2f, -2f)
            canvas.drawRect(
                androidx.compose.ui.geometry.Rect(0f, 0f, 4f, 10f),
                androidx.compose.ui.graphics.Paint().apply { color = Color.Red },
            )
        }
        // The translate inside the block is gone: a fill at (0, 0) lands at (10, 5) again.
        c.fillStyle = "#0000ff"
        c.fillRect(-1f, 3f, 2f, 1f)
        c.restore()
        val px = c.bitmap.toPixelMap()
        near(Color.Red, px[9, 6])
        near(Color.Transparent, px[9, 12])
        near(Color.Transparent, px[5, 6])
        near(Color.Blue, px[10, 8])
    }

    @Test
    fun aGradientKeepsItsShaderUntilAStopIsAdded() {
        val c = Canvas2D.create(10, 2)
        val g = c.createLinearGradient(0f, 0f, 10f, 0f)
        g.addColorStop(0f, "#000000")
        g.addColorStop(1f, "#ffffff")
        val first = g.shader()
        assertTrue(first === g.shader(), "filled again, the same shader")
        g.addColorStop(1f, "#ff0000")
        assertTrue(first !== g.shader(), "a new stop, a new shader")
        c.fillStyle = g
        c.fillRect(0f, 0f, 10f, 2f)
        assertTrue(c.bitmap.toPixelMap()[9, 1].red > .8f)
    }
}
