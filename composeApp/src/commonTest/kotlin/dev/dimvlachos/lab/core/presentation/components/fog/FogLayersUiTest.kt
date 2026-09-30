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

@OptIn(ExperimentalTestApi::class)
class FogLayersUiTest {
    private fun FogState.wipeAcross(y: Float) {
        val stroke = beginStroke(Offset(0.1f, y))
        extendStroke(stroke, Offset(0.9f, y))
    }

    @Test
    fun fogCoversTheWipesBelowItsFrontAndLaterWipesClearIt() = runComposeUiTest {
        val fog = FogState()
        fog.wipeAcross(0.2f)
        fog.wipeAcross(0.5f)
        // Solid fog up to 0.38 of the way down; its soft edge and puffs stop short of 0.2.
        fog.setBreathLevel(fog.beginBreath(), 0.62f)
        fog.wipeAcross(0.7f)
        setContent {
            FoggedWindow(
                photo = ColorPainter(Color.Red),
                state = fog,
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
        val fog = FogState(startClear = true)
        fog.setBreathLevel(fog.beginBreath(), 0.5f)
        setContent {
            FoggedWindow(
                photo = ColorPainter(Color.Red),
                state = fog,
                modifier = Modifier.size(200.dp).testTag("window"),
            )
        }
        val pixels = onNodeWithTag("window").captureToImage().toPixelMap()
        fun greenAt(y: Float) = pixels[pixels.width / 2, (y * (pixels.height - 1)).toInt()].green

        // Below the band of fog clear glass keeps round its edges.
        assertTrue(greenAt(0.2f) < 0.12f, "the top is still clear: ${greenAt(0.2f)}")
        assertTrue(greenAt(0.8f) > 0.2f, "the bottom is fogged: ${greenAt(0.8f)}")
    }

    @Test
    fun fogOverAStrongColourIsMilkyGrey() = runComposeUiTest {
        setContent {
            FoggedWindow(
                photo = ColorPainter(Color.Red),
                state = FogState(),
                modifier = Modifier.size(200.dp).testTag("window"),
            )
        }
        val pixels = onNodeWithTag("window").captureToImage().toPixelMap()
        // Averaged over the middle of the glass: one pixel may sit on a drop or a drip.
        val patch =
            (pixels.height / 4 until pixels.height * 3 / 4).flatMap { y ->
                (pixels.width / 4 until pixels.width * 3 / 4).map { x -> pixels[x, y] }
            }
        val channels =
            listOf(
                patch.map { it.red }.average(),
                patch.map { it.green }.average(),
                patch.map { it.blue }.average(),
            )

        // The reference: colours fade to grey-white behind the fog.
        assertTrue(channels.max() - channels.min() < 0.3, "washed out, not pink: $channels")
        assertTrue(channels.min() > 0.3, "milky, not dark: $channels")
    }

    @Test
    fun clearGlassKeepsFogRoundItsEdges() = runComposeUiTest {
        setContent {
            FoggedWindow(
                photo = ColorPainter(Color.Red),
                state = FogState(startClear = true),
                modifier = Modifier.size(200.dp).testTag("window"),
            )
        }
        val pixels = onNodeWithTag("window").captureToImage().toPixelMap()
        val middle = pixels[pixels.width / 2, pixels.height / 2].green
        val corner = pixels[2, pixels.height - 3].green

        assertTrue(middle < 0.12f, "the middle is clear: $middle")
        assertTrue(corner > 0.2f, "the corner stays fogged: $corner")
    }

    @Test
    fun aNarrowStrokeClearsANarrowerLine() = runComposeUiTest {
        val fog = FogState()
        fog.extendStroke(fog.beginStroke(Offset(0.1f, 0.3f)), Offset(0.9f, 0.3f))
        fog.extendStroke(fog.beginStroke(Offset(0.1f, 0.7f), radius = 8.dp), Offset(0.9f, 0.7f))
        setContent {
            FoggedWindow(
                photo = ColorPainter(Color.Red),
                state = fog,
                modifier = Modifier.size(200.dp).testTag("window"),
            )
        }
        val pixels = onNodeWithTag("window").captureToImage().toPixelMap()
        // How many rows are clear across each line, down the middle of the window.
        fun clearRowsAround(y: Float): Int {
            val centre = (y * (pixels.height - 1)).toInt()
            return (centre - pixels.height / 5..centre + pixels.height / 5).count { row ->
                pixels[pixels.width / 2, row.coerceIn(0, pixels.height - 1)].green < 0.12f
            }
        }

        val wide = clearRowsAround(0.3f)
        val narrow = clearRowsAround(0.7f)
        assertTrue(narrow in 1 until wide, "narrow $narrow, wide $wide")
    }
}
