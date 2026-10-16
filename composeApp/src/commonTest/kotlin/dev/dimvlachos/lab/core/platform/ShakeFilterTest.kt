package dev.dimvlachos.lab.core.platform

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ShakeFilterTest {
    @Test
    fun smallAccelerationsAreIgnored() {
        // Walking with the phone in hand peaks around 1.5–2 m/s².
        assertNull(shakeImpulse(1.2f, -1.4f))
        assertNull(shakeImpulse(0f, 2.4f))
    }

    @Test
    fun aRealShakeComesThroughInScreenSpace() {
        val impulse = assertNotNull(shakeImpulse(8f, 3f))
        // Device x is screen x; device y points up, the screen's down.
        assertEquals(Offset(8f, -3f), impulse)
    }

    @Test
    fun theHeelStrikesOfWalkingInAPocketAreIgnored() {
        // Vertical heel strikes in a pocket peak around 3–5 m/s²; a deliberate shake is well above
        // 6.
        assertNull(shakeImpulse(0f, 4.5f))
        assertNull(shakeImpulse(2f, -5f))
    }

    @Test
    fun aShakeTowardTheBottomComesThroughFlipped() {
        assertEquals(Offset(0f, 9f), shakeImpulse(0f, -9f))
    }
}
