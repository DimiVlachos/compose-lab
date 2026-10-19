package dev.dimvlachos.lab.core.presentation.components.bookshelf.book

internal data class WordRef(val paragraph: Int, val word: Int)

internal data class PageSelection(val paragraph: Int, val firstWord: Int, val lastWord: Int)

internal class PageText(paragraphs: List<List<String>>, private val firstParagraph: Int) {
    val text: String
    private val wordStarts: List<IntArray>
    private val wordEnds: List<IntArray>
    val paragraphStarts: IntArray

    init {
        val builder = StringBuilder()
        val starts = mutableListOf<IntArray>()
        val ends = mutableListOf<IntArray>()
        val paragraphOffsets = IntArray(paragraphs.size)
        paragraphs.forEachIndexed { p, words ->
            if (p > 0) builder.append('\n')
            paragraphOffsets[p] = builder.length
            val s = IntArray(words.size)
            val e = IntArray(words.size)
            words.forEachIndexed { w, word ->
                if (w > 0) builder.append(' ')
                s[w] = builder.length
                builder.append(word)
                e[w] = builder.length
            }
            starts += s
            ends += e
        }
        text = builder.toString()
        wordStarts = starts
        wordEnds = ends
        paragraphStarts = paragraphOffsets
    }

    fun rangeOf(ref: WordRef): IntRange? {
        val p = ref.paragraph - firstParagraph
        val start = wordStarts.getOrNull(p)?.getOrNull(ref.word) ?: return null
        return start until wordEnds[p][ref.word]
    }

    fun wordAt(offset: Int): WordRef? {
        val p = paragraphAt(offset) ?: return null
        val ends = wordEnds[p]
        val w = ends.indexOfFirst { it > offset }.takeIf { it >= 0 } ?: ends.lastIndex
        if (w < 0 || offset < wordStarts[p][w] - 1) return null
        return WordRef(firstParagraph + p, w)
    }

    fun select(start: Int, end: Int): PageSelection? {
        if (end <= start) return null
        val p = paragraphAt(start) ?: return null
        val starts = wordStarts[p]
        val ends = wordEnds[p]
        if (starts.isEmpty()) return null
        val first = ends.indexOfFirst { it > start }.takeIf { it >= 0 } ?: return null
        val last = starts.indexOfLast { it < end }.coerceAtLeast(first)
        return PageSelection(firstParagraph + p, first, last)
    }

    private fun paragraphAt(offset: Int): Int? {
        if (offset < 0 || offset > text.length || paragraphStarts.isEmpty()) return null
        return paragraphStarts.indexOfLast { it <= offset }.takeIf { it >= 0 }
    }
}
