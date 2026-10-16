package dev.dimvlachos.lab.core.presentation.components.snowglobe

import kotlin.math.cos
import kotlin.math.sin

/**
 * The water's velocity in the globe: a vortex turning [swirl]'s way, a lift that carries glitter up
 * the middle and eases off toward the top of the glass, and a few slow sine eddies, all as strong
 * as the swirl [energy].
 */
internal object WaterFlow {
    fun sample(x: Float, y: Float, t: Float, energy: Float, swirl: Float, out: FloatArray) {
        val r2 = x * x + y * y
        val falloff = 1f - 0.6f * r2
        val vortex = SnowGlobeDimens.VortexGain * swirl * falloff
        var vx = -y * vortex
        var vy = x * vortex
        // Up through the middle, down along the glass; the lift fades to nothing at the top, so
        // the glitter hangs in the water rather than piling against the glass.
        val r = SnowGlobeDimens.InnerRadius
        val ceiling = ((y + r) / r).coerceIn(0f, 1.5f)
        vy += -SnowGlobeDimens.LiftGain * (1f - 1.6f * x * x) * ceiling
        vx += SnowGlobeDimens.EddyGain * sin(2.1f * y + 1.3f * t)
        vy += SnowGlobeDimens.EddyGain * cos(1.7f * x - 0.9f * t)
        out[0] = vx * energy
        out[1] = vy * energy
    }
}
