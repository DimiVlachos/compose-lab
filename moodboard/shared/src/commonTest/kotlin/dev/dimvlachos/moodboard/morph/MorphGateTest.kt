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
    fun requestsArrivingWhileHeldAllRunInOrder() = runComposeUiTest {
        val ran = mutableListOf<String>()
        var gate: ((() -> Unit) -> Unit)? = null
        mainClock.autoAdvance = false
        setContent {
            SharedTransitionLayout {
                val g = rememberMorphGate()
                SideEffect { gate = g }
            }
        }
        mainClock.advanceTimeByFrame()
        // No morph starts, so each request holds the gate until the start timeout passes.
        gate!!.invoke { ran += "open" }
        gate!!.invoke { ran += "back" }
        gate!!.invoke { ran += "back again" }
        mainClock.advanceTimeBy(2_000)
        assertEquals(listOf("open", "back", "back again"), ran)
    }
}
