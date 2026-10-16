package dev.dimvlachos.lab.core.presentation.components.snowglobe

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * The gold flakes in the water, kept in flat arrays so a frame allocates nothing. A flake takes up
 * the [WaterFlow]'s velocity, sinks fluttering as it tumbles, never leaves the water's sphere, and
 * comes to rest on the [Figurine] or the glass, lifting off again when the flow is strong.
 */
internal class GlitterField(val count: Int, seed: Int) {
    val x = FloatArray(count)
    val y = FloatArray(count)
    private val vx = FloatArray(count)
    private val vy = FloatArray(count)
    val phase = FloatArray(count)
    private val spin = FloatArray(count)
    val size = FloatArray(count)
    val resting = BooleanArray(count)
    private val flow = FloatArray(2)

    init {
        val random = Random(seed)
        val r = SnowGlobeDimens.InnerRadius
        for (i in 0 until count) {
            x[i] = (random.nextFloat() * 2f - 1f) * r * 0.95f
            phase[i] = random.nextFloat() * 6.2832f
            spin[i] = 2f + random.nextFloat() * 5f
            size[i] = 0.010f + random.nextFloat() * 0.012f
        }
        settleAll()
    }

    /**
     * Where a flake at [x] comes to rest: the figurine, or the glass's floor where that is higher.
     */
    fun restY(x: Float): Float {
        val r = SnowGlobeDimens.InnerRadius
        val floor = sqrt(maxOf(0f, r * r - x * x))
        return min(Figurine.restHeight(x), floor)
    }

    /** Puts every flake straight down where it rests. */
    fun settleAll() {
        for (i in 0 until count) {
            y[i] = restY(x[i])
            vx[i] = 0f
            vy[i] = 0f
            resting[i] = true
        }
    }

    /** How brightly flake [i] catches the light: it flashes as its face turns to it. */
    fun glint(i: Int): Float = abs(cos(phase[i])).pow(8)

    fun step(dt: Float, t: Float, energy: Float, swirl: Float) {
        val r = SnowGlobeDimens.InnerRadius
        val take = min(1f, dt * SnowGlobeDimens.Drag)
        for (i in 0 until count) {
            if (resting[i]) {
                if (energy <= SnowGlobeDimens.LiftEnergy) continue
                resting[i] = false
            }
            WaterFlow.sample(x[i], y[i], t, energy, swirl, flow)
            val face = abs(sin(phase[i]))
            val sink = SnowGlobeDimens.Sink * (0.6f + 0.4f * face)
            vx[i] += (flow[0] + sin(phase[i]) * SnowGlobeDimens.Flutter - vx[i]) * take
            vy[i] += (flow[1] + sink - vy[i]) * take
            phase[i] += spin[i] * dt * (1f + energy * 4f)
            x[i] += vx[i] * dt
            y[i] += vy[i] * dt

            // The glass: back onto the sphere, losing the outward part of the velocity.
            val d2 = x[i] * x[i] + y[i] * y[i]
            if (d2 > r * r) {
                val d = sqrt(d2)
                val nx = x[i] / d
                val ny = y[i] / d
                x[i] = nx * r
                y[i] = ny * r
                val out = vx[i] * nx + vy[i] * ny
                if (out > 0f) {
                    vx[i] -= out * nx
                    vy[i] -= out * ny
                }
            }

            val rest = restY(x[i])
            if (y[i] >= rest) {
                y[i] = rest
                if (energy <= SnowGlobeDimens.LiftEnergy) {
                    vx[i] = 0f
                    vy[i] = 0f
                    resting[i] = true
                } else if (vy[i] > 0f) {
                    vy[i] = 0f
                }
            }
        }
    }
}
