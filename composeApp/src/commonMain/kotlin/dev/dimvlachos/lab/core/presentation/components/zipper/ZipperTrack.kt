package dev.dimvlachos.lab.core.presentation.components.zipper

import androidx.compose.foundation.lazy.LazyListState
import kotlin.math.roundToInt

/**
 * Maps a [LazyListState] onto the zipper's 0 to 1, as the prototype's `scrollTop / maxScroll`. A
 * lazy list doesn't know its whole height, so each item says which [kinds] of row it is (a section
 * header, a row, ...), every row of a kind is taken to be as tall as the last one laid out, and
 * [estimates] (px, by kind) stand in until one has been. [sectionLetters] holds the letter of every
 * item that starts a section and null for the rest.
 */
internal class ZipperTrack(
    val list: LazyListState,
    private val kinds: IntArray,
    estimates: FloatArray,
    private val sectionLetters: List<String?>,
) {
    private val heights = estimates.copyOf()

    // How many items of each kind come before each index, so an offset costs no walk.
    private val before: Array<IntArray> =
        Array(heights.size) { k ->
            IntArray(kinds.size + 1).also { a ->
                for (i in kinds.indices) a[i + 1] = a[i] + if (kinds[i] == k) 1 else 0
            }
        }
    private val sections: IntArray =
        sectionLetters.indices.filter { sectionLetters[it] != null }.toIntArray()

    init {
        require(kinds.size == sectionLetters.size) { "Each item needs a kind and a letter slot" }
    }

    /** Takes the heights of the rows on screen as the heights of their kinds. */
    fun measure() {
        val info = list.layoutInfo
        for (item in info.visibleItemsInfo) {
            if (item.index < kinds.size) heights[kinds[item.index]] = item.size.toFloat()
        }
    }

    /** Where item [index] starts in the list's content, in px. */
    fun offsetOf(index: Int): Float {
        var o = 0f
        val i = index.coerceIn(0, kinds.size)
        for (k in heights.indices) o += before[k][i] * heights[k]
        return o
    }

    /** How far the list has scrolled, in px. */
    fun scrolled(): Float = offsetOf(list.firstVisibleItemIndex) + list.firstVisibleItemScrollOffset

    /** How far the list can scroll, in px. */
    fun maxScroll(): Float {
        val info = list.layoutInfo
        val viewport = info.viewportSize.height - info.beforeContentPadding
        return (offsetOf(kinds.size) + info.afterContentPadding - viewport).coerceAtLeast(1f)
    }

    /** How far down the list is, 0 to 1. */
    fun position(): Float = (scrolled() / maxScroll()).coerceIn(0f, 1f)

    /** Puts the list [p] of the way down, on the next layout. */
    fun scrollTo(p: Float) = scrollToPx(p.coerceIn(0f, 1f) * maxScroll())

    private fun scrollToPx(target: Float) {
        var lo = 0
        var hi = kinds.size - 1
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            if (offsetOf(mid) <= target) lo = mid else hi = mid - 1
        }
        list.requestScrollToItem(lo, (target - offsetOf(lo)).roundToInt().coerceAtLeast(0))
    }

    /** The section at [top] px into the content, as the prototype's `letterAt`. */
    fun letterAt(top: Float): String {
        var l = sections.firstOrNull()?.let { sectionLetters[it] } ?: "A"
        for (s in sections) {
            if (offsetOf(s) <= top + 2f) l = sectionLetters[s] ?: l else break
        }
        return l
    }

    /**
     * Where the next ([down]) or previous letter starts, 0 to 1, from the section at the top: up
     * goes to the top of this one first if it is scrolled into.
     */
    fun letterStep(down: Boolean): Float {
        if (sections.isEmpty()) return 0f
        val top = scrolled()
        val l = letterAt(top)
        val i = sections.indexOfFirst { sectionLetters[it] == l }.coerceAtLeast(0)
        val to =
            if (down) sections[minOf(sections.size - 1, i + 1)]
            else if (offsetOf(sections[i]) < top - 2f) sections[i] else sections[maxOf(0, i - 1)]
        return (offsetOf(to) / maxScroll()).coerceIn(0f, 1f)
    }
}
