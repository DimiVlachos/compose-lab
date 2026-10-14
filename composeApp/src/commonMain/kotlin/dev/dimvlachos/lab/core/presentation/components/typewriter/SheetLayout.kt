package dev.dimvlachos.lab.core.presentation.components.typewriter

import androidx.compose.runtime.Immutable
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

/**
 * A text as the typewriter puts it on its sheet: its [lines], and where the carriage stands after
 * it, on line [line] at column [column]; its [glyphs], each letter in its place. [full] when the
 * last line has ended and the sheet can take no more; [overflow] when some of the text was left off
 * it for want of room.
 */
@Immutable
internal class SheetLayout(
    val lines: List<String>,
    val line: Int,
    val column: Int,
    val full: Boolean,
    val overflow: Boolean = false,
    val glyphs: List<Glyph> = emptyList(),
) {
    /** Whether there is anything on the sheet but spaces. */
    val inked: Boolean
        get() = lines.any { it.isNotBlank() }

    /** The lines, less any blank ones at the foot: what is sent. */
    val sent: List<String>
        get() = lines.dropLastWhile { it.isBlank() }.ifEmpty { listOf("") }

    companion object {
        val Empty = SheetLayout(listOf(""), line = 0, column = 0, full = false)
    }
}

/**
 * Lays [text] out on a sheet [columns] letters wide and at most [maxLines] lines tall, as the
 * typewriter types it. A new line starts at each line break; at the margin the carriage returns by
 * itself; and once the bell has rung, [bell] letters in, a space returns it too, as a typist who
 * heard the bell would at the end of the word, so words are kept whole where they can be. A space
 * just after the carriage has returned by itself is taken up by the return. The sheet holds no more
 * than [maxLines]: at the margin of the last, it is full, and what follows is left off.
 */
internal fun layOutSheet(
    text: CharSequence,
    columns: Int = TypewriterDimens.Columns,
    bell: Int = TypewriterDimens.BellColumn,
    maxLines: Int = TypewriterDimens.MaxLines,
): SheetLayout {
    val lines = mutableListOf(StringBuilder())
    // Whether the carriage has just returned by itself, so a space now is the one that ended a
    // word.
    var returned = false
    var full = false
    var overflow = false
    val glyphs = mutableListOf<Glyph>()
    fun newLine(): Boolean {
        if (lines.size >= maxLines) {
            full = true
            return false
        }
        lines += StringBuilder()
        return true
    }
    for ((index, char) in text.withIndex()) {
        if (full) {
            overflow = true
            break
        }
        val current = lines.last()
        when {
            char == '\n' -> {
                returned = false
                if (!newLine()) overflow = true
            }
            char == ' ' && returned && current.isEmpty() -> returned = false
            char == ' ' && current.length >= bell -> returned = newLine()
            else -> {
                returned = false
                if (char != ' ') glyphs += Glyph(index, lines.size - 1, current.length, char)
                current.append(char)
                if (current.length >= columns) returned = newLine()
            }
        }
    }
    val last = lines.last()
    return SheetLayout(
        lines.map { it.toString() },
        line = lines.size - 1,
        column = last.length,
        full = full,
        overflow = overflow,
        glyphs = glyphs,
    )
}

/**
 * A letter on the sheet: its [index] in the text, its place and itself, and how it was struck,
 * worked out once as it is laid out rather than every frame it is drawn.
 */
@Immutable
internal data class Glyph(val index: Int, val line: Int, val column: Int, val char: Char) {
    val strike: Strike = strikeOf(line, column, char)
}

/** The letters of [lines], as a slip lays them out: each line's letters at their columns. */
internal fun glyphsOf(lines: List<String>): List<Glyph> {
    val glyphs = mutableListOf<Glyph>()
    var index = 0
    for ((line, text) in lines.withIndex()) {
        for ((column, char) in text.withIndex()) {
            if (char != ' ') glyphs += Glyph(index, line, column, char)
            index++
        }
        index++
    }
    return glyphs
}

/**
 * How a struck letter lies on the paper: [dx] and [dy] off its place, in units, turned [tilt]
 * degrees, its ink [ink] dark (0 to 1).
 */
@Immutable internal data class Strike(val dx: Float, val dy: Float, val tilt: Float, val ink: Float)

/**
 * How the letter [char] struck at [column] of [line] lies: a little off its place, a little turned
 * and a little faint, the same every time for the same letter in the same place, so a sheet drawn
 * again, or its slip in the thread, looks just as it was typed.
 */
internal fun strikeOf(line: Int, column: Int, char: Char): Strike {
    val seed = line * 7919 + column * 104_729 + char.code * 31
    return Strike(
        dx = (noise(seed, 1) - 0.5f) * 2f * TypewriterDimens.Wobble,
        dy = (noise(seed, 2) - 0.5f) * 2f * TypewriterDimens.Bob,
        tilt = (noise(seed, 3) - 0.5f) * 2f * TypewriterDimens.Tilt,
        ink = TypewriterDimens.LeastInk + (1f - TypewriterDimens.LeastInk) * noise(seed, 4),
    )
}

/** A slip's turn, in degrees, for the message [id]: a little either way, never square. */
public fun slipTilt(id: Long): Float {
    val seed = (id * 2_654_435_761L).toInt()
    val side = if (noise(seed, 5) < 0.5f) -1f else 1f
    val size =
        TypewriterDimens.SlipLeastTilt +
            (TypewriterDimens.SlipMostTilt - TypewriterDimens.SlipLeastTilt) * noise(seed, 6)
    return side * size
}

// A number from 0 to 1 that looks random but is the same for the same seed and channel.
internal fun noise(seed: Int, channel: Int): Float {
    val x = sin((seed * 12.9898 + channel * 78.233) % (2 * PI * 1000)) * 43_758.5453
    return (x - floor(x)).toFloat()
}
