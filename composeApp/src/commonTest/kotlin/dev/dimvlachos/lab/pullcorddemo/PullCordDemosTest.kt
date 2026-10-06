@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.pullcorddemo

import dev.dimvlachos.lab.core.demo.FakeController
import dev.dimvlachos.lab.core.presentation.components.pullcord.PullCordDimens
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
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
}
