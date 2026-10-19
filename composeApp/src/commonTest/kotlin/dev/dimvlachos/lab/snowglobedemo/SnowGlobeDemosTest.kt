@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.snowglobedemo

import dev.dimvlachos.lab.core.demo.FakeController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class SnowGlobeDemosTest {
    private val demo = SnowGlobeDemos.all.single()

    @Test
    fun itIsATallDemo() {
        assertEquals("snowglobe.weather", demo.id)
        assertTrue(demo.tall)
    }

    @Test
    fun theClipShakesLooksAtFridayAndComesBackToTomorrowAsItBegan() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        demo.script.play(controller)
        val names = controller.calls.map { it.second }
        assertEquals(
            listOf("shakeGlobe(1500)", "tapControl(day-2)", "tapControl(day-1)", "resetFeed()"),
            names,
        )
    }

    @Test
    fun theClipFitsInFourteenSeconds() {
        assertTrue(demo.script.nominalDuration <= 14.seconds, "${demo.script.nominalDuration}")
    }
}
