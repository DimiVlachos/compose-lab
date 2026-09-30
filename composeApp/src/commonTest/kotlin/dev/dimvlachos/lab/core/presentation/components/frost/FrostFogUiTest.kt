package dev.dimvlachos.lab.core.presentation.components.frost

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

@OptIn(ExperimentalTestApi::class)
class FrostFogUiTest {
    private fun FrostState.wipeAcross(y: Float) {
        val stroke = beginStroke(Offset(0.1f, y))
        extendStroke(stroke, Offset(0.9f, y))
    }

    @Test
    fun fogCoversTheWipesBelowItsFrontAndLaterWipesClearIt() = runComposeUiTest {
        val frost = FrostState()
        frost.wipeAcross(0.2f)
        frost.wipeAcross(0.5f)
        // Solid fog up to 0.38 of the way down; its soft edge and puffs stop short of 0.2.
        frost.setBreathLevel(frost.beginBreath(), 0.62f)
        frost.wipeAcross(0.7f)
        setContent {
            FrostedWindow(
                photo = ColorPainter(Color.Red),
                state = frost,
                modifier = Modifier.size(200.dp).testTag("window"),
            )
        }
        val pixels = onNodeWithTag("window").captureToImage().toPixelMap()
        fun greenAt(y: Float) = pixels[pixels.width / 2, (y * (pixels.height - 1)).toInt()].green

        assertTrue(greenAt(0.2f) < 0.12f, "the wipe above the fog is still clear: ${greenAt(0.2f)}")
        assertTrue(greenAt(0.5f) > 0.2f, "the wipe under the fog is covered: ${greenAt(0.5f)}")
        assertTrue(greenAt(0.7f) < 0.12f, "the wipe after the breath clears it: ${greenAt(0.7f)}")
    }

    @Test
    fun clearGlassFogsFromTheBottomUp() = runComposeUiTest {
        val frost = FrostState(startClear = true)
        frost.setBreathLevel(frost.beginBreath(), 0.5f)
        setContent {
            FrostedWindow(
                photo = ColorPainter(Color.Red),
                state = frost,
                modifier = Modifier.size(200.dp).testTag("window"),
            )
        }
        val pixels = onNodeWithTag("window").captureToImage().toPixelMap()
        fun greenAt(y: Float) = pixels[pixels.width / 2, (y * (pixels.height - 1)).toInt()].green

        assertTrue(greenAt(0.1f) < 0.12f, "the top is still clear: ${greenAt(0.1f)}")
        assertTrue(greenAt(0.8f) > 0.2f, "the bottom is fogged: ${greenAt(0.8f)}")
    }
}
