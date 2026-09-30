package dev.dimvlachos.lab.core.audio

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

class PeriodicityTest {
    @Test
    fun aVoiceLikeToneRepeatsAtItsPitch() {
        val p = periodicity(tone(0.3f, 0, 180f, 360f, 540f, 720f), MicSampleRate)
        assertTrue(p > 0.8f, "tone periodicity $p")
    }

    @Test
    fun hissDoesNotRepeat() {
        val p = periodicity(noise(0.3f, Random(1)), MicSampleRate)
        assertTrue(p < 0.5f, "hiss periodicity $p")
    }

    @Test
    fun aBlowsRumbleDoesNotRepeatEither() {
        val p = periodicity(rumble(1, rms = 0.1f, Random(2)).single(), MicSampleRate)
        assertTrue(p < 0.5f, "rumble periodicity $p")
    }
}
