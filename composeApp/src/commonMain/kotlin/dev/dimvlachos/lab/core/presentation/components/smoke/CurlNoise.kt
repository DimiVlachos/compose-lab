package dev.dimvlachos.lab.core.presentation.components.smoke

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * A fixed, smooth 2D noise field and its curl: the swirl the smoke follows. The curl of a scalar
 * field has no divergence, so smoke following it curls and folds without bunching up or thinning
 * out. The noise is gradient noise with its derivatives worked out alongside it, so a curl costs
 * one look-up of four corners, not four look-ups. The same point always gives the same answer.
 */
internal object CurlNoise {
    /**
     * Writes the curl of the field at ([x], [y]), in cells, into [out] (x then y, at [at]). Each
     * component is at most about 2.
     */
    fun curl(x: Float, y: Float, out: FloatArray, at: Int = 0) {
        val fx = floor(x)
        val fy = floor(y)
        val ix = fx.toInt()
        val iy = fy.toInt()
        val tx = x - fx
        val ty = y - fy
        val ux = tx * tx * (3f - 2f * tx)
        val uy = ty * ty * (3f - 2f * ty)
        val dux = 6f * tx * (1f - tx)
        val duy = 6f * ty * (1f - ty)
        val a = corner(ix, iy)
        val b = corner(ix + 1, iy)
        val c = corner(ix, iy + 1)
        val d = corner(ix + 1, iy + 1)
        val gax = GradX[a]
        val gay = GradY[a]
        val gbx = GradX[b]
        val gby = GradY[b]
        val gcx = GradX[c]
        val gcy = GradY[c]
        val gdx = GradX[d]
        val gdy = GradY[d]
        val va = gax * tx + gay * ty
        val vb = gbx * (tx - 1f) + gby * ty
        val vc = gcx * tx + gcy * (ty - 1f)
        val vd = gdx * (tx - 1f) + gdy * (ty - 1f)
        val k = va - vb - vc + vd
        val dx =
            gax +
                ux * (gbx - gax) +
                uy * (gcx - gax) +
                ux * uy * (gax - gbx - gcx + gdx) +
                dux * (uy * k + vb - va)
        val dy =
            gay +
                ux * (gby - gay) +
                uy * (gcy - gay) +
                ux * uy * (gay - gby - gcy + gdy) +
                duy * (ux * k + vc - va)
        out[at] = dy
        out[at + 1] = -dx
    }

    /** The field itself at ([x], [y]), in cells: about -0.7 to 0.7. For tests. */
    fun value(x: Float, y: Float): Float {
        val fx = floor(x)
        val fy = floor(y)
        val ix = fx.toInt()
        val iy = fy.toInt()
        val tx = x - fx
        val ty = y - fy
        val ux = tx * tx * (3f - 2f * tx)
        val uy = ty * ty * (3f - 2f * ty)
        val a = corner(ix, iy)
        val b = corner(ix + 1, iy)
        val c = corner(ix, iy + 1)
        val d = corner(ix + 1, iy + 1)
        val va = GradX[a] * tx + GradY[a] * ty
        val vb = GradX[b] * (tx - 1f) + GradY[b] * ty
        val vc = GradX[c] * tx + GradY[c] * (ty - 1f)
        val vd = GradX[d] * (tx - 1f) + GradY[d] * (ty - 1f)
        return va + ux * (vb - va) + uy * (vc - va) + ux * uy * (va - vb - vc + vd)
    }

    // Which of the gradients a lattice corner has: a hash of its place, the same every time.
    private fun corner(x: Int, y: Int): Int {
        var h = x * 374_761_393 + y * 668_265_263
        h = (h xor (h ushr 13)) * 1_274_126_177
        h = h xor (h ushr 16)
        return h and (Directions - 1)
    }

    private const val Directions = 16
    private val GradX = FloatArray(Directions) { cos(2.0 * PI * it / Directions).toFloat() }
    private val GradY = FloatArray(Directions) { sin(2.0 * PI * it / Directions).toFloat() }
}
