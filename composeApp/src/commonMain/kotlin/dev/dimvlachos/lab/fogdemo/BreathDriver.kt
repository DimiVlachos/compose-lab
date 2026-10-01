package dev.dimvlachos.lab.fogdemo

import dev.dimvlachos.lab.core.presentation.components.fog.Breath
import dev.dimvlachos.lab.core.presentation.components.fog.FogState
import dev.dimvlachos.lab.core.presentation.components.fog.WipeStroke

/**
 * Turns a blow into fog on [fog]: each moment of blowing raises the fog in step with the blow's
 * strength, so a steady full-strength blow covers the glass in [secondsToCover]. Blowing again
 * carries on the same fog, unless something has been wiped since: then new fog rises from the
 * bottom and covers that too. The microphone and a held finger both breathe through this.
 */
internal class BreathDriver(
    private val fog: FogState,
    private val secondsToCover: Float = 1.2f,
) {
    private var breath: Breath? = null

    private fun Breath.isLatestBesidesStreaks(): Boolean {
        val index = fog.marks.indexOfLast { it === this }
        if (index < 0) return false
        for (i in index + 1 until fog.marks.size) {
            val mark = fog.marks[i]
            if (mark !is WipeStroke || mark.clarity >= 1f) return false
        }
        return true
    }

    fun advance(strength: Float, seconds: Float) {
        if (strength <= 0f || seconds <= 0f) return
        // Carries on unless the user has wiped since: a running drop's thin streak is not a wipe.
        val current =
            breath?.takeIf { it.isLatestBesidesStreaks() } ?: fog.beginBreath().also { breath = it }
        fog.setBreathLevel(current, current.level + strength * seconds / secondsToCover)
    }
}
