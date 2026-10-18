package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

/** A cover's title set to fit: the [size] its [lines] are drawn at. */
internal class CoverTitle(val size: Float, val lines: List<String>)

private const val SHRINK_STEP = 0.95f
private const val ELLIPSIS = "…"

/**
 * Sets [title] in at most [maxLines] lines no wider than [maxWidth], as a designer would: at [size]
 * if it fits, else smaller, down to [minSize]; a title too long even then ends in an ellipsis.
 * [measure] gives a line's width at a size.
 */
internal fun fitCoverTitle(
    title: String,
    maxWidth: Float,
    maxLines: Int,
    size: Float,
    minSize: Float,
    measure: (String, Float) -> Float,
): CoverTitle {
    var s = size
    while (true) {
        val lines = wrapWords(title, maxWidth) { measure(it, s) }
        val fits = lines.size <= maxLines && lines.all { measure(it, s) <= maxWidth }
        if (fits) return CoverTitle(s, lines)
        if (s <= minSize)
            return CoverTitle(s, shortened(lines, maxLines, maxWidth) { measure(it, s) })
        s = maxOf(s * SHRINK_STEP, minSize)
    }
}

private fun wrapWords(text: String, maxWidth: Float, width: (String) -> Float): List<String> {
    val lines = mutableListOf<String>()
    var current = ""
    for (word in text.split(' ').filter { it.isNotEmpty() }) {
        val candidate = if (current.isEmpty()) word else "$current $word"
        if (width(candidate) > maxWidth && current.isNotEmpty()) {
            lines += current
            current = word
        } else {
            current = candidate
        }
    }
    if (current.isNotEmpty()) lines += current
    return lines
}

// The title cut to its first lines, the last ending in an ellipsis where the rest was dropped.
private fun shortened(
    lines: List<String>,
    maxLines: Int,
    maxWidth: Float,
    width: (String) -> Float,
): List<String> {
    val kept =
        lines.take(maxLines).map { if (width(it) <= maxWidth) it else clipped(it, maxWidth, width) }
    if (lines.size <= maxLines) return kept
    val last = kept.last().removeSuffix(ELLIPSIS)
    var words = last.split(' ')
    while (words.size > 1 && width(words.joinToString(" ") + ELLIPSIS) > maxWidth) {
        words = words.dropLast(1)
    }
    val ending = words.joinToString(" ")
    val end =
        if (width(ending + ELLIPSIS) <= maxWidth) ending + ELLIPSIS
        else clipped(ending, maxWidth, width)
    return kept.dropLast(1) + end
}

// As much of [line] as fits with an ellipsis after it.
private fun clipped(line: String, maxWidth: Float, width: (String) -> Float): String {
    var n = line.length
    while (n > 0 && width(line.take(n).trimEnd() + ELLIPSIS) > maxWidth) n--
    return line.take(n).trimEnd() + ELLIPSIS
}
