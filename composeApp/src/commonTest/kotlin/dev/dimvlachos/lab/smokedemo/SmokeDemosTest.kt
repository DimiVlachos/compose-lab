@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.smokedemo

import dev.dimvlachos.lab.core.demo.FakeController
import dev.dimvlachos.lab.core.presentation.components.smoke.SmokeDimens
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class SmokeDemosTest {
    private val demo = SmokeDemos.all.single()

    @Test
    fun onThePhoneTheListWaitsToBeSwiped() {
        assertEquals("smoke.delete", demo.id)
        assertFalse(demo.autoplay)
        assertTrue(demo.tall)
    }

    @Test
    fun theBurnsGoPastAThirdAndTheShortSwipeDoesnt() {
        assertTrue(abs(SmokeDemos.Burn) * SmokeDimens.Pull > SmokeDimens.Commit)
        assertTrue(abs(SmokeDemos.Archive) * SmokeDimens.Pull > SmokeDimens.Commit)
        assertTrue(abs(SmokeDemos.Short) * SmokeDimens.Pull < SmokeDimens.Commit)
    }

    @Test
    fun theClipBurnsUndoesArchivesClearsAndPutsTheListBack() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        demo.script.play(controller)
        assertEquals(
            listOf(
                "patterson" to SmokeDemos.Burn,
                "ferry" to SmokeDemos.Short,
                "jake" to SmokeDemos.Archive,
            ),
            controller.chatSwipes,
        )
        val names = controller.calls.map { it.second }
        assertEquals(3, names.count { it == "tapControl(undo)" })
        assertEquals(1, names.count { it == "tapControl(clear-read)" })
        // Every action is undone before the end, so the reset finds the list as it began.
        assertEquals("resetFeed()", names.last())
        assertTrue(names.indexOf("tapControl(clear-read)") < names.lastIndexOf("tapControl(undo)"))
    }

    @Test
    fun theClipFitsInTwentySixSeconds() {
        assertTrue(demo.script.nominalDuration <= 26.seconds, "${demo.script.nominalDuration}")
    }
}
