package dev.dimvlachos.lab.core.presentation.components.fog

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue

// A drop, drawn big on clear red glass: darker inside, with a bright highlight near its top.
@OptIn(ExperimentalTestApi::class)
class BeadUiTest {
    @Test
    fun aDropIsDarkerInsideWithABrightHighlight() = runComposeUiTest {
        setContent {
            FoggedWindow(
                photo = ColorPainter(Color.Red),
                state = FogState(startClear = true),
                modifier = Modifier.size(200.dp).testTag("window"),
                beads = { listOf(Bead(Offset(0.5f, 0.5f), 40.dp, resting = false)) },
            )
        }
        val pixels = onNodeWithTag("window").captureToImage().toPixelMap()
        val centre = Offset(pixels.width / 2f, pixels.height / 2f)
        val radius = pixels.width * 40f / 200f
        fun at(offset: Offset) =
            pixels[(centre.x + offset.x).toInt(), (centre.y + offset.y).toInt()]

        val inside = at(Offset(radius * 0.3f, radius * 0.2f))
        val outside = at(Offset(radius * 1.6f, 0f))
        val highlight = at(Offset(-radius * 0.3f, -radius * 0.35f))
        assertTrue(inside.red < outside.red - 0.05f, "inside ${inside.red}, outside ${outside.red}")
        assertTrue(highlight.green > 0.5f, "a white glint on red: ${highlight.green}")
    }

    @Test
    fun withoutDropsTheGlassIsUnchanged() = runComposeUiTest {
        setContent {
            FoggedWindow(
                photo = ColorPainter(Color.Red),
                state = FogState(startClear = true),
                modifier = Modifier.size(200.dp).testTag("window"),
            )
        }
        val pixels = onNodeWithTag("window").captureToImage().toPixelMap()
        val middle = pixels[pixels.width / 2, pixels.height / 2]
        assertTrue(middle.red > 0.9f && middle.green < 0.1f, "$middle")
    }

    @Test
    fun aRunningDropIsPulledIntoATeardrop() = runComposeUiTest {
        setContent {
            FoggedWindow(
                photo = ColorPainter(Color.Red),
                state = FogState(startClear = true),
                modifier = Modifier.size(200.dp).testTag("window"),
                beads = { listOf(Bead(Offset(0.5f, 0.6f), 30.dp, resting = false, stretch = 1f)) },
            )
        }
        val pixels = onNodeWithTag("window").captureToImage().toPixelMap()
        val centre = Offset(pixels.width / 2f, pixels.height * 0.6f)
        val radius = pixels.width * 30f / 200f
        fun at(offset: Offset) =
            pixels[(centre.x + offset.x).toInt(), (centre.y + offset.y).toInt()]

        val clear = at(Offset(radius * 2.5f, 0f))
        val tail = at(Offset(0f, -radius * 1.8f))
        val side = at(Offset(radius * 1.05f, radius * 0.1f))
        assertTrue(tail.red < clear.red - 0.05f, "a tail up above the bead: ${tail.red}")
        assertTrue(side.red > clear.red - 0.02f, "narrower than a round bead: ${side.red}")
    }

    @Test
    fun aDropFadedRightOutLeavesTheGlassUntouched() = runComposeUiTest {
        setContent {
            FoggedWindow(
                photo = ColorPainter(Color.Red),
                state = FogState(startClear = true),
                modifier = Modifier.size(200.dp).testTag("window"),
                beads = { listOf(Bead(Offset(0.5f, 0.5f), 40.dp, resting = true, alpha = 0f)) },
            )
        }
        val pixels = onNodeWithTag("window").captureToImage().toPixelMap()
        val middle = pixels[pixels.width / 2, pixels.height / 2]
        assertTrue(middle.red > 0.9f && middle.green < 0.1f, "$middle")
    }

    // The red channel just inside a big drop on clear red glass: darker the more the drop stands
    // out.
    private fun insideRed(softness: Float): Float {
        var red = 0f
        runComposeUiTest {
            setContent {
                FoggedWindow(
                    photo = ColorPainter(Color.Red),
                    state = FogState(startClear = true),
                    modifier = Modifier.size(200.dp).testTag("window"),
                    beads = {
                        listOf(Bead(Offset(0.5f, 0.5f), 40.dp, resting = true, softness = softness))
                    },
                )
            }
            val pixels = onNodeWithTag("window").captureToImage().toPixelMap()
            // Low in the drop, on its rim: where a crisp drop is darkest.
            red =
                pixels[pixels.width / 2, (pixels.height / 2 + pixels.width * 36f / 200f).toInt()]
                    .red
        }
        return red
    }

    @Test
    fun aSettledDropStandsOutLessThanAFreshOne() {
        val crisp = insideRed(0f)
        val settled = insideRed(1f)
        assertTrue(settled > crisp + 0.05f, "a softer rim: $settled against $crisp")
    }
}
