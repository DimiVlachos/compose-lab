@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.padlockdemo

import dev.dimvlachos.lab.core.demo.FakeController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class PadlockDemosTest {
    private val demo = PadlockDemos.all.single()

    @Test
    fun itIsATallDemo() {
        assertEquals("padlock.password", demo.id)
        assertTrue(demo.tall)
    }

    @Test
    fun theClipTypesAStrongPasswordPeeksErasesAndRetypesThenClears() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        demo.script.play(controller)
        assertEquals(
            listOf(
                "tapControl(focus)",
                "tapControl(type:maple)",
                "tapControl(type:42)",
                "tapControl(type:-)",
                "tapControl(type:R)",
                "tapControl(type:ive)",
                "tapControl(type:r!)",
                "tapControl(peek)",
                "tapControl(peek)",
                "tapControl(erase:3)",
                "tapControl(type:er!)",
                "resetFeed()",
            ),
            controller.calls.map { it.second },
        )
    }

    @Test
    fun typedChunksSpellTheStrongPassword() {
        val typed = "maple" + "42" + "-" + "R" + "ive" + "r!"
        assertEquals("maple42-River!", typed)
        assertEquals("maple42-Riv", typed.dropLast(3))
    }

    @Test
    fun controlsRoundTrip() {
        assertEquals("er!", PadlockDemos.typed(PadlockDemos.type("er!")))
        assertEquals(3, PadlockDemos.erased(PadlockDemos.erase(3)))
        assertNull(PadlockDemos.typed(PadlockDemos.Peek))
        assertNull(PadlockDemos.erased(PadlockDemos.Focus))
    }

    @Test
    fun theClipFitsInSixteenSeconds() {
        assertTrue(demo.script.nominalDuration <= 16.seconds, "${demo.script.nominalDuration}")
    }
}
