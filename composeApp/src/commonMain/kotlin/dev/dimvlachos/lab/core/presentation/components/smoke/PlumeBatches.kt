package dev.dimvlachos.lab.core.presentation.components.smoke

/**
 * A plume's particles as they stand this frame, sorted into batches that are each drawn in one
 * call: one for each of the row's colours at each age, in two sizes, and one for the sparks at each
 * age. Ages are cut into [Bands] bands of a particle's life, closer together early on, where a
 * particle changes fastest. Each batch is a flat run of x, y pairs in px; the arrays grow as they
 * need to and are never shrunk, and what is past a batch's count lies off the screen, so filling
 * and drawing them allocates nothing once the plume has been drawn a few times, on either platform.
 */
internal class PlumeBatches {
    /** How many colours it was filled for: none until its plume's row has been sampled. */
    var colours = 0
        private set

    private var points: Array<FloatArray> = emptyArray()
    private var counts = IntArray(0)

    // How many points each batch held when it was last sealed: past them the array is already
    // off the screen.
    private var sealed = IntArray(0)

    /** Empties every batch, ready for [colourCount] colours. */
    fun clear(colourCount: Int) {
        if (colourCount != colours || points.isEmpty()) {
            colours = colourCount
            val n = colourCount * Bands * Sizes + Bands
            points = Array(n) { FloatArray(InitialPoints * 2).apply { fill(OffScreen) } }
            counts = IntArray(n)
            sealed = IntArray(n)
        } else counts.fill(0)
    }

    fun add(batch: Int, x: Float, y: Float) {
        var array = points[batch]
        val k = counts[batch] * 2
        if (k + 2 > array.size) {
            val grown = array.copyOf(array.size * 2)
            grown.fill(OffScreen, array.size)
            array = grown
            points[batch] = array
        }
        array[k] = x
        array[k + 1] = y
        counts[batch]++
    }

    /**
     * Puts every point past a batch's count off the screen, so a batch can be drawn whole, as Skia
     * draws it, without copying it to its length. Only what the last fill left there is moved, so
     * this costs next to nothing. A batch that holds far fewer points than it has room for gives
     * the room back, so drawing it whole never costs more than a few times its count.
     */
    fun seal() {
        for (b in counts.indices) {
            val count = counts[b]
            val array = points[b]
            if (array.size > InitialPoints * 2 && count * 2 * Shrink < array.size) {
                val kept = maxOf(InitialPoints, count * 2) * 2
                points[b] = array.copyOf(kept).apply { fill(OffScreen, count * 2) }
            } else if (count < sealed[b]) array.fill(OffScreen, count * 2, sealed[b] * 2)
            sealed[b] = count
        }
    }

    fun pointsOf(batch: Int): FloatArray = points[batch]

    fun countOf(batch: Int): Int = counts[batch]

    companion object {
        /** The batch for colour [colour] at age band [band], in size [size] of [Sizes]. */
        fun of(colour: Int, band: Int, size: Int = 0): Int = (colour * Bands + band) * Sizes + size

        /** The sparks' batch at age band [band], after [colours] colours' batches. */
        fun sparks(colours: Int, band: Int): Int = colours * Bands * Sizes + band

        /** Each colour's particles at each age are drawn in this many sizes. */
        const val Sizes = 2

        /** Which band [share] of a particle's life falls in. */
        fun bandOf(share: Float): Int {
            for (b in 1 until BandEdges.size) if (share < BandEdges[b]) return b - 1
            return Bands - 1
        }

        /** The share of a particle's life in the middle of band [band]. */
        fun middleOf(band: Int): Float = (BandEdges[band] + BandEdges[band + 1]) / 2f

        /** The share of a particle's life where band [band] starts. */
        fun startOf(band: Int): Float = BandEdges[band]

        /** The share of a particle's life where band [band] ends. */
        fun endOf(band: Int): Float = BandEdges[band + 1]

        private val BandEdges =
            floatArrayOf(
                0f,
                0.02f,
                0.045f,
                0.07f,
                0.1f,
                0.14f,
                0.19f,
                0.25f,
                0.32f,
                0.4f,
                0.49f,
                0.59f,
                0.7f,
                0.82f,
                1f,
            )
        val Bands = BandEdges.size - 1

        private const val InitialPoints = 64

        // A batch with room for this many times its points gives the room back.
        private const val Shrink = 4

        // Where a batch's unused points lie: just off the top left of the screen, where they draw
        // nothing however large a point is drawn, and near enough that a batch drawn whole keeps
        // bounds about the size of the screen.
        const val OffScreen = -1_000f
    }
}
