package dev.dimvlachos.lab.core.presentation.components.navbar

import androidx.compose.runtime.BroadcastFrameClock
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext

@OptIn(ExperimentalTestApi::class)
class IndicatorStateTest {
    @Test
    fun leadingEdgeArrivesBeforeTrailingEdgeWhenStretching() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = IndicatorState(0)
        var target by mutableIntStateOf(0)
        setContent {
            LaunchedEffect(target) { state.animateTo(target, stretch = true) }
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
            LaunchedEffect(target) { state.animateTo(target, stretch = false) }
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
            LaunchedEffect(target) { state.animateTo(target, stretch = true) }
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
                edgeState.animateTo(edgeTarget, stretch = true)
            }
            LaunchedEffect(middleTarget) {
                middleState.animateTo(middleTarget, stretch = true)
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
            LaunchedEffect(target) { state.animateTo(target, stretch = true) }
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
    fun bubbleLeansIntoItsTravelAndBulgesOnLandingInBothDirections() {
        val moves =
            listOf(
                Replay(itemCount = 4, from = 0, retargets = listOf(Retarget(0, 3))),
                Replay(itemCount = 4, from = 3, retargets = listOf(Retarget(0, 0))),
                Replay(itemCount = 5, from = 0, retargets = listOf(Retarget(0, 2))),
                Replay(itemCount = 5, from = 4, retargets = listOf(Retarget(0, 2))),
                Replay(itemCount = 5, from = 1, retargets = listOf(Retarget(0, 2))),
                Replay(itemCount = 5, from = 3, retargets = listOf(Retarget(0, 2))),
            )
        for (move in moves) {
            val target = move.retargets.last().target
            val movingRight = target > move.from
            val frames = replay(move)
            frames.forEachIndexed { index, frame ->
                assertTrue(
                    frame.lead(movingRight) >= frame.trail(movingRight) - Epsilon,
                    "$move frame $index: lead ${frame.lead(movingRight)} fell behind trail " +
                        "${frame.trail(movingRight)}",
                )
            }
            assertTrue(
                frames.maxOf { it.lead(movingRight) } >= RestHalfWidth * 1.15f,
                "$move: the lead side must stretch visibly past rest",
            )
            val arrival = frames.indexOfFirst { it.leadArrived(movingRight, target) }
            assertTrue(arrival >= 0, "$move: the lead edge never arrived")
            assertTrue(
                frames.drop(arrival).any { it.lead(movingRight) > RestHalfWidth + 0.5f },
                "$move: the lead side must bulge past rest after landing",
            )
            val restFront = frontPx(target + 0.5f, RestHalfWidth, movingRight)
            val overshoot = frames.maxOf {
                (it.frontPx(movingRight) - restFront) * sign(movingRight)
            }
            assertTrue(
                overshoot > 1f,
                "$move: the front must land a little past its resting place, got $overshoot",
            )
            frames.drop(arrival + SettleFrames).forEachIndexed { index, frame ->
                assertEquals(RestHalfWidth, frame.left, 0.5f, "$move settle frame $index")
                assertEquals(RestHalfWidth, frame.right, 0.5f, "$move settle frame $index")
            }
        }
    }

    @Test
    fun aSameDirectionRetargetKeepsTheLeanOnTheLeadSide() {
        val retargets =
            listOf(
                Replay(itemCount = 5, from = 0, retargets = listOf(Retarget(0, 4), Retarget(3, 1))),
                Replay(itemCount = 5, from = 0, retargets = listOf(Retarget(0, 2), Retarget(5, 4))),
                Replay(itemCount = 5, from = 4, retargets = listOf(Retarget(0, 0), Retarget(3, 3))),
                Replay(itemCount = 5, from = 4, retargets = listOf(Retarget(0, 2), Retarget(5, 0))),
            )
        for (move in retargets) {
            val movingRight = move.retargets.last().target > move.from
            val frames = replay(move)
            frames.forEachIndexed { index, frame ->
                assertTrue(
                    frame.lead(movingRight) >= frame.trail(movingRight) - Epsilon,
                    "$move frame $index: lead ${frame.lead(movingRight)} fell behind trail " +
                        "${frame.trail(movingRight)}",
                )
            }
            assertEquals(RestHalfWidth, frames.last().left, 0.5f, "$move settles")
            assertEquals(RestHalfWidth, frames.last().right, 0.5f, "$move settles")
        }
    }

    @Test
    fun aReversingRetargetSwingsTheLeanOverWithoutAJump() {
        val reversals =
            listOf(
                Replay(itemCount = 5, from = 0, retargets = listOf(Retarget(0, 4), Retarget(6, 0))),
                Replay(itemCount = 5, from = 4, retargets = listOf(Retarget(0, 0), Retarget(6, 4))),
                Replay(itemCount = 5, from = 1, retargets = listOf(Retarget(0, 3), Retarget(4, 1))),
            )
        for (move in reversals) {
            val frames = replay(move)
            frames.zipWithNext().forEachIndexed { index, (previous, frame) ->
                assertTrue(
                    abs(frame.left - previous.left) < MaxReversalStepPx &&
                        abs(frame.right - previous.right) < MaxReversalStepPx,
                    "$move frame ${index + 1}: extents jumped from " +
                        "(${previous.left}, ${previous.right}) to (${frame.left}, ${frame.right})",
                )
            }
            val finalMovingRight = move.retargets.last().target > move.retargets.first().target
            assertTrue(
                frames.any { it.lead(finalMovingRight) > RestHalfWidth * 1.15f },
                "$move: the lean must swing over to the new direction of travel",
            )
            assertEquals(RestHalfWidth, frames.last().left, 0.5f, "$move settles")
            assertEquals(RestHalfWidth, frames.last().right, 0.5f, "$move settles")
        }
    }

    @Test
    fun calmCenterNeverOvershootsWhenRetargetedToANearerTabInTheSameDirection() {
        val rightward =
            replay(
                Replay(itemCount = 5, from = 0, retargets = listOf(Retarget(0, 4), Retarget(3, 1)))
            )
        assertTrue(
            rightward.maxOf { it.calm } <= 1.5f + 0.001f,
            "calm centre overshot 1.5 to ${rightward.maxOf { it.calm }}",
        )
        assertEquals(1.5f, rightward.last().calm, 0.001f)

        val leftward =
            replay(
                Replay(itemCount = 5, from = 4, retargets = listOf(Retarget(0, 0), Retarget(3, 3)))
            )
        assertTrue(
            leftward.minOf { it.calm } >= 3.5f - 0.001f,
            "calm centre overshot 3.5 to ${leftward.minOf { it.calm }}",
        )
        assertEquals(3.5f, leftward.last().calm, 0.001f)
    }

    private class Retarget(val frame: Int, val target: Int)

    private class Replay(val itemCount: Int, val from: Int, val retargets: List<Retarget>) {
        override fun toString() =
            "from $from via ${retargets.joinToString { "${it.target}@${it.frame}" }} of $itemCount"
    }

    private class Frame(
        val left: Float,
        val right: Float,
        val calm: Float,
        val leftSlot: Float,
        val rightSlot: Float,
    ) {
        fun lead(movingRight: Boolean) = if (movingRight) right else left

        fun frontPx(movingRight: Boolean) = frontPx(calm, lead(movingRight), movingRight)

        fun trail(movingRight: Boolean) = if (movingRight) left else right

        fun leadArrived(movingRight: Boolean, target: Int) =
            if (movingRight) rightSlot >= target + 1f else leftSlot <= target.toFloat()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun replay(move: Replay): List<Frame> {
        val frames = mutableListOf<Frame>()
        runTest(UnconfinedTestDispatcher()) {
            val clock = BroadcastFrameClock()
            withContext(clock) {
                val state = IndicatorState(move.from)
                val jobs = mutableListOf<Job>()
                for (frame in 0 until ReplayFrames) {
                    move.retargets
                        .filter { it.frame == frame }
                        .forEach { jobs += launch { state.animateTo(it.target, stretch = true) } }
                    clock.sendFrame(frame * FrameNanos)
                    val extents = bubbleExtents(lean = state.lean, restHalfWidth = RestHalfWidth)
                    frames +=
                        Frame(
                            left = extents.left,
                            right = extents.right,
                            calm = state.calmCenterSlot,
                            leftSlot = state.leftSlot,
                            rightSlot = state.rightSlot,
                        )
                }
                jobs.forEach { it.cancel() }
            }
        }
        return frames
    }

    private companion object {
        fun frontPx(calm: Float, leadExtent: Float, movingRight: Boolean) =
            calm * SlotWidth + if (movingRight) leadExtent else -leadExtent

        fun sign(movingRight: Boolean) = if (movingRight) 1f else -1f

        const val SlotWidth = 88f
        const val ReplayFrames = 150
        const val SettleFrames = 60
        const val FrameNanos = 16_000_000L
        const val Epsilon = 0.01f
        const val MaxReversalStepPx = 6f
        val RestHalfWidth = NavBarDimens.BubbleSize.value / 2f
    }
}
