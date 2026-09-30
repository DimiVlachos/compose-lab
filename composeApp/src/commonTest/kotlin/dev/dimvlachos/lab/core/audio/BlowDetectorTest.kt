package dev.dimvlachos.lab.core.audio

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BlowDetectorTest {
    private val random = Random(7)

    // A quiet room: noise around -55 dBFS.
    private fun room() = noise(0.003f, random)

    // A blow: loud noise around -15 dBFS.
    private fun blow(amplitude: Float = 0.3f) = noise(amplitude, random)

    private fun BlowDetector.settleInAQuietRoom() = repeat(20) { process(room()) }

    @Test
    fun aBlowCountsAfterAbout100ms() {
        val detector = BlowDetector()
        detector.settleInAQuietRoom()

        val strengths = List(5) { detector.process(blow()) }

        assertEquals(0f, strengths[0])
        assertEquals(0f, strengths[1])
        assertTrue(strengths.drop(2).all { it > 0f }, "$strengths")
    }

    @Test
    fun aVoiceLikeToneIsNotABlow() {
        val detector = BlowDetector()
        detector.settleInAQuietRoom()

        val strengths = List(30) { detector.process(tone(0.3f, it, 180f, 360f, 540f, 720f)) }

        assertTrue(strengths.all { it == 0f }, "$strengths")
    }

    @Test
    fun silenceIsNotABlow() {
        val detector = BlowDetector()
        assertTrue(List(40) { detector.process(silence()) }.all { it == 0f })
    }

    @Test
    fun aRoomThatIsNoisyFromTheStartIsNotABlow() {
        val detector = BlowDetector()
        assertTrue(List(60) { detector.process(blow()) }.all { it == 0f })
    }

    @Test
    fun theBlowEndsAfterAbout130msOfQuiet() {
        val detector = BlowDetector()
        detector.settleInAQuietRoom()
        repeat(10) { detector.process(blow()) }

        val strengths = List(4) { detector.process(room()) }

        assertTrue(strengths.take(3).all { it > 0f }, "$strengths")
        assertEquals(0f, strengths[3])
    }

    @Test
    fun aHarderBlowIsStronger() {
        val soft = BlowDetector().apply { settleInAQuietRoom() }
        val hard = BlowDetector().apply { settleInAQuietRoom() }
        repeat(4) {
            soft.process(blow(0.05f))
            hard.process(blow(0.5f))
        }

        assertTrue(soft.process(blow(0.05f)) < hard.process(blow(0.5f)))
    }

    @Test
    fun aFanSwitchedOnStopsCountingAsABlowAfterAbout4s() {
        val detector = BlowDetector()
        detector.settleInAQuietRoom()

        val strengths = List(200) { detector.process(blow()) }

        assertTrue(strengths[10] > 0f, "it starts out as a blow")
        assertTrue(strengths.takeLast(50).all { it == 0f }, "then it is the room")
    }
}
