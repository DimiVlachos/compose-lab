package dev.dimvlachos.moodboard.morph

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MorphOverlayClipTest {
    private val bounds = Rect(0f, 0f, 100f, 100f)
    private val density = Density(1f)

    // True when the clip leaves nothing of [bounds] uncovered, i.e. it is the plain rectangle.
    private fun coversTheWholeRect(clip: Path): Boolean {
        val rect = Path().apply { addRect(bounds) }
        return Path.combine(PathOperation.Difference, rect, clip).isEmpty
    }

    @Test
    fun zeroRadiusClipsToTheWholeBounds() {
        val clip = RoundedOverlayClip({ 0.dp })
        assertTrue(coversTheWholeRect(clip.clipPath(bounds, density)))
    }

    @Test
    fun radiusIsReadOnEveryCall() {
        var radius: Dp = 0.dp
        val clip = RoundedOverlayClip({ radius })
        assertTrue(coversTheWholeRect(clip.clipPath(bounds, density)))
        radius = 16.dp
        // The corners are now cut away, so the clip no longer covers the rectangle.
        assertFalse(coversTheWholeRect(clip.clipPath(bounds, density)))
    }
}
