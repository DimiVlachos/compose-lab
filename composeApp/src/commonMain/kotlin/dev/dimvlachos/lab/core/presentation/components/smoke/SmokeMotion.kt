package dev.dimvlachos.lab.core.presentation.components.smoke

import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Where a particle is, worked out from where it started and how old it is, and nothing else: a few
 * steps through a fixed curl-noise field, plus buoyancy. Nothing is carried from one frame to the
 * next, so a frame can be drawn for any moment, forwards or back, and running a particle's age back
 * to nothing brings it home to the very pixel it left.
 */
internal object SmokeMotion {
    /** How long a particle with [seed] lives, in seconds: ink longest, mist longer than smoke. */
    fun life(seed: Float, ink: Boolean, material: SmokeMaterial): Float {
        val base = SmokeDimens.Life + SmokeDimens.LifeSpread * fract(seed * 7.31f)
        val kind = if (ink) 1f else SmokeDimens.BackgroundLife
        return base * kind * if (material == SmokeMaterial.Mist) SmokeDimens.MistLife else 1f
    }

    /** The longest any particle lives, its jitter included. */
    val longestLife: Float =
        (SmokeDimens.Life + SmokeDimens.LifeSpread) * SmokeDimens.MistLife + SmokeDimens.Jitter

    /** How long after its column is crossed a particle with [seed] lifts off. */
    fun jitter(seed: Float): Float = fract(seed * 13.7f) * SmokeDimens.Jitter

    /** Whether an ink particle with [seed] stays lit as a spark. Mist has none. */
    fun isSpark(seed: Float, ink: Boolean, material: SmokeMaterial): Boolean =
        ink && material == SmokeMaterial.Smoke && fract(seed * 3.17f) < SmokeDimens.SparkShare

    /**
     * How old a particle is at [now], its column crossed at [cross]: or, while it is reforming,
     * [reform] of the way (0 to 1) back from how old it was when reforming began at [reformStart].
     * A particle still in the air then starts back from where it is, so nothing jumps; one that had
     * already faded starts from [SmokeDimens.ReformFrom] of its [life], so it gathers out of the
     * air as a visible wisp rather than out of an invisible haze. Negative while it hasn't lifted
     * off.
     */
    fun ageAt(
        now: Float,
        cross: Float,
        jitter: Float,
        life: Float,
        reform: Float = -1f,
        reformStart: Float = 0f,
    ): Float {
        if (reform < 0f) return now - cross - jitter
        val then = reformStart - cross - jitter
        if (then < 0f) return -1f
        val from = if (then < life) then else life * SmokeDimens.ReformFrom
        return from * (1f - reform)
    }

    /**
     * Writes where a particle that started at ([x], [y]) px is at [age] seconds into [out] (x then
     * y). [travel] is the way the front ran across its row, -1 left or 1 right: smoke is kicked
     * that way at first, and mist drifts it. [density] is px per dp; [scratch] holds two floats.
     */
    fun place(
        x: Float,
        y: Float,
        seed: Float,
        ink: Boolean,
        age: Float,
        life: Float,
        material: SmokeMaterial,
        travel: Float,
        density: Float,
        out: FloatArray,
        scratch: FloatArray,
    ) {
        var px = x
        var py = y
        if (age > 0f) {
            val cell = SmokeDimens.NoiseCell.value * density
            val spark = if (isSpark(seed, ink, material)) 1f else 0f
            val mist = material == SmokeMaterial.Mist
            val steps = SmokeDimens.PathSteps
            val dt = age / steps
            for (i in 0 until steps) {
                val s = (i + 0.5f) * dt
                val hold = smoothstep(0.08f, 0.9f, s)
                val qx = px / cell
                val qy = py / cell
                CurlNoise.curl(qx, qy + s * 0.32f, scratch)
                var cx = scratch[0]
                var cy = scratch[1]
                CurlNoise.curl(
                    qx * SmokeDimens.FineOctave + 3.7f,
                    qy * SmokeDimens.FineOctave + 9.1f + s * 0.7f,
                    scratch,
                )
                cx += SmokeDimens.FineShare * scratch[0]
                cy += SmokeDimens.FineShare * scratch[1]
                val vx: Float
                val vy: Float
                if (mist) {
                    val drift =
                        (SmokeDimens.MistDrift + SmokeDimens.MistDriftGain * s) *
                            (1f - exp(-2.5f * s))
                    vx = travel * drift + cx * SmokeDimens.MistCurl * hold
                    vy =
                        -(SmokeDimens.MistRise + SmokeDimens.MistRiseGain * s) +
                            cy * SmokeDimens.MistCurl * hold
                } else {
                    vx =
                        travel * SmokeDimens.KickSpeed * exp(-s * SmokeDimens.KickFade) +
                            cx * SmokeDimens.CurlSpeed * hold
                    vy =
                        -(SmokeDimens.RiseSpeed + SmokeDimens.RiseGain * s) *
                            (1f + spark * SmokeDimens.SparkRise) + cy * SmokeDimens.CurlSpeed * hold
                }
                px += vx * density * dt
                py += vy * density * dt
            }
            val spread = spreadAt(age, life, ink)
            val wander = SmokeDimens.Wander * density * spread * spread * if (mist) 1.5f else 1f
            px += (fract(seed * 91.37f) - 0.5f) * wander
            py += (fract(seed * 57.13f) - 0.5f) * wander
            // And each drifts off on its own a little, more the longer it has been smoke, so a
            // solid shape breaks up into wisps rather than flying off whole.
            val drift = SmokeDimens.Diffuse * density * sqrt(age) * smoothstep(0f, 0.35f, age)
            px += (fract(seed * 23.71f) - 0.5f) * 2f * drift
            py += (fract(seed * 41.93f) - 0.5f) * 2f * drift
        }
        out[0] = px
        out[1] = py
    }

    /**
     * How far a particle has bloomed out of its pixel, 0 to 1: the card's own colour almost at
     * once, ink only slowly, so the words keep their shape a moment.
     */
    fun spreadAt(age: Float, life: Float, ink: Boolean): Float =
        if (ink) smoothstep(0.05f, life * 0.7f, age) else smoothstep(0f, life * 0.6f, age)
}

internal fun fract(x: Float): Float = x - floor(x)

internal fun smoothstep(from: Float, to: Float, x: Float): Float {
    val t = ((x - from) / (to - from)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}
