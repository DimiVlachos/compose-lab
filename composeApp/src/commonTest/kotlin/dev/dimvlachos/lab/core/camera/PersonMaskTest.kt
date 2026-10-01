package dev.dimvlachos.lab.core.camera

import androidx.compose.ui.unit.IntSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PersonMaskTest {
    @Test
    fun sureOfTheRoomItIsClearSureOfThePersonOpaque() {
        assertEquals(0f, personAlpha(0f))
        assertEquals(0f, personAlpha(0.3f))
        assertEquals(1f, personAlpha(0.8f))
        assertEquals(1f, personAlpha(1f))
    }

    @Test
    fun betweenItRampsSoftlyAndOnlyEverUp() {
        val half = personAlpha(0.55f)
        assertTrue(half in 0.3f..0.7f, "halfway: $half")
        var last = 0f
        for (i in 0..100) {
            val a = personAlpha(i / 100f)
            assertTrue(a >= last, "at ${i / 100f}: $a after $last")
            last = a
        }
    }

    @Test
    fun theFrameIsShrunkToTheModelsSizeKeepingItsShape() {
        assertEquals(IntSize(256, 144), segmentSize(1280, 720))
        assertEquals(IntSize(192, 256), segmentSize(720, 960))
    }

    @Test
    fun theFirstFrameMayTakeLongerWhileTheModelWakesUp() {
        val health = SegmentationHealth()
        assertTrue(health.waitMillis > 1_000, "first: ${health.waitMillis}")
        health.succeeded()
        assertTrue(health.waitMillis <= 500, "after: ${health.waitMillis}")
    }

    @Test
    fun aFewSlowFramesDoNotGiveUpButManyInARowDo() {
        val health = SegmentationHealth()
        health.succeeded()
        repeat(4) { health.failed() }
        assertFalse(health.givenUp, "a few")
        health.succeeded()
        repeat(4) { health.failed() }
        assertFalse(health.givenUp, "a few again, after one that worked")
        health.failed()
        assertTrue(health.givenUp, "five in a row")
    }
}
