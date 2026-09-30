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
}
