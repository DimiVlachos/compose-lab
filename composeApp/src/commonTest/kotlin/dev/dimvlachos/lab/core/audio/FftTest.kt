package dev.dimvlachos.lab.core.audio

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FftTest {
    @Test
    fun aToneLandsInItsOwnBin() {
        // 16 kHz over 512 samples is 31.25 Hz a bin, so 1 kHz is bin 32.
        val power = powerSpectrum(tone(0.5f, 0, 1_000f))
        assertEquals(257, power.size)
        assertEquals(32, power.indices.maxBy { power[it] })
    }

    @Test
    fun noiseIsFlatAndAToneIsNot() {
        val noisy =
            spectralFlatness(powerSpectrum(noise(0.3f, Random(1))), MicSampleRate, 100f, 4_000f)
        val tonal =
            spectralFlatness(
                powerSpectrum(tone(0.3f, 0, 220f, 440f, 660f)),
                MicSampleRate,
                100f,
                4_000f,
            )
        assertTrue(noisy > 0.4f, "noise flatness $noisy")
        assertTrue(tonal < 0.1f, "tone flatness $tonal")
    }
}
