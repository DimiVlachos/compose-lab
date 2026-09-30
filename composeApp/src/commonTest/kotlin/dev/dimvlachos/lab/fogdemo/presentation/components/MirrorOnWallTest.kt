package dev.dimvlachos.lab.fogdemo.presentation.components

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class MirrorOnWallTest {
    private val photo = Size(1000f, 2000f)
    private val glass = Rect(0.1f, 0.2f, 0.9f, 0.6f)

    // The whole mirror, its frame around the glass.
    private val frame = Rect(0.05f, 0.15f, 0.95f, 0.65f)

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
        assertNear(
            Rect(50f, 200f, 450f, 600f),
            glassOnScreen(photo, Size(500f, 1000f), glass, frame),
        )
    }

    @Test
    fun onAWiderScreenTheWholeMirrorIsShownCentred() {
        // 500 wide: the photo is 1000 tall, its frame from 150 to 650; the screen shows 600 of it.
        val onScreen = glassOnScreen(photo, Size(500f, 600f), glass, frame)
        // Centred on the frame: the frame from 50 to 550, the glass from 100 to 500.
        assertNear(Rect(50f, 100f, 450f, 500f), onScreen)
    }

    @Test
    fun onANarrowerScreenTheSidesAreCroppedEvenlyAndTheGlassStopsAtTheEdges() {
        // 1000 tall: the photo is 500 wide, 100 of it cropped off each side, and the glass with it.
        val onScreen = glassOnScreen(photo, Size(300f, 1000f), glass, frame)
        assertNear(Rect(0f, 200f, 300f, 600f), onScreen)
        assertNear(
            Rect(25f, 200f, 425f, 600f),
            glassOnScreen(photo, Size(450f, 1000f), glass, frame),
        )
    }

    @Test
    fun onAScreenTooWideForTheWholeMirrorTheGlassIsKeptOnIt() {
        val onScreen = glassOnScreen(photo, Size(500f, 100f), glass, frame)
        assertTrue(onScreen.top == 0f && onScreen.bottom == 100f, "$onScreen")
    }
}
