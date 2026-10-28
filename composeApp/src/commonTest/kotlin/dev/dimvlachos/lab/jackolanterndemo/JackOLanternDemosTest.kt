@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.jackolanterndemo

import dev.dimvlachos.lab.core.demo.FakeController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class JackOLanternDemosTest {
    private val demo = JackOLanternDemos.all.single()

    @Test
    fun itIsATallDemo() {
        assertEquals("jackolantern.theme", demo.id)
        assertTrue(demo.tall)
    }

    @Test
    fun theClipLightsBlowsOutThenTurnsALightRoundAndEndsByDay() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        demo.script.play(controller)
        assertEquals(
            listOf(
                "tapControl(light)",
                "tapControl(blow)",
                "tapControl(light-and-turn)",
                "resetFeed()",
            ),
            controller.calls.map { it.second },
        )
    }

    @Test
    fun theClipFitsInThirtySixSeconds() {
        assertTrue(demo.script.nominalDuration <= 36.seconds, "${demo.script.nominalDuration}")
    }
}
