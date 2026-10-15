package dev.dimvlachos.lab.core.presentation.components.smoke

/**
 * The line a row burns along, from its right edge ([fromRight]) or its left. The row is
 * [SmokeDimens.Columns] columns across, and every column has an age: how long its particles have
 * been smoke, which is all a particle needs to know where it is. A column the front passes starts
 * to age, a second a second; one the front draws back off, as a finger goes back, is wound back
 * instead, faster than it rose, until its smoke has landed in the row again and it is whole. Each
 * column's age is a straight line in time from the moment it last turned, so it can be read at any
 * moment, and a column the front comes back over carries on from wherever its smoke is.
 */
internal class BurnFront(val fromRight: Boolean) {
    // Each column's age is [ageRef] at [timeRef], rising one a second while it burns ([rate] 1), or
    // falling at -[rate] while it is wound back, to nothing; [timeRef] is [Uncrossed] while it is
    // whole. [woundFrom] is how old it was as its winding back began.
    private val ageRef = FloatArray(SmokeDimens.Columns)
    private val timeRef = FloatArray(SmokeDimens.Columns) { Uncrossed }
    private val rate = FloatArray(SmokeDimens.Columns)
    private val woundFrom = FloatArray(SmokeDimens.Columns)

    // How far the row had moved from where it began to burn when each column was crossed, in px,
    // and which of its rows of pixels showed then, from its top: a column's smoke leaves from where
    // it was, and only from what could be seen of it.
    private val movedX = FloatArray(SmokeDimens.Columns)
    private val movedY = FloatArray(SmokeDimens.Columns)
    private val shownTop = FloatArray(SmokeDimens.Columns) { Float.NEGATIVE_INFINITY }
    private val shownBottom = FloatArray(SmokeDimens.Columns) { Float.POSITIVE_INFINITY }

    /** Where the front stands, 0 at the edge the row burns from to 1 all the way across. */
    var burnt: Float = 0f
        private set

    /** When the front last moved on; never, at first. */
    var lastCross: Float = Float.NEGATIVE_INFINITY
        private set

    /** Whether the whole row has burnt. */
    val done: Boolean
        get() = burnt >= 1f

    /** Where the front stands across the row, as a fraction of its width from its left edge. */
    val edge: Float
        get() = if (fromRight) 1f - burnt else burnt

    /**
     * Moves the front to [to] of the row at [now], [seconds] after it was last moved. Moving on, a
     * column it passes starts to age from when the front passed it, between then and now in
     * proportion to where it lies, so a fast front still crosses its columns one after another, not
     * all at once; one still being wound back carries on from as old as it is, and a whole one is
     * stamped with where the row is now, [movedX] and [movedY] px from where it began to burn, and
     * the part of it that shows, [shownTop] to [shownBottom] px down it. Moving back, a column it
     * leaves is wound back from when it left it.
     */
    fun moveTo(
        to: Float,
        now: Float,
        seconds: Float,
        movedX: Float = 0f,
        movedY: Float = 0f,
        shownTop: Float = Float.NEGATIVE_INFINITY,
        shownBottom: Float = Float.POSITIVE_INFINITY,
    ) {
        val from = burnt
        val target = to.coerceIn(0f, 1f)
        if (target == from) return
        if (target > from) {
            for (c in timeRef.indices) {
                if (burning(c)) continue
                val d = distanceOf(c)
                if (d >= target) continue
                val share = ((target - d) / (target - from)).coerceIn(0f, 1f)
                val since = timeRef[c]
                val at =
                    if (since == Uncrossed) now - seconds * share
                    else maxOf(now - seconds * share, since)
                val age = ageOf(c, at).coerceAtLeast(0f)
                if (age == 0f) {
                    // Whole, or landed again: its smoke leaves from where the row is now.
                    this.movedX[c] = movedX
                    this.movedY[c] = movedY
                    this.shownTop[c] = shownTop
                    this.shownBottom[c] = shownBottom
                }
                ageRef[c] = age
                timeRef[c] = at
                rate[c] = 1f
            }
            lastCross = now
        } else {
            for (c in timeRef.indices) {
                if (!burning(c)) continue
                val d = distanceOf(c)
                if (d < target) continue
                val share = ((d - target) / (from - target)).coerceIn(0f, 1f)
                val at = maxOf(now - seconds * share, timeRef[c])
                // Smoke older than the longest life has all gone, however much older it is.
                val age = ageOf(c, at).coerceAtMost(SmokeMotion.longestLife)
                ageRef[c] = age
                timeRef[c] = at
                woundFrom[c] = age
                rate[c] = -maxOf(SmokeDimens.RewindRate, age / SmokeDimens.RewindMost)
            }
        }
        burnt = target
    }

    /** Whether [column] is burning: the front has passed it and not drawn back off it. */
    fun burning(column: Int): Boolean = timeRef[column] != Uncrossed && rate[column] > 0f

    /**
     * How long [column]'s particles have been smoke at [at]: nothing once it has been wound back
     * all the way, and [Float.NEGATIVE_INFINITY] while it is whole.
     */
    fun ageOf(column: Int, at: Float): Float {
        val since = timeRef[column]
        if (since == Uncrossed) return Float.NEGATIVE_INFINITY
        val age = ageRef[column] + rate[column] * (at - since)
        return if (rate[column] > 0f) age else age.coerceAtLeast(0f)
    }

    /**
     * When [column] would have been crossed had it burnt straight on from whole, or [Uncrossed]
     * unless it is burning.
     */
    fun crossing(column: Int): Float =
        if (burning(column)) timeRef[column] - ageRef[column] else Uncrossed

    /**
     * How far [column]'s smoke has been wound back at [at], from 0 as it begins to 1 landed: 0
     * while it burns, and 1 while it is whole.
     */
    fun woundBack(column: Int, at: Float): Float {
        if (timeRef[column] == Uncrossed) return 1f
        if (rate[column] > 0f) return 0f
        val from = woundFrom[column]
        return if (from <= 0f) 1f else 1f - ageOf(column, at) / from
    }

    /** Whether no column burns at [at], and the smoke of every one wound back has landed. */
    fun wholeAt(at: Float): Boolean {
        for (c in timeRef.indices) {
            if (timeRef[c] == Uncrossed) continue
            if (rate[c] > 0f || ageOf(c, at) > 0f) return false
        }
        return true
    }

    /**
     * How far from the edge it burns from the row is cut at [at], 0 to 1: at the front, but where
     * the front has been drawn back the row comes back column by column, each as its smoke lands,
     * so the cut runs behind the front until the smoke has caught up with it.
     */
    fun shownFrom(at: Float): Float {
        val n = SmokeDimens.Columns
        var cut = 1f
        for (i in 0 until n) {
            // From the far edge, the one that burns last, toward the one it burns from.
            val c = if (fromRight) i else n - 1 - i
            if (burning(c)) {
                cut = burnt
                break
            }
            val d = distanceOf(c)
            val back = woundBack(c, at)
            if (back >= 1f) {
                cut = d - 0.5f / n
                continue
            }
            // The column comes back from its far side over the last of its smoke's landing.
            cut = d + (0.5f - smoothstep(LandFrom, 1f, back)) / n
            break
        }
        return maxOf(cut, burnt)
    }

    /** How far across the row had moved from where it began to burn when [column] was crossed. */
    fun movedXOf(column: Int): Float = movedX[column]

    /** How far down the row had moved from where it began to burn when [column] was crossed. */
    fun movedYOf(column: Int): Float = movedY[column]

    /** The first row of pixels down the row that showed as [column] was crossed, in px. */
    fun shownTopOf(column: Int): Float = shownTop[column]

    /** The last row of pixels down the row that showed as [column] was crossed, in px. */
    fun shownBottomOf(column: Int): Float = shownBottom[column]

    // How far column [c]'s middle lies from the edge the row burns from, as a fraction of its
    // width.
    private fun distanceOf(c: Int): Float {
        val middle = (c + 0.5f) / SmokeDimens.Columns
        return if (fromRight) 1f - middle else middle
    }

    companion object {
        /** A column that is whole: never crossed, or wound back, landed and not crossed since. */
        const val Uncrossed: Float = Float.MAX_VALUE

        // A column wound back shows again over the last of its landing, from this far on.
        private const val LandFrom = 0.55f
    }
}
