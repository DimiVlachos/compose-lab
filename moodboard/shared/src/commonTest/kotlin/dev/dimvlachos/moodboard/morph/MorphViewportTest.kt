package dev.dimvlachos.moodboard.morph

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MorphViewportTest {
    // A grid seen between a top bar ending at 100 and a bottom bar starting at 900.
    private val top = 100f
    private val bottom = 900f

    @Test
    fun aPhotoUnderTheBottomBarIsCutAtTheBar() {
        val photo = Rect(0f, 800f, 300f, 1100f)
        assertEquals(Rect(0f, 800f, 300f, 900f), viewportClipRect(photo, top, bottom, 1f))
    }

    @Test
    fun aPhotoUnderTheTopBarIsCutAtTheBar() {
        val photo = Rect(0f, 0f, 300f, 300f)
        assertEquals(Rect(0f, 100f, 300f, 300f), viewportClipRect(photo, top, bottom, 1f))
    }

    @Test
    fun halfWayTheCutIsHalfWayFromThePhotosOwnEdge() {
        // The full-screen end starts unclipped instead of losing its strips on the first frame.
        val photo = Rect(0f, 800f, 300f, 1100f)
        assertEquals(Rect(0f, 800f, 300f, 1000f), viewportClipRect(photo, top, bottom, 0.5f))
    }

    @Test
    fun noCutWhenThePhotoIsInsideTheViewport() {
        assertNull(viewportClipRect(Rect(0f, 200f, 300f, 500f), top, bottom, 1f))
    }

    @Test
    fun noCutAtFractionZeroOrBeforeTheGridIsMeasured() {
        val photo = Rect(0f, 800f, 300f, 1100f)
        assertNull(viewportClipRect(photo, top, bottom, 0f))
        assertNull(viewportClipRect(photo, Float.NaN, Float.NaN, 1f))
    }

    @Test
    fun theOverlayClipStopsAtTheBottomBar() {
        val photo = Rect(0f, 800f, 300f, 1100f)
        val clip = RoundedOverlayClip({ 0.dp }) { viewportClipRect(it, top, bottom, 1f) }
        val path = clip.clipPath(photo, Density(1f))
        val underTheBar = Path().apply { addRect(Rect(0f, 900f, 300f, 1100f)) }
        assertTrue(Path.combine(PathOperation.Intersect, path, underTheBar).isEmpty)
    }
}
