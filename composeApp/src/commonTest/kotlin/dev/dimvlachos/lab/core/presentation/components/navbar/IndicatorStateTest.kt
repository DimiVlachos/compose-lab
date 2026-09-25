package dev.dimvlachos.lab.core.presentation.components.navbar

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
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
        setContent {
            LaunchedEffect(target) { state.animateTo(target, stretch = true, itemCount = 5) }
        }
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
        setContent {
            LaunchedEffect(target) { state.animateTo(target, stretch = false, itemCount = 4) }
        }
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
        setContent {
            LaunchedEffect(target) { state.animateTo(target, stretch = true, itemCount = 5) }
        }
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

    @Test
    fun edgeTargetStretchesJustLikeAMiddleTarget() = runComposeUiTest {
        mainClock.autoAdvance = false
        val edgeState = IndicatorState(1)
        val middleState = IndicatorState(1)
        var edgeTarget by mutableIntStateOf(1)
        var middleTarget by mutableIntStateOf(1)
        setContent {
            LaunchedEffect(edgeTarget) {
                edgeState.animateTo(edgeTarget, stretch = true, itemCount = 5)
            }
            LaunchedEffect(middleTarget) {
                middleState.animateTo(middleTarget, stretch = true, itemCount = 5)
            }
        }
        mainClock.advanceTimeBy(100)

        runOnUiThread {
            edgeTarget = 0
            middleTarget = 2
        }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(150)

        val edgeStretch = abs((edgeState.rightSlot - edgeState.leftSlot) - 1f)
        val middleStretch = abs((middleState.rightSlot - middleState.leftSlot) - 1f)
        assertTrue(edgeStretch > 0.01f, "an edge target should stretch just like a middle one")
        assertTrue(middleStretch > 0.01f, "a middle target should stretch")
        assertTrue(
            abs(edgeStretch - middleStretch) < 0.05f,
            "edge stretch $edgeStretch and middle stretch $middleStretch should read the same",
        )
    }

    @Test
    fun calmCenterGlidesToTheTargetWithoutOvershoot() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = IndicatorState(2)
        var target by mutableIntStateOf(2)
        setContent {
            LaunchedEffect(target) { state.animateTo(target, stretch = true, itemCount = 3) }
        }
        mainClock.advanceTimeBy(100)

        runOnUiThread { target = 0 }
        Snapshot.sendApplyNotifications()

        var minCenter = Float.MAX_VALUE
        var maxCenter = Float.MIN_VALUE
        var previous = state.calmCenterSlot
        var wentBackwards = false
        repeat(90) {
            mainClock.advanceTimeBy(16)
            val current = state.calmCenterSlot
            if (current > previous + 0.001f) wentBackwards = true
            previous = current
            minCenter = min(minCenter, current)
            maxCenter = max(maxCenter, current)
        }

        assertTrue(!wentBackwards, "the calm centre must move monotonically towards the target")
        assertTrue(
            minCenter >= 0.5f - 0.001f,
            "the calm centre must never leave [0.5, itemCount - 0.5]",
        )
        assertTrue(
            maxCenter <= 2.5f + 0.001f,
            "the calm centre must never leave [0.5, itemCount - 0.5]",
        )
        assertEquals(0.5f, state.calmCenterSlot, 0.01f)
    }

    @Test
    fun middleTargetStillStretches() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = IndicatorState(0)
        var target by mutableIntStateOf(0)
        setContent {
            LaunchedEffect(target) { state.animateTo(target, stretch = true, itemCount = 5) }
        }
        mainClock.advanceTimeBy(100)

        runOnUiThread { target = 2 }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(150)

        assertTrue(
            abs((state.rightSlot - state.leftSlot) - 1f) > 0.01f,
            "a middle target should still stretch away from 1",
        )
    }
}
