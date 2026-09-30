package dev.dimvlachos.lab.frostdemo

import dev.dimvlachos.lab.core.presentation.components.frost.Breath
import dev.dimvlachos.lab.core.presentation.components.frost.FrostState
import kotlin.math.PI
import kotlin.math.sin

/**
 * Turns a blow into fog on [frost]: each moment of blowing raises the fog in step with the blow's
 * strength, so a steady full-strength blow covers the glass in [secondsToCover]. Blowing again
 * carries on the same fog, unless something has been wiped since: then new fog rises from the
 * bottom and covers that too. The microphone, a held finger and the script all breathe through
 * this.
 */
internal class BreathDriver(
    private val frost: FrostState,
    private val secondsToCover: Float = 1.2f,
) {
    private var breath: Breath? = null

    fun advance(strength: Float, seconds: Float) {
        if (strength <= 0f || seconds <= 0f) return
        val current =
            breath?.takeIf { frost.marks.lastOrNull() === it }
                ?: frost.beginBreath().also { breath = it }
        frost.setBreathLevel(current, current.level + strength * seconds / secondsToCover)
    }
}

/**
 * The script's breath at [time], from 0 to 1 through it: it swells to [peak] halfway and fades, as
 * a breath out does, rather than switching on and off.
 */
internal fun scriptedBreathStrength(time: Float, peak: Float): Float =
    peak * sin(PI * time.coerceIn(0f, 1f)).toFloat()
