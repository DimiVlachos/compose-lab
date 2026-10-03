package dev.dimvlachos.moodboard.morph

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class, ExperimentalSharedTransitionApi::class)
class MorphGateTest {
    @Test
    fun waitingRequestsRunOneAtATimeInOrder() = runComposeUiTest {
        val ran = mutableListOf<String>()
        var gate: MorphGate? = null
        mainClock.autoAdvance = false
        setContent {
            SharedTransitionLayout {
                val g = rememberMorphGate()
                SideEffect { gate = g }
            }
        }
        mainClock.advanceTimeByFrame()
        // No morph starts, so each request holds the gate for the 300 ms start timeout.
        gate!!.run { ran += "open" }
        gate!!.run { ran += "back" }
        gate!!.run { ran += "back again" }
        assertEquals(listOf("open"), ran)
        mainClock.advanceTimeBy(100)
        assertEquals(listOf("open"), ran)
        mainClock.advanceTimeBy(400)
        assertEquals(listOf("open", "back"), ran)
        mainClock.advanceTimeBy(400)
        assertEquals(listOf("open", "back", "back again"), ran)
    }

    @Test
    fun cancelPendingDropsWaitingRequests() = runComposeUiTest {
        val ran = mutableListOf<String>()
        var gate: MorphGate? = null
        mainClock.autoAdvance = false
        setContent {
            SharedTransitionLayout {
                val g = rememberMorphGate()
                SideEffect { gate = g }
            }
        }
        mainClock.advanceTimeByFrame()
        gate!!.run { ran += "open" }
        gate!!.run { ran += "queued push" }
        gate!!.cancelPending()
        mainClock.advanceTimeBy(1_000)
        assertEquals(listOf("open"), ran)
        gate!!.run { ran += "later" }
        assertEquals(listOf("open", "later"), ran)
    }
}
