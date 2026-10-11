package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals

class CircularRevealTest {
    private val screen = Size(400f, 800f)

    @Test
    fun fromACornerTheCircleMustReachTheOppositeOne() {
        assertEquals(hypot(400f, 800f), farthestCorner(Offset.Zero, screen), 1e-3f)
        assertEquals(hypot(400f, 800f), farthestCorner(Offset(400f, 800f), screen), 1e-3f)
    }

    @Test
    fun fromTheMiddleItReachesEveryCornerAtOnce() {
        assertEquals(hypot(200f, 400f), farthestCorner(Offset(200f, 400f), screen), 1e-3f)
    }

    @Test
    fun fromAButtonNearTheTopRightItReachesTheFootOnTheLeft() {
        val button = Offset(370f, 40f)
        assertEquals(hypot(370f, 760f), farthestCorner(button, screen), 1e-3f)
    }
}
