@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.sandmandalademo

import dev.dimvlachos.lab.core.demo.FakeController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class SandMandalaDemosTest {
    private val demo = SandMandalaDemos.all.single()

    @Test
    fun itIsATallDemo() {
        assertEquals("sandmandala.history", demo.id)
        assertTrue(demo.tall)
    }

    @Test
    fun theClipGoesEveryWayInAndOutThenEndsAsItBegan() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        demo.script.play(controller)
        assertEquals(
            listOf(
                "tapControl(clear)",
                "tapControl(keep)",
                "tapControl(brush)",
                "tapControl(smear)",
                "tapControl(sweep)",
                "tapControl(undo)",
                "resetFeed()",
            ),
            controller.calls.map { it.second },
        )
    }

    @Test
    fun theSandIsBackInItsRowsBeforeTheReset() {
        // On the S23 the gather takes about 2.6 s either way (2.8 here, for slower phones), the
        // sweep's spiral 2.2, the heap's
        // hold 0.45, the pour 2.6 and the mandala's hold on the way back 0.75. The brush clears
        // about a third of the way across its row; the sweep lets go about 2.3 s into its strokes.
        val d = SandMandalaDemos
        val gather = 2.8
        val formed = .9 + (d.ShowMs + d.ToButtonMs + d.PressMs) / 1000.0 + gather
        val kept = formed + (d.SeeKeepMs + d.ShowMs + d.ToButtonMs + d.PressMs) / 1000.0 + gather
        val brushed = kept + (d.SeeRowsMs + d.ShowMs + d.RestMs + d.BrushMs / 3) / 1000.0
        val reformed = brushed + gather
        val smeared =
            reformed +
                (d.SeeMandalaMs + d.ShowMs + d.RestMs + d.GripMs + d.LegMs + d.SweepAsideMs) /
                    1000.0
        val letGo = smeared + (d.SeeSmearMs + d.RestMs + d.GripMs + 3 * d.LegMs) / 1000.0
        val cleared = letGo + 2.2 + .45 + 2.6
        val undone = cleared + (d.SeeClearedMs + d.ShowMs + d.ToButtonMs + d.PressMs) / 1000.0
        val rows = undone + 2.6 + 2.2 + .75 + gather
        assertTrue(rows < .9 + 36, "$rows")
    }

    @Test
    fun theClipFitsInFortySeconds() {
        assertTrue(demo.script.nominalDuration <= 40.seconds, "${demo.script.nominalDuration}")
    }
}
