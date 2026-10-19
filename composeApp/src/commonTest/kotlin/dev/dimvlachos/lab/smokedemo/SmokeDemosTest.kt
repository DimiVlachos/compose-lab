@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.smokedemo

import dev.dimvlachos.lab.core.demo.FakeController
import dev.dimvlachos.lab.core.presentation.components.smoke.SmokeDimens
import dev.dimvlachos.lab.core.presentation.components.smoke.frontUnder
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class SmokeDemosTest {
    private val demo = SmokeDemos.all.single()

    @Test
    fun itIsATallList() {
        assertEquals("smoke.delete", demo.id)
        assertTrue(demo.tall)
    }

    @Test
    fun theBurnsGoPastAThirdAndTheShortSwipeDoesnt() {
        // The scripted finger takes hold 40 dp in from the edge, an eighth of a narrow phone's row.
        val grip = 0.125f
        assertTrue(frontUnder(abs(SmokeDemos.Burn), grip) > SmokeDimens.Commit)
        assertTrue(frontUnder(abs(SmokeDemos.Archive), grip) > SmokeDimens.Commit)
        assertTrue(frontUnder(abs(SmokeDemos.Short), grip) < SmokeDimens.Commit)
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
