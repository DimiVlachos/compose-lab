@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.conchdemo

import dev.dimvlachos.lab.core.demo.FakeController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class ConchDemosTest {
    private val demo = ConchDemos.all.single()

    @Test
    fun itIsATallDemo() {
        assertEquals("conch.voice", demo.id)
        assertTrue(demo.tall)
    }

    @Test
    fun theClipPlaysMayasNoteSendsOneCancelsOneAndPutsItBack() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        demo.script.play(controller)
        assertEquals(
            listOf(
                "tapControl(play-maya)",
                "tapControl(hold)",
                "tapControl(release)",
                "tapControl(hold)",
                "tapControl(slide)",
                "tapControl(release)",
                "resetFeed()",
            ),
            controller.calls.map { it.second },
        )
    }

    @Test
    fun theHoldsAreThePrototypesLengths() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        demo.script.play(controller)
        val at = controller.calls.map { it.first }
        // Held 3.2 s after the 0.7 s glide; then 1.3 s, the 0.52 s slide and 0.7 s after the 0.4 s
        // one.
        assertEquals(3900L, at[2] - at[1])
        assertEquals(2920L, at[5] - at[3])
    }

    @Test
    fun theClipFitsInEighteenSeconds() {
        assertTrue(demo.script.nominalDuration <= 18.seconds, "${demo.script.nominalDuration}")
    }
}
