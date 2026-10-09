package dev.dimvlachos.lab.core.presentation.components.fishing

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

/**
 * The top of a band of water [width] dp wide, as [columns] columns of water side by side, each
 * pulled towards the level of its neighbours and, more gently, back to rest: a wave equation, so a
 * splash runs out both ways and settles. [heights] are how far each column's top sits below rest,
 * in dp (up is negative), evenly spaced from the left edge to the right. Its ends are free, so a
 * ripple turns back from them as from the side of a pool.
 */
internal class WaterSurface(columns: Int = FishingDimens.WaterColumns, width: Float) {
    val heights = FloatArray(columns)
    private val speeds = FloatArray(columns)

    // Each column's pull this step, worked out from the heights before any of them moves.
    private val pulls = FloatArray(columns)

    private var gap = width / (columns - 1)

    /** Whether every column sits at rest and has all but stopped. */
    var still = true
        private set

    /** Spreads the same columns over [width] dp instead: each ripple keeps its share of it. */
    fun resize(width: Float) {
        gap = width / (heights.size - 1)
    }

    /**
     * Pushes the water down at [x] at [push] dp a second, the most right at [x] and less out to
     * [spread] dp either side of it, as a bobber landing or dipping would. A push off the surface
     * pushes its nearest edge.
     */
    fun disturb(x: Float, push: Float, spread: Float) {
        val at = x.coerceIn(0f, gap * (heights.size - 1))
        for (i in heights.indices) {
            val off = abs(i * gap - at)
            if (off >= spread) continue
            speeds[i] += push * 0.5f * (1f + cos(PI.toFloat() * off / spread))
        }
        still = false
    }

    /** Steps the water on by [dt] seconds. */
    fun step(dt: Float) {
        val last = heights.size - 1
        // A ripple can't run further than a column in a step, or the steps outrun it and it grows
        // without end: on narrow water, where the columns sit close, it runs slower instead.
        val speed = min(FishingDimens.WaveSpeed, FishingDimens.StableShare * gap / dt)
        val tension = speed * speed / (gap * gap)
        for (i in heights.indices) {
            val left = heights[if (i == 0) 0 else i - 1]
            val right = heights[if (i == last) last else i + 1]
            pulls[i] =
                tension * (left + right - 2f * heights[i]) -
                    FishingDimens.WaterSpring * heights[i] -
                    FishingDimens.WaterDamping * speeds[i]
        }
        var most = 0f
        var fastest = 0f
        for (i in heights.indices) {
            speeds[i] += pulls[i] * dt
            heights[i] += speeds[i] * dt
            most = max(most, abs(heights[i]))
            fastest = max(fastest, abs(speeds[i]))
        }
        still = most < FishingDimens.StillHeight && fastest < FishingDimens.StillSpeed
    }

    /** How far below rest the water's top is at [x] dp from the left edge, between columns too. */
    fun heightAt(x: Float): Float {
        val at = (x / gap).coerceIn(0f, (heights.size - 1).toFloat())
        val i = at.toInt().coerceAtMost(heights.size - 2)
        val t = at - i
        return heights[i] + (heights[i + 1] - heights[i]) * t
    }

    /** Lays the water flat and still at once. */
    fun calm() {
        heights.fill(0f)
        speeds.fill(0f)
        still = true
    }
}
