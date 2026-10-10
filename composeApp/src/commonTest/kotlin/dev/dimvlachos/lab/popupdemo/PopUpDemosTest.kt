@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.popupdemo

import dev.dimvlachos.lab.core.demo.FakeController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class PopUpDemosTest {
    private val demo = PopUpDemos.all.single()

    @Test
    fun theTourIsATallClipThatPlaysByItself() {
        assertEquals("tour.popup", demo.id)
        assertTrue(demo.tall)
        assertTrue(demo.autoplay)
    }

    @Test
    fun theTourOpensTheBookSailsTheBoatTurnsAndSpinsTheSailsThenShutsIt() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        demo.script.play(controller)
        val verbs = controller.calls.map { it.second.substringBefore('(') }
        assertEquals(
            listOf(
                "dragPopUpPage",
                "dragPopUpPage",
                "pullPopUpTab",
                "select",
                "pullPopUpTab",
                "pullPopUpTab",
                "select",
            ),
            verbs,
        )
        assertEquals("select(3)", controller.calls[3].second)
        assertEquals("select(0)", controller.calls.last().second)
    }

    @Test
    fun theClipLastsLongEnoughToSettleAndLoop() {
        assertTrue(demo.script.nominalDuration >= 20.seconds, "${demo.script.nominalDuration}")
        assertTrue(demo.script.nominalDuration <= 24.seconds, "${demo.script.nominalDuration}")
    }

    @Test
    fun theScriptWaitsForTheBookToShutBeforeItEnds() {
        // select(0) returns at once while the book is tapped shut a leaf at a time (about 0.85 s
        // each, then the cover lands): the script's own hold at the end is what waits for that.
        assertTrue(demo.script.holdEnd >= 3.2.seconds, "${demo.script.holdEnd}")
    }
}
