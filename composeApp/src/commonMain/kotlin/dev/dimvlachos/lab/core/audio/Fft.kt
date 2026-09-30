package dev.dimvlachos.lab.core.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sin

/**
 * The power in each frequency bin, from 0 to half the sample rate, of [samples] under a Hann
 * window. [samples] must be a power of two long; the result has one more than half as many bins.
 */
internal fun powerSpectrum(samples: FloatArray): FloatArray {
    val n = samples.size
    require(n > 1 && (n and (n - 1)) == 0) { "the frame must be a power of two long: $n" }
    val re = DoubleArray(n) { samples[it] * (0.5 - 0.5 * cos(2 * PI * it / (n - 1))) }
    val im = DoubleArray(n)
    // Bit-reversed order, then butterflies of doubling length.
    var j = 0
    for (i in 1 until n) {
        var bit = n shr 1
        while ((j and bit) != 0) {
            j = j xor bit
            bit = bit shr 1
        }
        j = j xor bit
        if (i < j) {
            val swap = re[i]
            re[i] = re[j]
            re[j] = swap
        }
    }
    var length = 2
    while (length <= n) {
        val angle = -2 * PI / length
        for (start in 0 until n step length) {
            for (k in 0 until length / 2) {
                val wr = cos(angle * k)
                val wi = sin(angle * k)
                val a = start + k
                val b = a + length / 2
                val xr = re[b] * wr - im[b] * wi
                val xi = re[b] * wi + im[b] * wr
                re[b] = re[a] - xr
                im[b] = im[a] - xi
                re[a] += xr
                im[a] += xi
            }
        }
        length = length shl 1
    }
    return FloatArray(n / 2 + 1) { (re[it] * re[it] + im[it] * im[it]).toFloat() }
}

/**
 * How noise-like the sound is between [fromHz] and [toHz]: the geometric over the arithmetic mean
 * of the [power]. Near 1 for hiss, whose power is spread evenly; near 0 for a tone or a voice,
 * whose power sits in a few bins.
 */
internal fun spectralFlatness(
    power: FloatArray,
    sampleRate: Int,
    fromHz: Float,
    toHz: Float,
): Float {
    val binHz = sampleRate / 2f / (power.size - 1)
    val from = (fromHz / binHz).toInt().coerceAtLeast(1)
    val to = (toHz / binHz).toInt().coerceAtMost(power.size - 1)
    var logSum = 0.0
    var sum = 0.0
    for (i in from..to) {
        val p = power[i] + 1e-12
        logSum += ln(p)
        sum += p
    }
    val count = to - from + 1
    return (exp(logSum / count) / (sum / count)).toFloat()
}
