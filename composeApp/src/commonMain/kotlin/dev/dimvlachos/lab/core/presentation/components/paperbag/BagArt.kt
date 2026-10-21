package dev.dimvlachos.lab.core.presentation.components.paperbag

/**
 * What a paper bag can hold, each drawn as the prototype draws it, standing on its base, and how
 * tall it stands in bag heights: [height] over 1 pokes out of the top.
 */
internal enum class BagArt(val height: Float) {
    Baguette(1.22f),
    Celery(1.08f),
    Apples(.42f),
    Sunflowers(1.14f),
    Milk(.7f),
    Eggs(.34f),
}

/** How many things a bag holds at most: the prototype's nine places across its mouth. */
internal const val BagCapacity = 9
