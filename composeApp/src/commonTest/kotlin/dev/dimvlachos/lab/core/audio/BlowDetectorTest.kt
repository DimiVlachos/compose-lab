package dev.dimvlachos.lab.core.audio

import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BlowDetectorTest {
    private val random = Random(7)

    // A quiet room: noise around -55 dBFS.
    private fun room() = noise(0.003f, random)

    // A blow: loud noise around -17 dBFS.
    private fun blow(amplitude: Float = 0.5f) = noise(amplitude, random)

    private fun BlowDetector.settleInAQuietRoom() = repeat(20) { process(room()) }

    @Test
    fun aBlowCountsAfterAbout250ms() {
        val detector = BlowDetector()
        detector.settleInAQuietRoom()

        val strengths = List(10) { detector.process(blow()) }

        assertTrue(strengths.take(7).all { it == 0f }, "$strengths")
        assertTrue(strengths.drop(7).all { it > 0f }, "$strengths")
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
        repeat(10) {
            soft.process(blow(0.2f))
            hard.process(blow(0.6f))
        }
        val softer = soft.process(blow(0.2f))

        assertTrue(softer > 0f, "a soft blow is still a blow")
        assertTrue(softer < hard.process(blow(0.6f)))
    }

    @Test
    fun aFanSwitchedOnStopsCountingAsABlowAfterAbout4s() {
        val detector = BlowDetector()
        detector.settleInAQuietRoom()

        val strengths = List(200) { detector.process(blow()) }

        assertTrue(strengths[10] > 0f, "it starts out as a blow")
        assertTrue(strengths.takeLast(50).all { it == 0f }, "then it is the room")
    }

    @Test
    fun aRealBlowIsALowRumbleAndStillCounts() {
        val detector = BlowDetector()
        detector.settleInAQuietRoom()

        // About -14 dBFS, as a blow measured on a phone.
        val strengths = rumble(12, rms = 0.2f, random).map { detector.process(it) }

        assertTrue(strengths.drop(7).all { it > 0f }, "$strengths")
    }

    @Test
    fun aLoudSoundThatIsNoBlowDoesNotRaiseTheRoomsLevel() {
        val detector = BlowDetector()
        detector.settleInAQuietRoom()
        repeat(30) { detector.process(tone(0.3f, it, 180f, 360f, 540f, 720f)) }

        val strengths = rumble(12, rms = 0.2f, random).map { detector.process(it) }

        assertTrue(strengths.drop(7).all { it > 0f }, "$strengths")
    }

    @Test
    fun talkingAtArmsLengthIsTooQuietToBeABlow() {
        val detector = BlowDetector()
        detector.settleInAQuietRoom()

        // About -35 dBFS: well above the room, well below a blow; hiss, so only loudness rejects
        // it.
        val strengths = List(30) { detector.process(noise(0.03f, random)) }

        assertTrue(strengths.all { it == 0f }, "$strengths")
    }

    @Test
    fun theRecordersSilentFirstBuffersAreNotTheRoom() {
        // A recorder hands over digital silence while it starts; the room is what follows.
        val plain = BlowDetector().apply { settleInAQuietRoom() }
        val startedSilent =
            BlowDetector().apply {
                repeat(5) { process(silence()) }
                settleInAQuietRoom()
            }
        val blow = List(10) { noise(0.2f, random) }

        val expected = blow.map { plain.process(it) }.last()
        val actual = blow.map { startedSilent.process(it) }.last()

        assertTrue(expected in 0.1f..0.95f, "a moderate blow: $expected")
        // Each settled on its own random room, so only near-equal.
        assertTrue(abs(expected - actual) < 0.02f, "expected $expected, got $actual")
    }

    @Test
    fun shortBurstsLikeTheBreathyBitsOfSpeechDoNotCount() {
        // Measured on the phone: talking and room noise pass the other checks only in runs of up
        // to 3 frames; a blow runs 30 and more.
        val detector = BlowDetector()
        detector.settleInAQuietRoom()

        val strengths =
            (1..20).flatMap {
                List(3) { detector.process(blow()) } + List(2) { detector.process(room()) }
            }

        assertTrue(strengths.all { it == 0f }, "$strengths")
    }

    @Test
    fun aSoundOnlyAsLoudAsTalkingIsNoBlow() {
        // About -25 dBFS, where talking's passing frames sat; blows measured about -12.
        val detector = BlowDetector()
        detector.settleInAQuietRoom()

        val strengths = List(30) { detector.process(noise(0.097f, random)) }

        assertTrue(strengths.all { it == 0f }, "$strengths")
    }

    @Test
    fun aFanThatFlickersStillBecomesTheRoomAfterAbout4s() {
        // Loud all along, but now and then failing a check, as borderline noise does: the fan
        // rule counts loud time, not unbroken blowing.
        val detector = BlowDetector()
        detector.settleInAQuietRoom()

        val strengths =
            (0 until 240).map { i ->
                if (i % 25 >= 20) detector.process(tone(0.3f, i, 180f, 360f, 540f, 720f))
                else detector.process(blow())
            }

        assertTrue(
            strengths.takeLast(60).all { it == 0f },
            "then it is the room: ${strengths.takeLast(60)}",
        )
    }
}
