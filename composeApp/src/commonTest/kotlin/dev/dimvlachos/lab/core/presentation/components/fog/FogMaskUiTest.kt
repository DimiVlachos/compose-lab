package dev.dimvlachos.lab.core.presentation.components.fog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue

// The mask drawn in black on white: dark where the fog is, white where it is not.
@OptIn(ExperimentalTestApi::class)
class FogMaskUiTest {
    private fun fogAt(level: Float, x: Float, y: Float): Float {
        var fog = 0f
        runComposeUiTest {
            setContent {
                Box(
                    Modifier.size(100.dp)
                        .background(Color.White)
                        .drawBehind { drawFogMask(level) }
                        .testTag("mask")
                )
            }
            val pixels = onNodeWithTag("mask").captureToImage().toPixelMap()
            val pixel = pixels[(x * (pixels.width - 1)).toInt(), (y * (pixels.height - 1)).toInt()]
            fog = 1f - pixel.red
        }
        return fog
    }

    @Test
    fun belowTheFrontTheFogIsSolid() {
        assertTrue(fogAt(level = 0.5f, x = 0.5f, y = 0.9f) > 0.95f)
    }

    @Test
    fun wellAboveTheFrontThereIsNoFog() {
        assertTrue(fogAt(level = 0.5f, x = 0.5f, y = 0.1f) < 0.05f)
    }

    @Test
    fun aBreathNotYetBegunDrawsNothing() {
        assertTrue(fogAt(level = 0f, x = 0.5f, y = 0.99f) < 0.05f)
    }

    @Test
    fun aFullBreathCoversTheTop() {
        assertTrue(fogAt(level = 1f, x = 0.5f, y = 0.01f) > 0.95f)
    }
}
