package dev.dimvlachos.lab.fogdemo.presentation.components

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class MirrorOnWallTest {
    private val photo = Size(1000f, 2000f)
    private val glass = Rect(0.1f, 0f, 0.9f, 0.5f)

    private fun assertNear(expected: Rect, actual: Rect) {
        val near =
            listOf(
                    expected.left to actual.left,
                    expected.top to actual.top,
                    expected.right to actual.right,
                    expected.bottom to actual.bottom,
                )
                .all { (e, a) -> abs(e - a) < 0.5f }
        assertTrue(near, "expected $expected, got $actual")
    }

    @Test
    fun onAScreenThePhotosShapeTheGlassIsWhereItIsInThePhoto() {
        assertNear(Rect(50f, 0f, 450f, 500f), glassOnScreen(photo, Size(500f, 1000f), glass))
    }

    @Test
    fun onAWiderScreenTheGlassEndsWellDownItRatherThanNearTheTop() {
        // 500 wide: the photo is 1000 tall, its glass ending at 500; the screen shows 400 of it.
        val screen = Size(500f, 400f)
        val onScreen = glassOnScreen(photo, screen, glass)
        assertNear(Rect(50f, 0f, 450f, screen.height * GlassEndsAt), onScreen)
    }

    @Test
    fun onANarrowerScreenTheSidesAreCroppedEvenlyAndTheGlassStopsAtTheEdges() {
        // 1000 tall: the photo is 500 wide, 100 of it cropped off each side, and the glass with it.
        val onScreen = glassOnScreen(photo, Size(300f, 1000f), glass)
        assertNear(Rect(0f, 0f, 300f, 500f), onScreen)
        assertNear(Rect(25f, 0f, 425f, 500f), glassOnScreen(photo, Size(450f, 1000f), glass))
    }

    @Test
    fun theGlassNeverStartsAboveTheScreen() {
        val onScreen = glassOnScreen(photo, Size(500f, 200f), glass)
        assertTrue(onScreen.top == 0f, "$onScreen")
        assertTrue(onScreen.bottom <= 200f, "$onScreen")
    }
}
