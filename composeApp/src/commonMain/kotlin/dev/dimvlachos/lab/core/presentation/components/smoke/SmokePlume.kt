package dev.dimvlachos.lab.core.presentation.components.smoke

/** Where a row's going up in smoke stands. */
internal enum class PlumePhase {
    /** A finger holds the front: it follows the finger, and only ever onwards. */
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
    /** The smoke flows back into the row along the paths it left by. */
    Reforming,
}

/**
 * One row going up in [material]: its burn [front], its particles once the row has been sampled,
 * and where in the smoke's overlay the row lay ([originX], [originY], in px), which is where the
 * plume stays, whatever the list does under it.
 */
internal class SmokePlume(val material: SmokeMaterial, fromRight: Boolean) {
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

    // How the finger is holding it: how far it has dragged, in px, and how fast, in px/s.
    var dragged = 0f
    var velocity = 0f

    // Whether the drag has passed the commit point yet, for its haptic tick.
    var armed = false

    /**
     * The drawn particles' batches, by colour and age, and how far reforming had got when they were
     * filled; and the spare set the next frame is filled into, off the main thread, while these are
     * drawn.
     */
    var batches = PlumeBatches()
    var batchesReform = -1f
    var spare = PlumeBatches()
    var filling = false

    /** Whether any of its smoke is in the air, or about to be. */
    fun visibleAt(now: Float): Boolean =
        when (phase) {
            PlumePhase.Held,
            PlumePhase.Burning,
            PlumePhase.Reforming -> true
            // Waiting for the row to open, what is still in the air drifts on as it did.
            PlumePhase.Burnt,
            PlumePhase.Waiting -> now - front.lastCross < SmokeMotion.longestLife
        }

    /** How far reforming has got, 0 to 1, or -1 while it isn't. */
    val reformProgress: Float
        get() = if (phase == PlumePhase.Reforming) reform else -1f
}
