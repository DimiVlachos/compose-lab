package dev.dimvlachos.lab.core.presentation.components.decoderring

import androidx.compose.runtime.Immutable

/** One character of a wrapped text: where it starts ([x]), how wide it is and which [line]. */
@Immutable internal class DecoderGlyph(val ch: Char, val x: Float, val w: Float, val line: Int)

/** A text wrapped into [lines] lines, one [DecoderGlyph] for each of its characters. */
@Immutable internal class DecoderLines(val chars: List<DecoderGlyph>, val lines: Int)

/**
 * The prototype's greedy word wrap at [maxWidth]: a word that doesn't fit goes down a line, and
 * each character is measured within its word, as the difference of its prefixes, so kerning stays
 * honest. [measure] gives a string's advance in the units of [maxWidth]; spaces count.
 */
internal fun wrapDecoderText(
    text: String,
    maxWidth: Float,
    measure: (String) -> Float,
): DecoderLines {
    val out = ArrayList<DecoderGlyph>(text.length)
    val words = text.split(' ')
    val sp = measure(" ")
    var x = 0f
    var line = 0
    words.forEachIndexed { wi, w ->
        val ww = measure(w)
        if (x > 0f && x + ww > maxWidth) {
            x = 0f
            line++
        }
        var x0 = 0f
        for (j in w.indices) {
            val x1 = measure(w.substring(0, j + 1))
            out += DecoderGlyph(w[j], x + x0, x1 - x0, line)
            x0 = x1
        }
        x += ww
        if (wi < words.size - 1) {
            out += DecoderGlyph(' ', x, sp, line)
            x += sp
        }
    }
    return DecoderLines(out, line + 1)
}

/**
 * Character i of the wave: the Spanish [ca] at [pa], the English [cb] at [pb] (a missing end takes
 * the other's place), and its short reel of [d] steps, which runs letters leading up to the
 * target's alphabet index [ib], in capitals if [up].
 */
@Immutable
internal class DecoderSlot(
    val ca: Char,
    val cb: Char,
    val pa: DecoderGlyph,
    val pb: DecoderGlyph,
    val ib: Int,
    val d: Int,
    val up: Boolean,
) {
    /** A space at both ends: nothing to draw. */
    val blank = ca == ' ' && cb == ' '

    /** Whether its two slots are on different lines, so it rolls out and in rather than sliding. */
    val jump = pa.line != pb.line

    /** Step [j] of the reel that starts from [src]: [src] itself, letters, then the target. */
    fun reel(j: Int, src: Char): Char {
        if (j <= 0) return src
        if (j >= d) return cb
        val l = 'a' + ((ib - d + j) % 26 + 26) % 26
        return if (up) l.uppercaseChar() else l
    }

    internal companion object {
        /** The slots from Spanish [a] to English [b], one for each character of the longer. */
        fun build(a: DecoderLines, b: DecoderLines): List<DecoderSlot> {
            val n = maxOf(a.chars.size, b.chars.size)
            return List(n) { i ->
                val ga = a.chars.getOrNull(i)
                val gb = b.chars.getOrNull(i)
                val ca = ga?.ch ?: ' '
                val cb = gb?.ch ?: ' '
                val pa = ga ?: gb!!
                val pb = gb ?: ga!!
                val d = ReelMin + (i * 5) % (ReelMax - ReelMin + 1)
                DecoderSlot(ca, cb, pa, pb, idx(cb), d, if (isLetter(cb)) isUp(cb) else isUp(ca))
            }
        }
    }
}

// Each slot's reel shows this many steps (the source letter, then letters, then the target), varied
// per slot so neighbours don't tick in step.
internal const val ReelMin = 4
internal const val ReelMax = 7

private const val Accented = "áàâäãåéèêëíìîïóòôöõúùûüñçýÿÁÀÂÄÃÅÉÈÊËÍÌÎÏÓÒÔÖÕÚÙÛÜÑÇÝ"
private const val Plain = "aaaaaaeeeeiiiiooooouuuuncyyAAAAAAEEEEIIIIOOOOOUUUUNCY"

/** The character without its accent, as NFD with the marks dropped. */
internal fun baseOf(c: Char): Char = Accented.indexOf(c).let { if (it < 0) c else Plain[it] }

internal fun isLetter(c: Char) = baseOf(c).let { it in 'a'..'z' || it in 'A'..'Z' }

/** A letter's place in the alphabet; anything else gets a steady one of its own. */
internal fun idx(c: Char) = if (isLetter(c)) baseOf(c).lowercaseChar() - 'a' else (c.code * 7) % 26

private fun isUp(c: Char) = isLetter(c) && c == c.uppercaseChar()
