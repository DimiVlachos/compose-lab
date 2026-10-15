package dev.dimvlachos.lab.core.presentation.components.smoke

/** Where a row's going up in smoke stands. */
internal enum class PlumePhase {
    /**
     * A finger holds the front: it follows the finger on, and back, where the columns it leaves are
     * wound back until their smoke lands in the row again.
     */
    Held,
    /** Let go past the commit point, or burnt by a key or an action: it burns on by itself. */
    Burning,
    /** All smoke. The plume drifts off and fades, and is kept, invisible, for an undo. */
    Burnt,
    /**
     * Undone: waiting for the row to open again before the smoke is breathed back in, what is still
     * in the air drifting on meanwhile.
     */
    Waiting,
    /** Undone: the smoke flows back into the row along the paths it left by. */
    Reforming,
    /**
     * Let go short of the commit point: the front goes back to the edge, every column is wound back
     * as a finger drawing it back would, and the row is whole once the last smoke has landed.
     */
    Returning,
}

/**
 * One row going up in [material]: its burn [front], its particles once the row has been sampled,
 * and where in the smoke's overlay the row lay as it began to burn ([originX], [originY], in px).
 * Each column's smoke leaves from where the row was as the front crossed it, so the smoke stays on
 * a row that moves as it burns, and once in the air stays there, whatever the list does under it;
 * breathed back in, it comes back to where the row is now ([atX], [atY]), at its size now.
 */
internal class SmokePlume(val key: Any, val material: SmokeMaterial, fromRight: Boolean) {
    val front = BurnFront(fromRight)

    /** The way the front runs across the row: -1 left, 1 right. */
    val travel: Float = if (fromRight) -1f else 1f

    /** The row it rose from, or the one it reforms into. */
    var row: SmokeRowState? = null

    /** What runs once the row is all smoke. */
    var action: SmokeAction? = null

    var particles: SmokeParticles? = null
    var originX = 0f
    var originY = 0f

    /** The row's size in px when it was sampled. */
    var width = 0f
    var height = 0f

    /** Where the row is now in the overlay, and its size, in px: followed while it is laid out. */
    var atX = 0f
    var atY = 0f
    var atWidth = 0f
    var atHeight = 0f

    var phase = PlumePhase.Held

    // Burning on by itself, in dp/s.
    var speed = 0f

    // When the row was all smoke, when the undo's reforming starts, and how far it has got.
    var burntAt = 0f
    var reformAt = 0f
    var reformSeconds = 0f
    var reform = -1f

    // Whether the burnt row's callbacks have run: the caller told, and the row closed.
    var told = false
    var closed = false

    // How the finger is holding it: how far it has dragged, in px, and how fast, in px/s; and
    // where across the row it took hold, in px from its left edge, if that is known.
    var dragged = 0f
    var velocity = 0f
    var heldAt = Float.NaN

    /**
     * How far in from the edge the row burns from the finger took hold, as a share of its width.
     */
    val grip: Float
        get() {
            if (heldAt.isNaN() || width < 1f) return 0f
            val across = (heldAt / width).coerceIn(0f, 1f)
            return if (front.fromRight) 1f - across else across
        }

    // Whether the drag is past the commit point, so a release burns the rest: set as it passes it,
    // and only cleared a little short of it, so a finger hovering there doesn't tick and tick.
    var armed = false

    /** The front's columns as the particles being filled see them, taken as their job is set. */
    val columns = FrontColumns()

    /**
     * The drawn particles' batches, by colour and age, and how far reforming had got when they were
     * filled; and the spare set the next frame is filled into, off the main thread, while these are
     * drawn.
     */
    var batches = PlumeBatches()
    var batchesReform = -1f
    var spare = PlumeBatches()

    /** Whether any of its smoke is in the air, or about to be. */
    fun visibleAt(now: Float): Boolean =
        when (phase) {
            PlumePhase.Held,
            PlumePhase.Burning,
            PlumePhase.Reforming,
            PlumePhase.Returning -> true
            // Waiting for the row to open, what is still in the air drifts on as it did.
            PlumePhase.Burnt,
            PlumePhase.Waiting -> now - front.lastCross < SmokeMotion.longestLife
        }

    /** How far reforming has got, 0 to 1, or -1 while it isn't. */
    val reformProgress: Float
        get() = if (phase == PlumePhase.Reforming) reform else -1f
}

/**
 * A copy of a [BurnFront]'s columns at one moment: each one's age, how far it has been wound back,
 * where the row was as it was crossed and what of it showed. The particles are worked out from it
 * off the main thread while the frame loop moves the front on.
 */
internal class FrontColumns {
    val age = FloatArray(SmokeDimens.Columns)
    val woundBack = FloatArray(SmokeDimens.Columns)
    val movedX = FloatArray(SmokeDimens.Columns)
    val movedY = FloatArray(SmokeDimens.Columns)
    val shownTop = FloatArray(SmokeDimens.Columns)
    val shownBottom = FloatArray(SmokeDimens.Columns)

    /** Copies [front]'s columns: their ages at [ageAt], and how far wound back at [now]. */
    fun take(front: BurnFront, ageAt: Float, now: Float) {
        for (c in age.indices) {
            age[c] = front.ageOf(c, ageAt)
            woundBack[c] = if (front.burning(c)) 0f else front.woundBack(c, now)
            movedX[c] = front.movedXOf(c)
            movedY[c] = front.movedYOf(c)
            shownTop[c] = front.shownTopOf(c)
            shownBottom[c] = front.shownBottomOf(c)
        }
    }

    /**
     * Whether [y] px down the row showed when [column] was crossed: what didn't, makes no smoke.
     */
    fun showed(column: Int, y: Float): Boolean = y >= shownTop[column] && y <= shownBottom[column]
}
