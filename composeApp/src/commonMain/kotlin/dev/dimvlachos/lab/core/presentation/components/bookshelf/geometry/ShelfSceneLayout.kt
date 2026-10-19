package dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry

private const val RING_HERO = 0.45f

// The planks are laid out in the band they were tuned in, 412 by 640, scaled to the width.
private const val PLANKS_ASPECT = 640f / 412f

/**
 * One scene for the whole shelf: the ring across the top [RING_HERO] of the viewport, and below it
 * two oak planks, which scroll into view. Both stand against one wall, under one light.
 */
internal class ShelfSceneLayout
private constructor(
    val width: Float,
    val viewportHeight: Float,
    val ring: ShelfRing,
    val planks: ShelfLayout,
) {
    val ringTop: Float
        get() = 0f

    val ringBottom: Float
        get() = ring.height

    val totalHeight: Float
        get() = ringBottom + planks.height

    companion object {
        fun of(
            width: Float,
            viewportHeight: Float,
            ring: List<ShelfBookSpec>,
            planks: List<List<ShelfBookSpec>>,
        ): ShelfSceneLayout {
            val ringHeight = viewportHeight * RING_HERO
            val planksHeight = width * PLANKS_ASPECT
            val shelf =
                layoutShelf(width, planksHeight, planks)
                    .onStage(0f, ringHeight, width, planksHeight)
            return ShelfSceneLayout(
                width,
                viewportHeight,
                ShelfRing(width, ringHeight, ring),
                shelf,
            )
        }
    }
}
