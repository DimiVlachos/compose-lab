@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.keyhookdemo

import dev.dimvlachos.lab.core.demo.FakeController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class KeyHookDemosTest {
    private val demo = KeyHookDemos.all.single()

    @Test
    fun itIsATallDemo() {
        assertEquals("keyhook.signout", demo.id)
        assertTrue(demo.tall)
    }

    @Test
    fun theClipHangsJordansKeysUpTakesAvasDownAndPutsTheRackBack() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        demo.script.play(controller)
        assertEquals(
            listOf("tapControl(hang-jordan)", "tapControl(take-ava)", "resetFeed()"),
            controller.calls.map { it.second },
        )
    }

    @Test
    fun theClipFitsInElevenSeconds() {
        assertTrue(demo.script.nominalDuration <= 11.seconds, "${demo.script.nominalDuration}")
    }

    @Test
    fun everyoneHasTwoShows() {
        assertEquals(Household.people.size, Household.shows.size)
        assertTrue(Household.shows.all { it.size == 2 })
    }
}
