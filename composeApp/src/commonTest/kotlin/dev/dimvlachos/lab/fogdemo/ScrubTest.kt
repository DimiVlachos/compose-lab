package dev.dimvlachos.lab.fogdemo

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class ScrubTest {
    // Across and back: two half-second sweeps.
    private val scrub =
        Scrub(
            start = Offset(0.2f, 0.5f),
            sweeps =
                listOf(
                    Sweep(to = Offset(0.8f, 0.5f), bow = 0.05f, duration = 500.milliseconds),
                    Sweep(to = Offset(0.2f, 0.6f), bow = 0.05f, duration = 500.milliseconds),
                ),
        )
    private val path = scrub.path(step = 10.milliseconds)

    @Test
    fun theDurationIsEverySweepInTurn() {
        assertEquals(1_000.milliseconds, scrub.duration)
    }

    @Test
    fun thePathIsSampledAtEvenTimeStepsFromTouchdownToTheLastTurn() {
        assertEquals(101, path.size)
        assertEquals(Offset(0.2f, 0.5f), path.first())
        assertEquals(Offset(0.8f, 0.5f), path[50])
        assertEquals(Offset(0.2f, 0.6f), path.last())
    }

    @Test
    fun theFingerSlowsIntoEachTurnAndIsFastestMidSweep() {
        fun speedAt(i: Int) = (path[i + 1] - path[i]).getDistance()
        assertTrue(speedAt(48) < speedAt(24) / 4, "slow into the turn")
        assertTrue(speedAt(51) < speedAt(74) / 4, "slow out of it")
    }

    @Test
    fun theFingerLandsAndLiftsOffAlreadyMoving() {
        // Only the turns come to rest: a hand that touched down still would press a dot first.
        fun speedAt(i: Int) = (path[i + 1] - path[i]).getDistance()
        assertTrue(speedAt(0) > speedAt(24) / 2, "lands moving")
        assertTrue(speedAt(99) > speedAt(74) / 2, "lifts moving")
    }

    @Test
    fun aSweepBowsUpTheGlassAsAForearmSwingsFromTheElbow() {
        assertTrue(path[25].y < 0.5f - 0.04f, "mid-sweep sits above the chord: ${path[25]}")
        // Going back the other way it still bows up, not down.
        assertTrue(path[75].y < 0.55f - 0.04f, "the return bows up too: ${path[75]}")
    }
}
