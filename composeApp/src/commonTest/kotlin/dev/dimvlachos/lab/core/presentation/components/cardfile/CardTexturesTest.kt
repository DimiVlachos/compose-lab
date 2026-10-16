package dev.dimvlachos.lab.core.presentation.components.cardfile

import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import kotlin.math.ceil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CardTexturesTest {
    private val abby =
        PaperCard.Contact(
            "Abby Carter",
            "Carter Hardware",
            listOf("(802) 555-0142", "14 Elm St, Burlington", "Spare key under the planter"),
            seed = 31,
        )

    // At S = 100 and a dpr of 1 a card is 175 by 117 px: the tab strip is 17 high, the body 100.
    private val s = 100f

    @OptIn(ExperimentalTestApi::class)
    private fun withInk(block: (CardInk) -> Unit) = runComposeUiTest {
        var measurer: TextMeasurer? = null
        setContent { measurer = rememberTextMeasurer() }
        waitForIdle()
        val m = checkNotNull(measurer)
        block(
            CardInk(
                m,
                mapOf(
                    "TYPED" to FontFamily.Monospace,
                    "HAND" to FontFamily.Cursive,
                    "SANS" to FontFamily.SansSerif,
                ),
                makeGrain(),
            )
        )
    }

    private fun PixelMap.mean(xs: IntRange, ys: IntRange, channel: (Int, Int) -> Float): Float {
        var sum = 0f
        var n = 0
        for (y in ys) for (x in xs) {
            sum += channel(x, y)
            n++
        }
        return sum / n
    }

    @Test
    fun aFaceIsTheCardsSizeAtItsDensity() = withInk { ink ->
        val face = bakeFace(abby, s, 1.5f, ink)
        assertEquals(ceil(175 * 1.5).toInt(), face.width)
        assertEquals(ceil(117 * 1.5).toInt(), face.height)
    }

    @Test
    fun aContactsCardHasARedMarginLineAndBlueRules() = withInk { ink ->
        val px = bakeFace(abby, s, 1f, ink).bitmap.toPixelMap()
        val xs = 120..165
        // The red line runs at the top of the body plus a quarter: y 42.
        val red = px.mean(xs, 42..42) { x, y -> px[x, y].red - px[x, y].green }
        val paper = px.mean(xs, 60..62) { x, y -> px[x, y].red - px[x, y].green }
        assertTrue(red > paper + .15f, "red margin line: $red against paper $paper")
        // The first blue rule lies .126 below it, at y 54.6.
        val rule = px.mean(xs, 54..55) { x, y -> px[x, y].blue - px[x, y].red }
        val between = px.mean(xs, 60..62) { x, y -> px[x, y].blue - px[x, y].red }
        assertTrue(rule > between + .04f, "blue rule: $rule against paper $between")
    }

    @Test
    fun theRailNotchesAreCutThrough() = withInk { ink ->
        val px = bakeFace(abby, s, 1f, ink).bitmap.toPixelMap()
        // The round notch at a third of the width, .085 above the bottom, and the slit under it.
        assertTrue(px[59, 108].alpha < .05f, "notch: ${px[59, 108]}")
        assertTrue(px[59, 115].alpha < .05f, "slit: ${px[59, 115]}")
        assertTrue(px[45, 108].alpha > .95f, "paper beside the notch: ${px[45, 108]}")
    }

    @Test
    fun onlyADividerRaisesATab() = withInk { ink ->
        val divider = bakeFace(PaperCard.Divider('A', 0), s, 1f, ink).bitmap.toPixelMap()
        val contact = bakeFace(abby, s, 1f, ink).bitmap.toPixelMap()
        // The first divider's tab stands over x 8 to 38, above the body's top at 17.
        assertTrue(divider[20, 6].alpha > .95f, "the tab: ${divider[20, 6]}")
        assertTrue(divider[100, 6].alpha < .05f, "beside the tab: ${divider[100, 6]}")
        assertTrue(contact[20, 6].alpha < .05f, "a contact has no tab: ${contact[20, 6]}")
        // The next divider's tab moves along: tabX(1) = .398.
        val next = bakeFace(PaperCard.Divider('B', 1), s, 1f, ink).bitmap.toPixelMap()
        assertTrue(next[20, 6].alpha < .05f && next[55, 6].alpha > .95f, "the second tab")
    }

    @Test
    fun aCardsBackIsDarkerThanItsFace() = withInk { ink ->
        val face = bakeFace(abby, s, 1f, ink).bitmap.toPixelMap()
        val back = bakeBack(abby, s, 1f, ink).bitmap.toPixelMap()
        val xs = 120..165
        val ys = 96..102
        val f = face.mean(xs, ys) { x, y -> face[x, y].let { it.red + it.green + it.blue } }
        val b = back.mean(xs, ys) { x, y -> back[x, y].let { it.red + it.green + it.blue } }
        assertTrue(b < f - .05f, "back $b against face $f")
    }
}
