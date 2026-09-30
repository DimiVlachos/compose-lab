package dev.dimvlachos.lab.core.demo

import androidx.compose.ui.geometry.Offset
import kotlin.time.Duration

interface DemoController {
    val selectedIndex: Int

    fun select(index: Int)

    suspend fun scrollBy(px: Float)

    /**
     * Drags a finger through [path] over [duration]: points as fractions of the stage, sampled at
     * even time steps, so their spacing sets the finger's speed.
     */
    suspend fun wipe(path: List<Offset>, duration: Duration)

    /** Breathes on the stage for [duration], swelling to [strength], from 0 to 1, and fading. */
    suspend fun breathe(duration: Duration, strength: Float)

    /**
     * Starts a drop of condensation at [at], a fraction of the stage, to run [length] of its
     * height. Returns at once; the drop runs on its own.
     */
    suspend fun drip(at: Offset, length: Float)

    /** Mists the stage evenly back over in [duration], as a steamy room does on its own. */
    suspend fun mist(duration: Duration)
}
