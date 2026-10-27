@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.decoderringdemo

import dev.dimvlachos.lab.core.demo.FakeController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class DecoderRingDemosTest {
    private val demo = DecoderRingDemos.all.single()

    @Test
    fun itIsATallDemo() {
        assertEquals("decoderring.translate", demo.id)
        assertTrue(demo.tall)
    }

    @Test
    fun theClipTwistsIntoEnglishBackAndResets() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        demo.script.play(controller)
        assertEquals(
            listOf("tapControl(twist)", "tapControl(twist-back)", "resetFeed()"),
            controller.calls.map { it.second },
        )
    }

    @Test
    fun theClipFitsInTwelveSeconds() {
        assertTrue(demo.script.nominalDuration <= 12.seconds, "${demo.script.nominalDuration}")
    }
}
