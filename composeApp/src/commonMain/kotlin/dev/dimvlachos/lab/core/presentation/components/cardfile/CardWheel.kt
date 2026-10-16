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
        val travel = velocity / CardFileDimens.Friction
        return (position + travel).roundToInt().coerceIn(0, count - 1)
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

    /** How many cards crossed the top going from [from] to [to]: one each half-card mark passed. */
    fun cardsCrossed(from: Float, to: Float): Int =
        abs(floor(to + 0.5f) - floor(from + 0.5f)).toInt()

    /**
     * Card [index]'s turn on the rod at [position], in degrees: 0 upright at the front, leaning
     * back the further behind it is, and falling forward as it is flipped over the top.
     */
    fun cardAngle(index: Int, position: Float): Float {
        val d = index - position
        return if (d >= 0f) {
            (d * CardFileDimens.BehindStep).coerceAtMost(CardFileDimens.BehindAngle)
        } else {
            (d * CardFileDimens.FlippedAngle).coerceAtLeast(-CardFileDimens.FlippedAngle)
        }
    }

    /** The cards composed around [position]: the front and [CardFileDimens.Window] either side. */
    fun window(position: Float, count: Int): IntRange {
        if (count <= 0) return IntRange.EMPTY
        val front = position.roundToInt().coerceIn(0, count - 1)
        return (front - CardFileDimens.Window).coerceAtLeast(0)..(front + CardFileDimens.Window)
                .coerceAtMost(count - 1)
    }
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
