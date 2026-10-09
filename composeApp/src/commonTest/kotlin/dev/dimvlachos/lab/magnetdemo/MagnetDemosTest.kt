@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.magnetdemo

import androidx.compose.ui.geometry.Offset
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
    fun theClipSearchesApartThenTogetherOpensAPhotoAndPutsEverythingBack() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        MagnetDemos.all.single().script.play(controller)
        assertEquals(
            listOf(
                "dragMagnet(sunset",
                "dragMagnet(sea",
                "dragMagnet(sea",
                "tapMagnet(sunset",
                "tapPhoto(corfu",
                "tapPhoto(corfu",
                "tapMagnet(sunset",
                "releaseMagnet(sea",
                "releaseMagnet(sunset",
            ),
            controller.calls.map { it.second.substringBefore(',').substringBefore(')') },
        )
        assertTrue(controller.magnetsOut.isEmpty())
        assertNull(controller.fannedOut)
        assertNull(controller.openedPhoto)
    }

    @Test
    fun theScriptedPathsStayOverTheTable() {
        for (point in MagnetDemos.SunsetPath + MagnetDemos.SeaPath + MagnetDemos.SeaJoinPath) {
            assertTrue(point.x in 0.05f..0.95f && point.y in 0.05f..0.75f, "$point")
        }
    }

    // On the 400 × 800 dp stage the clip is recorded at.
    private fun apart(a: Offset, b: Offset): Float = hypot((a.x - b.x) * 400f, (a.y - b.y) * 800f)

    @Test
    fun seaIsFirstPutDownTooFarFromSunsetToJoinIt() {
        val apart = apart(MagnetDemos.SunsetPath.last(), MagnetDemos.SeaPath.last())
        assertTrue(apart > MagnetDimens.SnapRange.value * 2f, "they lie $apart dp apart")
    }

    @Test
    fun thenItIsBroughtCloseEnoughToJoin() {
        val apart = apart(MagnetDemos.SunsetPath.last(), MagnetDemos.SeaJoinPath.last())
        assertTrue(apart <= MagnetDimens.SnapRange.value, "they end $apart dp apart")
    }
}
