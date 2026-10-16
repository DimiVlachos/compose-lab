package dev.dimvlachos.lab.core.presentation.components.snowglobe

import kotlin.math.abs

/**
 * The little scene inside the globe: a lighthouse and two shingled cottages on a dune, in globe
 * units (the centre at 0,0, the sphere's radius 1, y down). The same numbers draw it and tell the
 * glitter where it can come to rest, so gold settles on the roofs it sees.
 */
internal object Figurine {
    /** The lighthouse: a tapering tower, a gallery ledge, the lantern room and its cap. */
    object Lighthouse {
        const val TowerBaseLeft = -0.40f
        const val TowerBaseRight = -0.20f
        const val TowerTopLeft = -0.37f
        const val TowerTopRight = -0.23f
        const val galleryLeft = -0.42f
        const val galleryRight = -0.18f
        const val galleryTop = -0.30f
        const val LanternLeft = -0.36f
        const val LanternRight = -0.24f
        const val LanternTop = -0.42f
        const val CapLeft = -0.33f
        const val CapRight = -0.27f
        const val CapTop = -0.50f
    }

    /** A cottage: walls from [left] to [right] up to [eaves], a gable roof up to [ridge]. */
    class Cottage(val left: Float, val right: Float, val eaves: Float, val ridge: Float) {
        val centre: Float
            get() = (left + right) / 2f

        fun roofAt(x: Float): Float {
            val half = (right - left) / 2f
            return ridge + (eaves - ridge) * (abs(x - centre) / half)
        }
    }

    /** The taller cottage, with the weathervane on its ridge. */
    val CottageA = Cottage(left = 0.02f, right = 0.36f, eaves = 0.18f, ridge = 0.00f)
    val CottageB = Cottage(left = 0.40f, right = 0.66f, eaves = 0.24f, ridge = 0.10f)

    /** The weathervane's pivot, on Cottage A's ridge, and how tall its post is. */
    const val VaneX = 0.19f
    const val VanePostTop = -0.14f

    /** The dune: highest in the middle, falling away to the glass. */
    fun duneTop(x: Float): Float = 0.40f + 0.16f * x * x

    /** The topmost surface under [x], as a y: the smaller, the higher. */
    fun restHeight(x: Float): Float {
        var top = duneTop(x)
        with(Lighthouse) {
            if (x in CapLeft..CapRight) top = minOf(top, CapTop)
            if (x in LanternLeft..LanternRight) top = minOf(top, LanternTop)
            if (x in galleryLeft..galleryRight) top = minOf(top, galleryTop)
        }
        for (c in listOf(CottageA, CottageB)) {
            if (x in c.left..c.right) top = minOf(top, c.roofAt(x))
        }
        return top
    }
}
