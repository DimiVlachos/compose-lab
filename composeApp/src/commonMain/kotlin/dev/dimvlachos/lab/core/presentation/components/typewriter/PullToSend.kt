package dev.dimvlachos.lab.core.presentation.components.typewriter

import kotlin.math.exp

/**
 * Pulling the sheet up out of the roller, to send it: the maths, in units. The finger's [pull] is
 * how far it has moved up since it took the sheet; the paper follows it like a rubber band that
 * stiffens as it comes out, and an empty sheet, with nothing to send, barely moves at all.
 */
internal object PullToSend {
    /** How far the paper has come out for a finger [pull] units up, [inked] or blank. */
    fun shown(pull: Float, inked: Boolean): Float {
        val p = pull.coerceAtLeast(0f)
        return if (inked) {
            TypewriterDimens.MostPull * (1f - exp(-p / TypewriterDimens.MostPull))
        } else {
            TypewriterDimens.EmptyPull * (1f - exp(-p / TypewriterDimens.EmptyPullScale))
        }
    }

    /**
     * Whether letting go of a sheet out by [shown] units, the finger moving up at [speed] units a
     * second, sends it: out past the threshold, or flung up once it has started to come out. A
     * blank sheet is never sent.
     */
    fun sends(shown: Float, speed: Float, inked: Boolean): Boolean =
        inked &&
            (shown > TypewriterDimens.SendPull ||
                (speed > TypewriterDimens.FlingSpeed && shown > TypewriterDimens.FlingPull))

    /**
     * How many ratchet clicks the paper has made moving from [from] to [to], counting a click each
     * [RatchetStep][TypewriterDimens.RatchetStep] it crosses either way.
     */
    fun ticks(from: Float, to: Float): Int {
        val step = TypewriterDimens.RatchetStep
        return kotlin.math.abs(
            kotlin.math.floor(to / step).toInt() - kotlin.math.floor(from / step).toInt()
        )
    }
}
