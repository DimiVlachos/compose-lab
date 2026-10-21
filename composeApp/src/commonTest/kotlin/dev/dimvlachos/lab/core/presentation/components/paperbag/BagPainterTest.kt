package dev.dimvlachos.lab.core.presentation.components.paperbag

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class BagPainterTest {
    @Test
    fun aBagGrowingAsItTipsBakesItsNumberAFewTimesNotEveryFrame() = runComposeUiTest {
        var ink: BagInk? = null
        setContent { ink = BagInk(rememberTextMeasurer(), FontFamily.Default) }
        runOnIdle {
            val inked = ink!!
            val g = Canvas2D.create(480, 480)
            for (i in 0..120) {
                val e = i / 120f
                val pose = BagPose(tip = e, count = 4, prevCount = 3, numP = .2f)
                drawBag(
                    g,
                    240f,
                    460f,
                    34f + (140f - 34f) * e,
                    pose,
                    inked,
                    dark = false,
                    pixels = 3f,
                )
            }
            // Two numbers, at rest in the corner and in the sheet and on the ladder between.
            assertTrue(inked.numbers.size <= 24, "${inked.numbers.size} numbers baked")
        }
    }
}
