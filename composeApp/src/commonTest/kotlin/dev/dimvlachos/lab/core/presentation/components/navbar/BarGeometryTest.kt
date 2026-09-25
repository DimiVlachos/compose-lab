package dev.dimvlachos.lab.core.presentation.components.navbar

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
    fun cornerRadiusBecomesAPillWhenCollapsed() {
        assertEquals(24f, barCornerRadius(collapse = 0f, barHeight = 72f, expandedRadius = 24f))
        assertEquals(30f, barCornerRadius(collapse = 0.5f, barHeight = 72f, expandedRadius = 24f))
        assertEquals(36f, barCornerRadius(collapse = 1f, barHeight = 72f, expandedRadius = 24f))
    }

    @Test
    fun notchCenterStaysInsideTheBar() {
        assertEquals(0f, clampNotchCenter(centerX = -12f, barWidth = 400f))
        assertEquals(200f, clampNotchCenter(centerX = 200f, barWidth = 400f))
        assertEquals(400f, clampNotchCenter(centerX = 430f, barWidth = 400f))
    }
}
