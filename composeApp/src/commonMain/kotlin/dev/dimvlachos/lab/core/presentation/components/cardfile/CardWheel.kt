package dev.dimvlachos.lab.core.presentation.components.cardfile

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * The card file's wheel as numbers. Its position is measured in cards: card i stands upright at the
 * front when the position is i. Turning it on flips cards over the top toward you.
 */
internal object CardWheel {
    /** The whole card a release at [position] going [velocity] cards/s comes to rest on. */
    fun restingCard(position: Float, velocity: Float, count: Int): Int {
        if (count <= 0) return 0
        val v = if (abs(velocity) < CardFileDimens.MinFlingVelocity) 0f else velocity
        return (position + v * CardFileDimens.FlingCarry).roundToInt().coerceIn(0, count - 1)
    }

    /** How far the wheel goes for a drag [over] past an end: always less, ever stiffer. */
    fun rubberBand(over: Float): Float = over / (1f + abs(over) * CardFileDimens.RubberStiffness)

    /** A raw dragged position, rubber-banded past the deck's ends. */
    fun banded(raw: Float, count: Int): Float {
        val last = (count - 1).coerceAtLeast(0).toFloat()
        return when {
            raw < 0f -> rubberBand(raw)
            raw > last -> last + rubberBand(raw - last)
            else -> raw
        }
    }

    /**
     * The raw drag that [banded] turns into [position]. No drag reaches the band's asymptote, but a
     * spring can swing the wheel there, so the overshoot is held just short of it and the raw drag
     * stays a few cards out rather than running off.
     */
    fun unbanded(position: Float, count: Int): Float {
        val last = (count - 1).coerceAtLeast(0).toFloat()
        val k = CardFileDimens.RubberStiffness
        val reach = 0.95f / k
        fun unband(over: Float): Float {
            val o = over.coerceAtMost(reach)
            return o / (1f - o * k)
        }
        return when {
            position < 0f -> -unband(-position)
            position > last -> last + unband(position - last)
            else -> position
        }
    }

    /** How many cards crossed the top going from [from] to [to]: one each half-card mark passed. */
    fun cardsCrossed(from: Float, to: Float): Int =
        abs(floor(to + 0.5f) - floor(from + 0.5f)).toInt()
}

/** Lets a tick through at most once every [minGapMs]: a fast spin drops ticks, not queues them. */
internal class TickThrottle(private val minGapMs: Long) {
    private var last = Long.MIN_VALUE / 2

    fun shouldTick(nowMs: Long): Boolean {
        if (nowMs - last < minGapMs) return false
        last = nowMs
        return true
    }
}
