package dev.dimvlachos.lab.core.presentation.components.snowglobe

import kotlin.math.cos
import kotlin.math.sin

/**
 * The water's velocity in the globe: a vortex turning [swirl]'s way, a lift that carries glitter up
 * the middle, and a few slow sine eddies, all as strong as the swirl [energy].
 */
internal object WaterFlow {
    fun sample(x: Float, y: Float, t: Float, energy: Float, swirl: Float, out: FloatArray) {
        val r2 = x * x + y * y
        val falloff = 1f - 0.6f * r2
        var vx = -y * swirl * 1.3f * falloff
        var vy = x * swirl * 1.3f * falloff
        // Up through the middle, down along the glass.
        vy += -0.9f * (1f - 1.6f * x * x)
        vx += 0.35f * sin(2.1f * y + 1.3f * t)
        vy += 0.35f * cos(1.7f * x - 0.9f * t)
        out[0] = vx * energy
        out[1] = vy * energy
    }
}
