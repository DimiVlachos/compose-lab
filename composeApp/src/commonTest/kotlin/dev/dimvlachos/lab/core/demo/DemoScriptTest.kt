@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.core.demo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest

class DemoScriptTest {
    @Test
    fun stepsFireAtTheirTimestampsAfterTheStartHold() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        val script =
            demoScript(holdStart = 600.milliseconds, holdEnd = 400.milliseconds) {
                at(800.milliseconds) { select(2) }
                at(200.milliseconds) { select(1) }
            }

        script.play(controller)

        assertEquals(listOf(800L to "select(1)", 1_400L to "select(2)"), controller.calls)
        assertEquals(1_800L, testScheduler.currentTime)
    }

    @Test
    fun aLongStepExtendsPlaybackBeforeTheEndHold() = runTest {
        val controller = FakeController { testScheduler.currentTime }
        val script =
            demoScript(holdStart = 600.milliseconds, holdEnd = 400.milliseconds) {
                at(100.milliseconds) {
                    delay(2.seconds)
                    select(1)
                }
            }

        script.play(controller)

        assertEquals(listOf(2_700L to "select(1)"), controller.calls)
        assertEquals(3_100L, testScheduler.currentTime)
    }

    @Test
    fun nominalDurationIsBothHoldsPlusTheLastStep() {
        val script =
            demoScript(holdStart = 600.milliseconds, holdEnd = 600.milliseconds) {
                at(800.milliseconds) { select(1) }
                at(300.milliseconds) { select(0) }
            }
        assertEquals(2_000.milliseconds, script.nominalDuration)
    }
}
