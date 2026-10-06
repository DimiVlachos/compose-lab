@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.pullcorddemo

import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.FakeController
import dev.dimvlachos.lab.core.presentation.components.pullcord.PullCordDimens
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class PullCordDemosTest {
    @Test
    fun onThePhoneTheLampWaitsToBePulled() {
        assertFalse(PullCordDemos.all.single().autoplay)
    }

    @Test
    fun theClipSwitchesTheLampOnSwingsItAndSwitchesItOff() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        PullCordDemos.all.single().script.play(controller)
        // On, still on after the tug, and off again.
        assertEquals(listOf(true, true, false), controller.litAfterPulls)
        val tug = controller.calls.filter { it.second.startsWith("pullCord") }[1].second
        assertTrue(tug.startsWith("pullCord(${PullCordDemos.Tug}"), tug)
        assertTrue(tug.endsWith(", ${PullCordDemos.Swing})"), tug)
        assertFalse(controller.lampLit)
    }

    @Test
    fun theSwingInTheMiddleIsSidewaysAndClicksNothing() {
        val script = PullCordDemos.all.single().script
        // Two pulls past the click and one tug short of it, between them.
        assertTrue(PullCordDemos.Pull >= PullCordDimens.ClickPull)
        assertTrue(PullCordDemos.Tug < PullCordDimens.ClickPull)
        assertTrue(PullCordDemos.Swing > PullCordDemos.Tug)
        assertEquals(3, script.steps.size)
    }

    @Test
    fun theFakeClicksOnlyAPullThatIsMoreDownThanAlong() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        val down = PullCordDimens.ClickPull + 10.dp
        controller.pullCord(down, 400.milliseconds, across = down)
        assertFalse(controller.lampLit, "a pull as far along as down only swings")
        controller.pullCord(down, 400.milliseconds, across = down / 2)
        assertTrue(controller.lampLit)
    }
}
