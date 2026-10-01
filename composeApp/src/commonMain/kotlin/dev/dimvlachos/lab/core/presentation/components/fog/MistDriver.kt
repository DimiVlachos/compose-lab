package dev.dimvlachos.lab.core.presentation.components.fog

import androidx.compose.runtime.Stable

/**
 * A still-steamy room misting a [FogState] back over: once the hand has stopped wiping for
 * [waitSeconds], the wiped glass fogs over evenly again in about [riseSeconds]. Wiping again clears
 * the new wipe on top of the mist; the mist starts over once the hand stops again.
 */
@Stable
class MistDriver(
    private val fog: FogState,
    private val waitSeconds: Float = 2f,
    private val riseSeconds: Float = 25f,
) {
    private var mist: Mist? = null
    private var idle = 0f
    private var lastWiping = -1

    /** Whether there is wiped glass to mist over: read in a snapshot, it wakes on a wipe. */
    val needed: Boolean
        get() = fog.marks.any { it is WipeStroke && it.clarity >= 1f }

    fun advance(seconds: Float) {
        if (!needed) {
            mist = null
            idle = 0f
            return
        }
        // The hand is still at it while wipes keep growing: wait for it to stop.
        val wiping = wipingSignature()
        if (wiping != lastWiping) {
            lastWiping = wiping
            idle = 0f
        }
        val current = mist?.takeIf { it.isAboveEveryWipe() }
        if (current == null) {
            idle += seconds
            if (idle >= waitSeconds) mist = fog.beginMist()
            return
        }
        fog.setMistAmount(current, current.amount + seconds / riseSeconds)
    }

    // Still on the glass, with no wipe made since: a drop's part-clear trail does not count.
    private fun Mist.isAboveEveryWipe(): Boolean {
        val index = fog.marks.indexOfFirst { it === this }
        if (index < 0) return false
        for (i in index + 1 until fog.marks.size) {
            val mark = fog.marks[i]
            if (mark is WipeStroke && mark.clarity >= 1f) return false
        }
        return true
    }

    // Changes whenever the hand wipes: a new stroke, or a stroke growing.
    private fun wipingSignature(): Int {
        var signature = 0
        for (mark in fog.marks) {
            if (mark is WipeStroke && mark.clarity >= 1f)
                signature = signature * 31 + mark.points.size
        }
        return signature
    }
}
