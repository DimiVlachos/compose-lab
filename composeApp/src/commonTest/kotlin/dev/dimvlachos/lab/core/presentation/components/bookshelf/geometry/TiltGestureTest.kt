package dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry

import kotlin.math.atan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TiltGestureTest {

    @Test
    fun tiltGrowsGentlyFromUprightWithTheDrag() {
        val grab = 100f
        val small = TiltGesture.tiltFor(dragY = 8f, grabHeight = grab, startTilt = 0f)

        assertEquals(atan(8f / grab), small, 1e-4f)
        assertTrue(small < 0.1f)
        assertEquals(
            TiltGesture.tiltFor(dragY = 20f, grabHeight = grab, startTilt = 0f),
            TiltGesture.tiltFor(
                dragY = 10f,
                grabHeight = grab,
                startTilt = TiltGesture.tiltFor(dragY = 10f, grabHeight = grab, startTilt = 0f),
            ),
            1e-4f,
        )
    }

    @Test
    fun tiltIsClampedBetweenUprightAndTheMaximumLean() {
        assertEquals(
            0f,
            TiltGesture.tiltFor(dragY = -50f, grabHeight = 100f, startTilt = 0f),
            1e-4f,
        )
        assertEquals(
            TiltGesture.MAX_TILT,
            TiltGesture.tiltFor(dragY = 500f, grabHeight = 100f, startTilt = 0f),
            1e-4f,
        )
    }

    @Test
    fun onlyAGrabOnTheUpperPartOfTheSpineTipsTheBook() {
        assertTrue(TiltGesture.canTip(grabHeight = 60f, spineHeight = 100f))
        assertFalse(TiltGesture.canTip(grabHeight = 50f, spineHeight = 100f))
    }

    @Test
    fun aLowGrabTipsTheBookNoFasterThanAGrabAtTheTipZone() {
        assertEquals(55f, TiltGesture.lever(grabHeight = 10f, spineHeight = 100f), 1e-4f)
        assertEquals(80f, TiltGesture.lever(grabHeight = 80f, spineHeight = 100f), 1e-4f)
    }

    @Test
    fun releaseTakesTheBookOutPastTheTiltThresholdOrOnAFastFlick() {
        assertTrue(TiltGesture.shouldTakeOut(tilt = 0.35f, velocityY = 0f))
        assertTrue(TiltGesture.shouldTakeOut(tilt = 0.13f, velocityY = 700f))
        assertFalse(TiltGesture.shouldTakeOut(tilt = 0.11f, velocityY = 900f))
        assertFalse(TiltGesture.shouldTakeOut(tilt = 0.3f, velocityY = 100f))
    }
}
