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
}
