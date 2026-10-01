package dev.dimvlachos.lab.fogdemo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue

// The wall photo itself, not only the maths: its mirror's dark frame runs right around the glass.
@OptIn(ExperimentalTestApi::class)
class MirrorOnWallUiTest {
    private fun frameHugsTheGlass(stage: DpSize) = runComposeUiTest {
        setContent {
            Box(Modifier.size(stage).testTag("wall")) {
                MirrorOnWall { glass -> Box(glass.background(Color.Magenta)) }
            }
        }
        val pixels = onNodeWithTag("wall").captureToImage().toPixelMap()
        val glass = wallGlassOn(Size(pixels.width.toFloat(), pixels.height.toFloat()))
        // About 20 of the photo's pixels out: past the glass's light bevel, into the black frame.
        val scale = maxOf(pixels.width / 1220f, pixels.height / 2639f)
        val gap = (20 * scale).toInt().coerceAtLeast(3)
        val mid = glass.center
        val outside =
            listOf(
                mid.x.toInt() to glass.top.toInt() - gap,
                mid.x.toInt() to glass.bottom.toInt() + gap,
                glass.left.toInt() - gap to mid.y.toInt(),
                glass.right.toInt() + gap to mid.y.toInt(),
            )
        for ((x, y) in outside) {
            val c = pixels[x, y]
            assertTrue(
                (c.red + c.green + c.blue) / 3 < 0.45f,
                "the frame at ($x, $y) on $stage: $c",
            )
        }
        val inside = pixels[mid.x.toInt(), mid.y.toInt()]
        assertTrue(inside.red > 0.9f && inside.green < 0.1f, "the glass: $inside")
    }

    @Test
    fun onAStageWiderThanTheWallTheFrameHugsTheGlass() = frameHugsTheGlass(DpSize(400.dp, 500.dp))

    @Test
    fun onAStageTheWallsShapeTheFrameHugsTheGlass() = frameHugsTheGlass(DpSize(400.dp, 865.dp))
}
