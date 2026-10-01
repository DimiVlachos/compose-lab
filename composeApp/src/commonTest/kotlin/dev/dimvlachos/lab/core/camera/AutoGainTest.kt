package dev.dimvlachos.lab.core.camera

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// A dim room's camera frames brightened towards a normal level, gently and within limits.
class AutoGainTest {
    @Test
    fun aBrightEnoughFrameIsLeftAlone() {
        assertEquals(1f, autoGain(previous = 1f, meanBrightness = 0.6f))
    }

    @Test
    fun aDarkFrameIsBrightenedTowardsANormalLevel() {
        var gain = 1f
        repeat(200) { gain = autoGain(gain, meanBrightness = 0.1f) }
        assertTrue(gain in 3.5f..4.5f, "0.1 lifted to about 0.4: $gain")
    }

    @Test
    fun theGainIsCappedSoANearlyBlackFrameIsNotAllNoise() {
        var gain = 1f
        repeat(200) {
            gain = autoGain(gain, meanBrightness = 0.001f)
            assertTrue(gain <= MaxCameraGain, "never past the cap: $gain")
        }
        assertTrue(MaxCameraGain - gain < 0.01f, "up at the cap: $gain")
    }

    @Test
    fun theGainChangesGentlyFromFrameToFrame() {
        val next = autoGain(previous = 1f, meanBrightness = 0.1f)
        assertTrue(next > 1f && next < 1.5f, "one frame moves only part of the way: $next")
    }
}
