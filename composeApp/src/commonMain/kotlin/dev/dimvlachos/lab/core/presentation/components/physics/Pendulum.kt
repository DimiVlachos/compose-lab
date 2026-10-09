package dev.dimvlachos.lab.core.presentation.components.physics

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * A damped pendulum: something that hangs from a point and swings, as the pull cord's lamp shade
 * does from the ceiling and a horseshoe magnet does from its nail. [angle] is how far it hangs from
 * straight down, in radians, and [swing] how fast that is changing, in radians a second. Left alone
 * it swings at [hz] times a second, losing [damping] of its swing as it goes, and it never tilts
 * past [most] either way: there it stops dead, as against a stop.
 */
internal class Pendulum(
    private val hz: Float,
    private val damping: Float,
    private val most: Float = Float.MAX_VALUE,
) {
    var angle = 0f
    var swing = 0f

    /** Swings it on by [dt] seconds, with [turn] more, in radians a second squared, turning it. */
    fun step(dt: Float, turn: Float = 0f) {
        val omega = 2f * PI.toFloat() * hz
        val accel = turn - omega * omega * sin(angle) - 2f * damping * omega * swing
        swing += accel * dt
        angle += swing * dt
        if (abs(angle) > most) {
            angle = angle.coerceIn(-most, most)
            swing = 0f
        }
    }

    /** Whether it hangs within [level] radians of straight down, swinging slower than [still]. */
    fun isStill(level: Float, still: Float): Boolean = abs(angle) < level && abs(swing) < still

    /** Hangs it straight and still. */
    fun stop() {
        angle = 0f
        swing = 0f
    }
}
