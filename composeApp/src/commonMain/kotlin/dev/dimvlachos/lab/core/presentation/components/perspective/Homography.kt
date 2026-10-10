package dev.dimvlachos.lab.core.presentation.components.perspective

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import kotlin.math.abs

/**
 * How a flat plane in 3D is seen on the screen: one projection, a 3 x 3 written row by row into a
 * float array, exact for every point of the plane. A plane is drawn with it by drawing its flat
 * content under [toMatrix].
 */
internal object Homography {
    /**
     * Writes into [out] the projection of the plane origin + u · (ux, uy, uz) + v · (vx, vy, vz),
     * all in px with z away from the eye, seen from [perspective] px in front of the screen and
     * centred on ([cx], [cy]): a point at depth z is drawn P / (P + z) as far from the centre.
     */
    fun of(
        ox: Float,
        oy: Float,
        oz: Float,
        ux: Float,
        uy: Float,
        uz: Float,
        vx: Float,
        vy: Float,
        vz: Float,
        cx: Float,
        cy: Float,
        perspective: Float,
        out: FloatArray,
    ): FloatArray {
        val w0 = uz / perspective
        val w1 = vz / perspective
        val w2 = 1f + oz / perspective
        out[0] = ux + cx * w0
        out[1] = vx + cx * w1
        out[2] = ox - cx + cx * w2
        out[3] = uy + cy * w0
        out[4] = vy + cy * w1
        out[5] = oy - cy + cy * w2
        out[6] = w0
        out[7] = w1
        out[8] = w2
        return out
    }

    /** Writes [h] into [out] as the 4 x 4 Compose draws with, and returns it. */
    fun toMatrix(h: FloatArray, out: Matrix): Matrix {
        out.reset()
        out[0, 0] = h[0]
        out[1, 0] = h[1]
        out[3, 0] = h[2]
        out[0, 1] = h[3]
        out[1, 1] = h[4]
        out[3, 1] = h[5]
        out[0, 3] = h[6]
        out[1, 3] = h[7]
        out[3, 3] = h[8]
        return out
    }

    /** Where [h] draws the plane's point ([u], [v]). */
    fun project(h: FloatArray, u: Float, v: Float): Offset {
        val w = h[6] * u + h[7] * v + h[8]
        return Offset((h[0] * u + h[1] * v + h[2]) / w, (h[3] * u + h[4] * v + h[5]) / w)
    }

    /** The plane's point seen at [point], or null where the plane is seen edge on. */
    fun unproject(h: FloatArray, point: Offset): Offset? {
        // The inverse of a projection, up to scale: its adjugate.
        val a0 = h[4] * h[8] - h[5] * h[7]
        val a1 = h[2] * h[7] - h[1] * h[8]
        val a2 = h[1] * h[5] - h[2] * h[4]
        val a3 = h[5] * h[6] - h[3] * h[8]
        val a4 = h[0] * h[8] - h[2] * h[6]
        val a5 = h[2] * h[3] - h[0] * h[5]
        val a6 = h[3] * h[7] - h[4] * h[6]
        val a7 = h[1] * h[6] - h[0] * h[7]
        val a8 = h[0] * h[4] - h[1] * h[3]
        val w = a6 * point.x + a7 * point.y + a8
        if (abs(w) < 1e-9f) return null
        return Offset(
            (a0 * point.x + a1 * point.y + a2) / w,
            (a3 * point.x + a4 * point.y + a5) / w,
        )
    }
}
