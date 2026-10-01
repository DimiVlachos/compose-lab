package dev.dimvlachos.lab.fogdemo

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import dev.dimvlachos.lab.fogdemo.presentation.components.wallGlassOn
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class ClipFrameTest {
    // The mirror's glass in the recorded clip: the stage the choreography is drawn on.
    private val glass = Size(500f * ClipGlassAspect, 500f)

    private fun assertNear(expected: Offset, actual: Offset) =
        assertTrue((expected - actual).getDistance() < 1e-4f, "expected $expected, got $actual")

    @Test
    fun theChoreographyIsDrawnOnTheMirrorsGlassInTheClip() {
        val inClip = wallGlassOn(Size(1080f, 1350f))
        assertTrue(
            abs(ClipGlassAspect - inClip.width / inClip.height) < 0.001f,
            "$ClipGlassAspect against ${inClip.width / inClip.height}",
        )
    }

    @Test
    fun onTheClipsOwnGlassNothingMoves() {
        assertNear(Offset(0.3f, 0.6f), clipFrameToWindow(Offset(0.3f, 0.6f), glass))
    }

    @Test
    fun onTallerGlassTheFrameSitsCentredAcrossTheFullWidth() {
        val tall = Size(glass.width, glass.height * 2)
        assertNear(Offset(0.5f, 0.25f), clipFrameToWindow(Offset(0.5f, 0f), tall))
        assertNear(Offset(0.5f, 0.5f), clipFrameToWindow(Offset(0.5f, 0.5f), tall))
        assertNear(Offset(0.5f, 0.75f), clipFrameToWindow(Offset(0.5f, 1f), tall))
    }

    @Test
    fun onWiderGlassTheFrameSitsCentredAcrossTheFullHeight() {
        val wide = Size(glass.width * 2, glass.height)
        assertNear(Offset(0.25f, 0.5f), clipFrameToWindow(Offset(0f, 0.5f), wide))
        assertNear(Offset(0.75f, 0.5f), clipFrameToWindow(Offset(1f, 0.5f), wide))
    }
}
