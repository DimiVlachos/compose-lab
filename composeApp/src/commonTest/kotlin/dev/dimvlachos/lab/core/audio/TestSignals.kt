package dev.dimvlachos.lab.core.audio

import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** One frame of white noise, uniform in -[amplitude]..[amplitude]. */
internal fun noise(amplitude: Float, random: Random) =
    FloatArray(MicFrameSize) { (random.nextFloat() * 2f - 1f) * amplitude }

/** One frame, number [frameIndex] of a steady tone made of [hz], peaking near [amplitude]. */
internal fun tone(amplitude: Float, frameIndex: Int, vararg hz: Float) =
    FloatArray(MicFrameSize) { i ->
        val seconds = (frameIndex * MicFrameSize + i) / MicSampleRate.toDouble()
        (hz.sumOf { sin(2 * PI * it * seconds) } * amplitude / hz.size).toFloat()
    }

internal fun silence() = FloatArray(MicFrameSize)
