package dev.dimvlachos.lab.core.presentation.components.navbar

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BarGeometryTest {
    @Test
    fun barWidthInterpolatesFromFullToCollapsed() {
        assertEquals(1000, barWidth(fullWidth = 1000, collapsedWidth = 256, collapse = 0f))
        assertEquals(628, barWidth(fullWidth = 1000, collapsedWidth = 256, collapse = 0.5f))
        assertEquals(256, barWidth(fullWidth = 1000, collapsedWidth = 256, collapse = 1f))
    }

    @Test
    fun barNeverGrowsWhenTheFullWidthIsAlreadyNarrow() {
        assertEquals(200, barWidth(fullWidth = 200, collapsedWidth = 256, collapse = 1f))
    }

    @Test
    fun notchCenterStaysInsideTheBar() {
        assertEquals(0f, clampNotchCenter(centerX = -12f, barWidth = 400f))
        assertEquals(200f, clampNotchCenter(centerX = 200f, barWidth = 400f))
        assertEquals(400f, clampNotchCenter(centerX = 430f, barWidth = 400f))
    }

    @Test
    fun filletCentersSitAtNotchRadiusPlusFilletRadiusFromTheNotchCenter() {
        val notch = straightSegmentNotch()
        assertEquals(48f, (notch.leftFilletCenter - notch.notchCenter).getDistance(), 0.01f)
        assertEquals(48f, (notch.rightFilletCenter - notch.notchCenter).getDistance(), 0.01f)
    }

    @Test
    fun filletCentersSitAtYEqualsFilletRadiusInTheStraightSegment() {
        val notch = straightSegmentNotch()
        assertEquals(16f, notch.leftFilletCenter.y, 0.01f)
        assertEquals(16f, notch.rightFilletCenter.y, 0.01f)
    }

    @Test
    fun straightSegmentFilletsAreStillTangentToTheTopEdge() {
        val notch = straightSegmentNotch()
        assertEquals(0f, notch.leftOuterTangent.y)
        assertEquals(0f, notch.rightOuterTangent.y)
    }

    @Test
    fun notchTangentPointsLieOnBothCircles() {
        val notch = straightSegmentNotch()
        assertEquals(32f, (notch.leftNotchTangent - notch.notchCenter).getDistance(), 0.01f)
        assertEquals(16f, (notch.leftNotchTangent - notch.leftFilletCenter).getDistance(), 0.01f)
        assertEquals(32f, (notch.rightNotchTangent - notch.notchCenter).getDistance(), 0.01f)
        assertEquals(16f, (notch.rightNotchTangent - notch.rightFilletCenter).getDistance(), 0.01f)
    }

    @Test
    fun theShapeIsSymmetricAboutTheNotchCenterX() {
        val notch = straightSegmentNotch()
        val cx = notch.notchCenter.x
        assertEquals(cx - notch.leftFilletCenter.x, notch.rightFilletCenter.x - cx, 0.01f)
        assertEquals(cx - notch.leftNotchTangent.x, notch.rightNotchTangent.x - cx, 0.01f)
        assertEquals(notch.leftFilletCenter.y, notch.rightFilletCenter.y, 0.01f)
        assertEquals(notch.leftNotchTangent.y, notch.rightNotchTangent.y, 0.01f)
    }

    @Test
    fun barBoundsAreUnchangedByTheNotch() {
        val size = Size(400f, 72f)
        val notch = straightSegmentNotch()
        val bounds = barPath(size, cornerRadius = 36f, notch = notch).getBounds()
        assertEquals(Rect(Offset.Zero, size), bounds)
    }

    @Test
    fun aSingleLargeSweepArcStaysOnItsCircle() {
        val center = Offset(200f, 6f)
        val radius = 32f
        val path =
            Path().apply { arcTo(Rect(center, radius), 167.9753f, -155.95056f, forceMoveTo = true) }
        for (point in samplePath(path)) {
            assertEquals(radius, (point - center).getDistance(), 0.05f)
        }
    }

    @Test
    fun cutterPathCurveSpansTheNotchAndClosesAboveTheBar() {
        val notch = straightSegmentNotch()
        val samples = samplePath(notchCutterPath(notch))
        assertEquals(6f + 32f, samples.maxOf { it.y }, 0.05f)
        assertEquals(notch.leftOuterTangent.x, samples.minOf { it.x }, 0.05f)
        assertEquals(notch.rightOuterTangent.x, samples.maxOf { it.x }, 0.05f)
        assertEquals(-(32f + 16f), samples.minOf { it.y }, 0.05f)
    }

    @Test
    fun outerFilletIsCapTangentAtAnEdgeTab() {
        val notch =
            filletedNotch(
                centerX = 32f,
                centerY = 6f,
                notchRadius = 32f,
                filletRadius = 16f,
                barWidth = 256f,
                cornerRadius = 36f,
            )
        val capCenter = Offset(36f, 36f)
        assertEquals(36f - 16f, (notch.leftFilletCenter - capCenter).getDistance(), 0.01f)
        assertEquals(32f + 16f, (notch.leftFilletCenter - notch.notchCenter).getDistance(), 0.01f)
        assertEquals(36f, (notch.leftOuterTangent - capCenter).getDistance(), 0.01f)
    }

    @Test
    fun notchArcStillDipsThroughTheBottomWhenTheFilletIsSmallerThanCenterY() {
        val notch =
            filletedNotch(
                centerX = 200f,
                centerY = 16f,
                notchRadius = 32f,
                filletRadius = 6f,
                barWidth = 400f,
                cornerRadius = 36f,
            )
        val samples = samplePath(notchCutterPath(notch))
        assertEquals(16f + 32f, samples.maxOf { it.y }, 0.05f)
    }

    @Test
    fun aCapTangentFilletNeverExtendsPastTheBarsOuterEdge() {
        // Internal tangency (|F - C| = cornerRadius - filletRadius) puts every point of the
        // fillet circle within cornerRadius of C by the triangle inequality, so the fillet can
        // never poke outside the cap circle it is nested inside, whatever the exact numbers are.
        val notch =
            filletedNotch(
                centerX = 32f,
                centerY = 6f,
                notchRadius = 32f,
                filletRadius = 16f,
                barWidth = 256f,
                cornerRadius = 36f,
            )
        assertTrue(notch.leftFilletCenter.x - notch.filletRadius >= 0f)
    }

    @Test
    fun edgeTabAtRealNavBarWidthStaysCapTangentAndTheSilhouetteIntrusionIsExact() {
        val size = Size(371f, 72f)
        val notch =
            filletedNotch(
                centerX = 46.5f,
                centerY = 6f,
                notchRadius = 32f,
                filletRadius = 16f,
                barWidth = size.width,
                cornerRadius = 36f,
            )
        assertTrue(notch.leftIsCapTangent)
        val leftmost = notch.leftFilletCenter.x - notch.filletRadius
        assertTrue(leftmost >= 0f)
        assertEquals(1.93f, leftmost, 0.05f)
    }

    @Test
    fun mirroredEdgeTabAtRealNavBarWidthStaysCapTangentAndTheSilhouetteIntrusionIsExact() {
        val size = Size(371f, 72f)
        val notch =
            filletedNotch(
                centerX = size.width - 46.5f,
                centerY = 6f,
                notchRadius = 32f,
                filletRadius = 16f,
                barWidth = size.width,
                cornerRadius = 36f,
            )
        assertTrue(notch.rightIsCapTangent)
        val rightmost = notch.rightFilletCenter.x + notch.filletRadius
        assertTrue(rightmost <= size.width)
        assertEquals(size.width - 1.93f, rightmost, 0.05f)
    }

    @Test
    fun filletRadiusMustBePositive() {
        assertFailsWith<IllegalArgumentException> {
            filletedNotch(
                centerX = 200f,
                centerY = 6f,
                notchRadius = 32f,
                filletRadius = 0f,
                barWidth = 400f,
                cornerRadius = 36f,
            )
        }
    }

    @Test
    fun morphedNotchParamsAtRestReturnsTheRestValues() {
        val morphed =
            morphedNotchParams(m = 0f, restCenterY = 6f, notchRadius = 32f, filletRadius = 16f)
        assertNotNull(morphed)
        assertEquals(32f, morphed.notchRadius)
        assertEquals(16f, morphed.filletRadius)
        assertEquals(6f, morphed.centerY)
    }

    @Test
    fun morphedNotchParamsIsNullAtFullMorph() {
        assertNull(
            morphedNotchParams(m = 1f, restCenterY = 6f, notchRadius = 32f, filletRadius = 16f)
        )
    }

    @Test
    fun morphedNotchParamsDepthIsMonotonicallyDecreasingAndReachesZero() {
        val restCenterY = 6f
        val notchRadius = 32f
        val filletRadius = 16f
        var previousDepth = Float.MAX_VALUE
        var becameNullAtStep: Int? = null
        for (step in 0..1000) {
            val m = step / 1000f
            val morphed = morphedNotchParams(m, restCenterY, notchRadius, filletRadius)
            if (morphed == null) {
                if (becameNullAtStep == null) becameNullAtStep = step
                continue
            }
            assertNull(becameNullAtStep, "must not un-morph after the notch has gone")
            val depth = morphed.centerY + morphed.notchRadius
            assertTrue(depth <= previousDepth + 0.0001f, "depth must not increase as m grows")
            previousDepth = depth
        }
        assertNotNull(becameNullAtStep, "the notch must fully close before m reaches 1")
    }

    @Test
    fun filletedNotchNeverThrowsThroughTheMorphAtRealBarWidthsAndTabCentres() {
        val restCenterY = 6f
        val notchRadius = 32f
        val filletRadius = 16f
        val cornerRadius = 36f
        val itemCount = 4
        for (barWidthPx in listOf(256f, 300f, 371f)) {
            val slot = barWidthPx / itemCount
            val tabCenters = listOf(0.5f * slot, 2f * slot, 3.5f * slot)
            for (centerX in tabCenters) {
                for (step in 0..200) {
                    val m = step / 200f
                    val morphed =
                        morphedNotchParams(m, restCenterY, notchRadius, filletRadius) ?: continue
                    filletedNotch(
                        centerX = clampNotchCenter(centerX, barWidthPx),
                        centerY = morphed.centerY,
                        notchRadius = morphed.notchRadius,
                        filletRadius = morphed.filletRadius,
                        barWidth = barWidthPx,
                        cornerRadius = cornerRadius,
                    )
                }
            }
        }
    }

    private fun straightSegmentNotch() =
        filletedNotch(
            centerX = 200f,
            centerY = 6f,
            notchRadius = 32f,
            filletRadius = 16f,
            barWidth = 400f,
            cornerRadius = 36f,
        )

    private fun samplePath(path: Path, steps: Int = 500): List<Offset> {
        val measure = PathMeasure()
        measure.setPath(path, false)
        val length = measure.length
        return (0..steps).map { step -> measure.getPosition(length * step / steps) }
    }
}
