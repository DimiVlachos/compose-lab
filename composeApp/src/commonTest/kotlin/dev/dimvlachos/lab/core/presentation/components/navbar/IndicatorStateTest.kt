package dev.dimvlachos.lab.core.presentation.components.navbar

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class IndicatorStateTest {
    @Test
    fun leadingEdgeArrivesBeforeTrailingEdgeWhenStretching() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = IndicatorState(0)
        var target by mutableIntStateOf(0)
        setContent { LaunchedEffect(target) { state.animateTo(target, stretch = true) } }
        mainClock.advanceTimeBy(100)

        runOnUiThread { target = 3 }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(150)

        val leadProgress = (state.rightSlot - 1f) / 3f
        val trailProgress = state.leftSlot / 3f
        assertTrue(
            leadProgress > trailProgress + 0.1f,
            "lead $leadProgress should be ahead of trail $trailProgress",
        )

        mainClock.advanceTimeBy(3_000)
        assertEquals(3f, state.leftSlot, 0.01f)
        assertEquals(4f, state.rightSlot, 0.01f)
    }

    @Test
    fun edgesMoveTogetherWithoutStretch() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = IndicatorState(0)
        var target by mutableIntStateOf(0)
        setContent { LaunchedEffect(target) { state.animateTo(target, stretch = false) } }
        mainClock.advanceTimeBy(100)

        runOnUiThread { target = 2 }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(120)

        assertTrue(abs((state.rightSlot - 1f) - state.leftSlot) < 0.01f)
    }

    @Test
    fun retargetingMidFlightSettlesOnTheLastTarget() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = IndicatorState(0)
        var target by mutableIntStateOf(0)
        setContent { LaunchedEffect(target) { state.animateTo(target, stretch = true) } }
        mainClock.advanceTimeBy(100)

        runOnUiThread { target = 3 }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(80)
        runOnUiThread { target = 1 }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(3_000)

        assertEquals(1f, state.leftSlot, 0.01f)
        assertEquals(2f, state.rightSlot, 0.01f)
        assertEquals(1.5f, state.centerSlot, 0.01f)
    }
}
