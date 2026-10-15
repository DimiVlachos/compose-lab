package dev.dimvlachos.lab.core.presentation.components.smoke

import kotlin.math.abs
import kotlin.random.Random

/**
 * A row cut into particles: [count] of them, each where it lay in the row ([x], [y], in px from its
 * top left), in one of the row's [palette] colours ([colour]; the card's own is 0, the rest are
 * ink), with a [seed] that sets its life and its wander, and the [column] of the burn front it
 * belongs to.
 */
internal class SmokeParticles(
    val count: Int,
    val x: FloatArray,
    val y: FloatArray,
    val colour: ByteArray,
    val seed: FloatArray,
    val column: ByteArray,
    val palette: IntArray,
) {
    /** Whether particle [i] is ink rather than the card. */
    fun isInk(i: Int): Boolean = colour[i] > 0
}

/**
 * Cuts a drawing of a row into particles. Every pixel of ink is a candidate, and a sparse sample of
 * the card's own colour, so the words and the avatar lift off in their own colours while the card
 * blooms into haze. The card's colour is the commonest in the row; ink is anything far enough from
 * it. The row's colours are cut down to a few, so the particles can be drawn in a batch a colour.
 */
internal object RowSampler {
    /**
     * Samples [pixels], a [width] × [height] ARGB drawing of a row, into about [budget] particles,
     * the same ones every time for the same [seed]. Fully clear pixels are skipped: a rounded
     * corner has no card there to burn.
     */
    fun sample(pixels: IntArray, width: Int, height: Int, budget: Int, seed: Int): SmokeParticles {
        val bins = IntArray(BinCount)
        val sumR = IntArray(BinCount)
        val sumG = IntArray(BinCount)
        val sumB = IntArray(BinCount)
        val total = width * height
        for (i in 0 until total) {
            val p = pixels[i]
            if (alpha(p) < Opaque) continue
            val bin = binOf(p)
            bins[bin]++
            sumR[bin] += red(p)
            sumG[bin] += green(p)
            sumB[bin] += blue(p)
        }
        var cardBin = 0
        for (b in bins.indices) if (bins[b] > bins[cardBin]) cardBin = b
        if (bins[cardBin] == 0) return empty()
        val card = average(cardBin, bins, sumR, sumG, sumB)

        // The ink's commonest colours, after the card's.
        val inkBins = IntArray(BinCount)
        var inkCount = 0
        var cardCount = 0
        for (i in 0 until total) {
            val p = pixels[i]
            if (alpha(p) < Opaque) continue
            if (isInk(p, card)) {
                inkBins[binOf(p)]++
                inkCount++
            } else cardCount++
        }
        val palette = mutableListOf(card)
        val taken = BooleanArray(BinCount)
        while (palette.size < SmokeDimens.MostColours) {
            var best = -1
            for (b in inkBins.indices) {
                if (taken[b] || inkBins[b] == 0) continue
                if (best < 0 || inkBins[b] > inkBins[best]) best = b
            }
            if (best < 0 || inkBins[best] < inkCount / MinShareOfInk) break
            taken[best] = true
            palette += average(best, bins, sumR, sumG, sumB)
        }

        val inkWanted = minOf(inkCount.toFloat(), budget * SmokeDimens.InkShare)
        val cardWanted = (budget - inkWanted).coerceAtLeast(0f)
        val inkChance = if (inkCount == 0) 0f else inkWanted / inkCount
        val cardChance = if (cardCount == 0) 0f else (cardWanted / cardCount).coerceAtMost(1f)
        val most = minOf(SmokeDimens.MostPerRow, budget + budget / 8)
        val xs = FloatArray(most)
        val ys = FloatArray(most)
        val colours = ByteArray(most)
        val seeds = FloatArray(most)
        val columns = ByteArray(most)
        val random = Random(seed)
        var n = 0
        val inkPalette = palette.size > 1
        for (i in 0 until total) {
            if (n == most) break
            val p = pixels[i]
            if (alpha(p) < Opaque) continue
            val ink = inkPalette && isInk(p, card)
            if (random.nextFloat() >= if (ink) inkChance else cardChance) continue
            val px = i % width
            val py = i / width
            xs[n] = px + random.nextFloat()
            ys[n] = py + random.nextFloat()
            colours[n] = if (ink) nearestInk(p, palette).toByte() else 0
            seeds[n] = random.nextFloat()
            columns[n] =
                (px * SmokeDimens.Columns / width).coerceAtMost(SmokeDimens.Columns - 1).toByte()
            n++
        }
        return SmokeParticles(n, xs, ys, colours, seeds, columns, palette.toIntArray())
    }

    private fun empty() =
        SmokeParticles(
            0,
            FloatArray(0),
            FloatArray(0),
            ByteArray(0),
            FloatArray(0),
            ByteArray(0),
            IntArray(1),
        )

    private fun isInk(p: Int, card: Int): Boolean = distance(p, card) > SmokeDimens.InkDistance

    private fun nearestInk(p: Int, palette: List<Int>): Int {
        var best = 1
        var bestDistance = Int.MAX_VALUE
        for (k in 1 until palette.size) {
            val d = distance(p, palette[k])
            if (d < bestDistance) {
                bestDistance = d
                best = k
            }
        }
        return best
    }

    private fun distance(a: Int, b: Int): Int =
        abs(red(a) - red(b)) + abs(green(a) - green(b)) + abs(blue(a) - blue(b))

    private fun average(bin: Int, bins: IntArray, r: IntArray, g: IntArray, b: IntArray): Int {
        val n = bins[bin].coerceAtLeast(1)
        return (0xFF shl 24) or ((r[bin] / n) shl 16) or ((g[bin] / n) shl 8) or (b[bin] / n)
    }

    // Colours are binned four bits a channel: near enough that antialiased shades of one colour
    // fall together, far enough apart that the text and the card never do.
    private fun binOf(p: Int): Int =
        ((red(p) shr 4) shl 8) or ((green(p) shr 4) shl 4) or (blue(p) shr 4)

    private fun alpha(p: Int) = (p ushr 24) and 0xFF

    private fun red(p: Int) = (p shr 16) and 0xFF

    private fun green(p: Int) = (p shr 8) and 0xFF

    private fun blue(p: Int) = p and 0xFF

    private const val BinCount = 4_096
    private const val Opaque = 128

    // An ink colour needs at least one pixel in this many of the ink to have its own batch.
    private const val MinShareOfInk = 200
}
