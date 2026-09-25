package dev.dimvlachos.lab.core.presentation.components.navbar

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlin.test.Test
import kotlin.test.assertEquals

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
        val notch =
            filletedNotch(centerX = 200f, centerY = 6f, notchRadius = 32f, filletRadius = 10f)
        assertEquals(42f, (notch.leftFilletCenter - notch.notchCenter).getDistance(), 0.01f)
        assertEquals(42f, (notch.rightFilletCenter - notch.notchCenter).getDistance(), 0.01f)
    }

    @Test
    fun filletCentersSitAtYEqualsFilletRadius() {
        val notch =
            filletedNotch(centerX = 200f, centerY = 6f, notchRadius = 32f, filletRadius = 10f)
        assertEquals(10f, notch.leftFilletCenter.y, 0.01f)
        assertEquals(10f, notch.rightFilletCenter.y, 0.01f)
    }

    @Test
    fun notchTangentPointsLieOnBothCircles() {
        val notch =
            filletedNotch(centerX = 200f, centerY = 6f, notchRadius = 32f, filletRadius = 10f)
        assertEquals(
            32f,
            (notch.leftNotchTangent - notch.notchCenter).getDistance(),
            0.01f,
        )
        assertEquals(
            10f,
            (notch.leftNotchTangent - notch.leftFilletCenter).getDistance(),
            0.01f,
        )
        assertEquals(
            32f,
            (notch.rightNotchTangent - notch.notchCenter).getDistance(),
            0.01f,
        )
        assertEquals(
            10f,
            (notch.rightNotchTangent - notch.rightFilletCenter).getDistance(),
            0.01f,
        )
    }

    @Test
    fun theShapeIsSymmetricAboutTheNotchCenterX() {
        val notch =
            filletedNotch(centerX = 200f, centerY = 6f, notchRadius = 32f, filletRadius = 10f)
        val cx = notch.notchCenter.x
        assertEquals(
            cx - notch.leftFilletCenter.x,
            notch.rightFilletCenter.x - cx,
            0.01f,
        )
        assertEquals(
            cx - notch.leftNotchTangent.x,
            notch.rightNotchTangent.x - cx,
            0.01f,
        )
        assertEquals(notch.leftFilletCenter.y, notch.rightFilletCenter.y, 0.01f)
        assertEquals(notch.leftNotchTangent.y, notch.rightNotchTangent.y, 0.01f)
    }

    @Test
    fun barBoundsAreUnchangedByTheNotch() {
        val size = Size(400f, 72f)
        val notch =
            filletedNotch(centerX = 200f, centerY = 6f, notchRadius = 32f, filletRadius = 10f)
        val bounds = barPath(size, cornerRadius = 36f, notch = notch).getBounds()
        assertEquals(Rect(Offset.Zero, size), bounds)
    }
}
