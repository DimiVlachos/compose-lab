@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.magnetdemo

import dev.dimvlachos.lab.core.demo.FakeController
import dev.dimvlachos.lab.core.presentation.components.magnet.MagnetDimens
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class MagnetDemosTest {
    @Test
    fun onThePhoneTheTableWaitsForTheHand() {
        assertFalse(MagnetDemos.all.single().autoplay)
    }

    @Test
    fun theClipPutsTwoMagnetsDownFansOneOutAndPutsThemBothBack() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        MagnetDemos.all.single().script.play(controller)
        assertEquals(
            listOf(
                "dragMagnet(sunset",
                "dragMagnet(sea",
                "tapMagnet(sunset",
                "tapMagnet(sunset",
                "releaseMagnet(sea",
                "releaseMagnet(sunset",
            ),
            controller.calls.map { it.second.substringBefore(',').substringBefore(')') },
        )
        assertTrue(controller.magnetsOut.isEmpty())
        assertNull(controller.fannedOut)
    }

    @Test
    fun theScriptedPathsStayOverTheTable() {
        for (point in MagnetDemos.SunsetPath + MagnetDemos.SeaPath) {
            assertTrue(point.x in 0.05f..0.95f && point.y in 0.05f..0.75f, "$point")
        }
    }

    @Test
    fun theTwoMagnetsEndCloseEnoughToJoin() {
        // On the 400 × 800 dp stage the clip is recorded at.
        val a = MagnetDemos.SunsetPath.last()
        val b = MagnetDemos.SeaPath.last()
        val apart = hypot((a.x - b.x) * 400f, (a.y - b.y) * 800f)
        assertTrue(
            apart <= MagnetDimens.SnapRange.value,
            "they end $apart dp apart: too far to join",
        )
    }
}
