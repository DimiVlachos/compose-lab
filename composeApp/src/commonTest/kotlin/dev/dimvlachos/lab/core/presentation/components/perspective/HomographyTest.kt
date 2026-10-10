package dev.dimvlachos.lab.core.presentation.components.perspective

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class HomographyTest {
    private val h = FloatArray(9)

    @Test
    fun aFlatPlaneAtZeroDepthIsATranslation() {
        Homography.of(10f, 20f, 0f, 1f, 0f, 0f, 0f, 1f, 0f, 100f, 100f, 800f, h)
        assertOffset(Offset(13f, 27f), Homography.project(h, 3f, 7f))
    }

    @Test
    fun aPointFurtherAwayIsDrawnNearerTheCentre() {
        // P / (P + z) = 800 / 1600 = 0.5: 200 px right of centre lands 100 px right.
        Homography.of(300f, 100f, 800f, 1f, 0f, 0f, 0f, 1f, 0f, 100f, 100f, 800f, h)
        assertOffset(Offset(200f, 100f), Homography.project(h, 0f, 0f))
    }

    @Test
    fun unprojectUndoesProject() {
        Homography.of(40f, 60f, 120f, 0.9f, 0.1f, 0.4f, -0.2f, 0.8f, 0.5f, 180f, 240f, 1100f, h)
        val seen = Homography.project(h, 37f, 52f)
        assertOffset(Offset(37f, 52f), assertNotNull(Homography.unproject(h, seen)))
    }

    @Test
    fun theMatrixMapsLikeProject() {
        Homography.of(40f, 60f, 120f, 0.9f, 0.1f, 0.4f, -0.2f, 0.8f, 0.5f, 180f, 240f, 1100f, h)
        val m = Homography.toMatrix(h, Matrix())
        assertOffset(Homography.project(h, 37f, 52f), m.map(Offset(37f, 52f)))
    }

    private fun assertOffset(expected: Offset, actual: Offset) {
        assertEquals(expected.x, actual.x, 0.01f)
        assertEquals(expected.y, actual.y, 0.01f)
    }
}
