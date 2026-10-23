@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.zipperdemo

import dev.dimvlachos.lab.core.demo.FakeController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class ZipperDemosTest {
    private val demo = ZipperDemos.all.single()

    @Test
    fun itIsATallDemo() {
        assertEquals("zipper.scrollbar", demo.id)
        assertTrue(demo.tall)
    }

    @Test
    fun theClipGrabsDragsThreeTimesLetsGoFlicksAndGoesHome() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        demo.script.play(controller)
        assertEquals(
            listOf(
                "tapControl(grab)",
                "tapControl(drag:0.46:1700)",
                "tapControl(drag:0.86:800)",
                "tapControl(drag:0.3:900)",
                "tapControl(release)",
                "tapControl(flick)",
                "tapControl(home)",
                "resetFeed()",
            ),
            controller.calls.map { it.second },
        )
    }

    @Test
    fun dragsRoundTrip() {
        assertEquals(.46f to 1700, ZipperDemos.dragged(ZipperDemos.drag(.46f, 1700)))
        assertNull(ZipperDemos.dragged(ZipperDemos.Grab))
        assertNull(ZipperDemos.dragged("drag:x:1"))
    }

    @Test
    fun theClipFitsInSixteenSeconds() {
        assertTrue(demo.script.nominalDuration <= 16.seconds, "${demo.script.nominalDuration}")
    }
}
