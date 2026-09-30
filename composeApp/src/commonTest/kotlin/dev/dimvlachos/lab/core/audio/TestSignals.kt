package dev.dimvlachos.lab.core.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt
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

/**
 * [count] frames of the rumble a real blow makes on a microphone: noise low-passed at about 150 Hz,
 * scaled to [rms]. Not hiss: its power falls steeply with frequency.
 */
internal fun rumble(count: Int, rms: Float, random: Random): List<FloatArray> {
    val pole = exp(-2 * PI * 150 / MicSampleRate).toFloat()
    var y = 0f
    val samples =
        FloatArray(count * MicFrameSize) {
            y = pole * y + (1 - pole) * (random.nextFloat() * 2f - 1f)
            y
        }
    val scale = rms / sqrt(samples.sumOf { it.toDouble() * it } / samples.size).toFloat()
    return List(count) { frame ->
        FloatArray(MicFrameSize) { samples[frame * MicFrameSize + it] * scale }
    }
}
