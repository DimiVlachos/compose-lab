package dev.dimvlachos.lab.core.presentation.components.smoke

/**
 * The line a row burns along, from its right edge ([fromRight]) or its left. The row is
 * [SmokeDimens.Columns] columns across, and as the front passes a column it is stamped with the
 * moment it was passed: every particle of that column has been smoke since then, so the front is
 * all a particle needs to know how old it is. The front only ever moves on; a row drawn back in
 * runs its particles' ages back instead, so the smoke retraces its own path.
 */
internal class BurnFront(val fromRight: Boolean) {
    private val crossed = FloatArray(SmokeDimens.Columns) { Uncrossed }

    // How far the row had moved from where it began to burn when each column was crossed, in px,
    // and which of its rows of pixels showed then, from its top: a column's smoke leaves from where
    // it was, and only from what could be seen of it.
    private val movedX = FloatArray(SmokeDimens.Columns)
    private val movedY = FloatArray(SmokeDimens.Columns)
    private val shownTop = FloatArray(SmokeDimens.Columns) { Float.NEGATIVE_INFINITY }
    private val shownBottom = FloatArray(SmokeDimens.Columns) { Float.POSITIVE_INFINITY }

    /** How much of the row has burnt, 0 none to 1 all, from the edge it burns from. */
    var burnt: Float = 0f
        private set

    /** When the front last crossed a column; never, at first. */
    var lastCross: Float = Float.NEGATIVE_INFINITY
        private set

    /** Whether the whole row has burnt. */
    val done: Boolean
        get() = burnt >= 1f

    /** Where the front stands across the row, as a fraction of its width from its left edge. */
    val edge: Float
        get() = if (fromRight) 1f - burnt else burnt

    /**
     * Moves the front on to [to] of the row at [now], [seconds] after it was last moved. A column
     * it passes is stamped with when the front passed it, between then and now in proportion to
     * where it lies, so a fast front still crosses its columns one after another, not all at once;
     * and with where the row is now, [movedX] and [movedY] px from where it began to burn, and the
     * part of it that shows, [shownTop] to [shownBottom] px down it. A front never moves back.
     */
    fun advanceTo(
        to: Float,
        now: Float,
        seconds: Float,
        movedX: Float = 0f,
        movedY: Float = 0f,
        shownTop: Float = Float.NEGATIVE_INFINITY,
        shownBottom: Float = Float.POSITIVE_INFINITY,
    ) {
        val from = burnt
        val target = to.coerceAtMost(1f)
        if (target <= from) return
        for (c in crossed.indices) {
            if (crossed[c] != Uncrossed) continue
            val d = distanceOf(c)
            if (d < target) {
                val share = ((target - d) / (target - from)).coerceIn(0f, 1f)
                crossed[c] = now - seconds * share
                this.movedX[c] = movedX
                this.movedY[c] = movedY
                this.shownTop[c] = shownTop
                this.shownBottom[c] = shownBottom
            }
        }
        burnt = target
        lastCross = now
    }

    /** When column [column] was crossed, or [Uncrossed] if it hasn't been. */
    fun crossing(column: Int): Float = crossed[column]

    /** How far across the row had moved from where it began to burn when [column] was crossed. */
    fun movedXOf(column: Int): Float = movedX[column]

    /** How far down the row had moved from where it began to burn when [column] was crossed. */
    fun movedYOf(column: Int): Float = movedY[column]

    /**
     * Whether [y] px down the row showed when [column] was crossed: what didn't, makes no smoke.
     */
    fun showed(column: Int, y: Float): Boolean = y >= shownTop[column] && y <= shownBottom[column]

    // How far column [c]'s middle lies from the edge the row burns from, as a fraction of its
    // width.
    private fun distanceOf(c: Int): Float {
        val middle = (c + 0.5f) / SmokeDimens.Columns
        return if (fromRight) 1f - middle else middle
    }

    companion object {
        /** A column not yet crossed. */
        const val Uncrossed: Float = Float.MAX_VALUE
    }
}
