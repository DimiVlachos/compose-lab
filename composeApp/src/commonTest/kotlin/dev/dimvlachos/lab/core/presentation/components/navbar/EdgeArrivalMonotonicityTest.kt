package dev.dimvlachos.lab.core.presentation.components.navbar

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class EdgeArrivalMonotonicityTest {

    private val itemCount = 3
    private val widths = listOf(264f, 344f, 192f)
    private val ms = listOf(0f, 0.5f)
    private val transitions =
        listOf(Transition(2, 0), Transition(1, 0), Transition(0, 2), Transition(1, 2))
    private val framesPerArrival = 60
    private val frameNanos = 16_000_000L
    private val tipMarginPx = 10f
    private val monotonicTolerancePx = 0.5f
    private val maxFrameToFrameDeltaPx = 9f

    private class Transition(val from: Int, val to: Int)

    private class Combo(val width: Float, val m: Float, val transition: Transition) {
        override fun toString() = "width=$width m=$m ${transition.from}->${transition.to}"
    }

    @Test
    fun barEndTipMovesMonotonicallyTowardsRestOnEveryTabArrival() =
        runTest(UnconfinedTestDispatcher()) {
            val combos = buildList {
                for (width in widths) {
                    for (m in ms) {
                        for (transition in transitions) {
                            add(Combo(width, m, transition))
                        }
                    }
                }
            }

            var worstReversal = 0f
            var worstReversalDescription = ""
            var worstDelta = 0f
            var worstDeltaDescription = ""
            val frameClock = BroadcastFrameClock()

            withContext(frameClock) {
                for (combo in combos) {
                    val rightTip = combo.transition.to == itemCount - 1
                    val restSamples =
                        tipSamples(
                            restPath(combo.transition.to, combo.width, combo.m),
                            combo.width,
                            rightTip,
                        )

                    val state = IndicatorState(combo.transition.from)
                    val job = launch {
                        state.animateTo(combo.transition.to, stretch = true)
                    }

                    var previousSamples: Tip? = null
                    var previousDeviation = Float.MAX_VALUE
                    for (frame in 0 until framesPerArrival) {
                        frameClock.sendFrame(frame * frameNanos)
                        val path = framePath(state.calmCenterSlot, combo.width, combo.m)
                        val samples = tipSamples(path, combo.width, rightTip)

                        val deviation = hausdorff(restSamples, samples)
                        val reversal = deviation - previousDeviation
                        if (reversal > worstReversal) {
                            worstReversal = reversal
                            worstReversalDescription =
                                "$combo frame=$frame deviation=$deviation prev=$previousDeviation"
                        }
                        previousDeviation = deviation

                        previousSamples?.let { previous ->
                            val delta = hausdorff(previous, samples)
                            if (delta > worstDelta) {
                                worstDelta = delta
                                worstDeltaDescription = "$combo frame=$frame"
                            }
                        }
                        previousSamples = samples
                    }
                    job.cancel()
                }
            }

            assertTrue(
                worstReversal < monotonicTolerancePx,
                "bar-end tip moved away from rest by ${worstReversal}px at $worstReversalDescription",
            )
            assertTrue(
                worstDelta < maxFrameToFrameDeltaPx,
                "worst bar-end tip frame-to-frame change was ${worstDelta}px at $worstDeltaDescription",
            )
        }

    private fun framePath(calmCenterSlot: Float, width: Float, m: Float): Path {
        val slot = width / itemCount
        val cornerRadius = barHeightPx(m, BarHeightPx, CollapsedBarHeightPx) / 2f
        val currentBarHeight = barHeightPx(m, BarHeightPx, CollapsedBarHeightPx)
        val pillHeight = pillHeightPx(currentBarHeight, PillInsetPx)
        val restingBubble =
            bubbleGeometry(
                m = m,
                stretchFactor = 1f,
                bubbleSize = BubbleSizePx,
                bubbleOverhang = BubbleOverhangPx,
                restBarHeight = BarHeightPx,
                barHeight = currentBarHeight,
                pillHeight = pillHeight,
            )
        val centerX = clampNotchCenter(calmCenterSlot * slot, width)
        val morphed =
            morphedNotchParams(
                m = m,
                handoffM = HandoffM,
                bubbleCenterY = restingBubble.centerY,
                bubbleHalfHeight = restingBubble.halfHeight,
                gap = Gap,
                filletRadius = FilletRadius,
            )
        val notch = morphed?.let {
            filletedNotch(
                centerX = centerX,
                centerY = it.centerY,
                notchRadius = it.notchRadius,
                filletRadius = it.filletRadius,
                barWidth = width,
                cornerRadius = cornerRadius,
            )
        }
        return barPath(Size(width, currentBarHeight), cornerRadius, notch)
    }

    private fun restPath(target: Int, width: Float, m: Float): Path =
        framePath(target + 0.5f, width, m)

    private fun tipSamples(path: Path, width: Float, right: Boolean): Tip {
        val measure = PathMeasure()
        measure.setPath(path, false)
        val length = measure.length
        val all = (0 until SampleSteps).map { measure.getPosition(length * it / SampleSteps) }
        fun inTip(point: Offset) =
            if (right) point.x >= width - tipMarginPx else point.x <= tipMarginPx
        val segments =
            all.indices
                .map { all[it] to all[(it + 1) % all.size] }
                .filter { (start, end) -> inTip(start) || inTip(end) }
        return Tip(points = all.filter(::inTip), segments = segments)
    }

    private fun hausdorff(reference: Tip, sample: Tip): Float {
        if (reference.segments.isEmpty() || sample.points.isEmpty()) return 0f
        return sample.points.maxOf { point ->
            reference.segments.minOf { (start, end) -> distanceToSegment(point, start, end) }
        }
    }

    private fun distanceToSegment(point: Offset, start: Offset, end: Offset): Float {
        val segment = end - start
        val lengthSquared = segment.x * segment.x + segment.y * segment.y
        if (lengthSquared == 0f) return (point - start).getDistance()
        val t =
            (((point - start).x * segment.x + (point - start).y * segment.y) / lengthSquared)
                .coerceIn(0f, 1f)
        return (point - (start + segment * t)).getDistance()
    }

    private class Tip(val points: List<Offset>, val segments: List<Pair<Offset, Offset>>)

    private val BubbleSizePx = NavBarDimens.BubbleSize.value
    private val BubbleOverhangPx = NavBarDimens.BubbleOverhang.value
    private val BarHeightPx = NavBarDimens.BarHeight.value
    private val CollapsedBarHeightPx = NavBarDimens.CollapsedBarHeight.value
    private val PillInsetPx = NavBarDimens.PillInset.value
    private val FilletRadius = NavBarDimens.NotchFillet.value
    private val Gap = NavBarDimens.NotchGap.value
    private val HandoffM = NavBarDimens.BubbleHandoff
    private val SampleSteps = 2000
}
