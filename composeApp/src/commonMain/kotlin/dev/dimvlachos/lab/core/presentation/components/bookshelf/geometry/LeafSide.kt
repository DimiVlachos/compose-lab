package dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry

internal sealed interface LeafSide {
    data class Page(val index: Int) : LeafSide

    data object Endpaper : LeafSide

    data object Blank : LeafSide
}

internal fun leafCount(pages: Int, spread: Boolean): Int = if (spread) (pages + 1) / 2 else pages

internal fun leafOfPage(page: Int, spread: Boolean): Int = if (spread) page / 2 else page

internal fun leafSide(index: Int, right: Boolean, pages: Int, spread: Boolean): LeafSide? {
    val page =
        when {
            !spread -> index
            right && index == -1 -> -1
            right && index in 0 until leafCount(pages, true) -> 2 * index + 1
            right -> return null
            else -> 2 * index + 2
        }
    return when (page) {
        -1 -> LeafSide.Endpaper
        in 0 until pages -> LeafSide.Page(page)
        pages -> LeafSide.Blank.takeIf { spread && right }
        else -> null
    }
}
