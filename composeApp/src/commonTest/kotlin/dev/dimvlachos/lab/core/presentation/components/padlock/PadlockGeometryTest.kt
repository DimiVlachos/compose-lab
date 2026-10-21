package dev.dimvlachos.lab.core.presentation.components.padlock

import kotlin.test.Test
import kotlin.test.assertTrue

class PadlockGeometryTest {
    @Test
    fun theKeyholeCoverNeverSwingsOverTheStamp() {
        assertTrue(PadlockGeometry.coverClearsStamp())
        // Fully open and at the spring's overshoot, the disc's top stays under the stamp's line.
        assertTrue(PadlockGeometry.coverTop(1.15f) > StampAt + .02f)
    }

    @Test
    fun theLockBoxFitsAndCentresInTheHero() {
        val box = PadlockBox(360f, 240f)
        assertTrue(box.scale <= 1.35f)
        assertTrue(box.left >= 0f && box.left + 300f * box.scale <= 360f + .01f)
        assertTrue(box.hitLeft > box.left && box.hitLeft + box.hitWidth < 360f)
    }
}
