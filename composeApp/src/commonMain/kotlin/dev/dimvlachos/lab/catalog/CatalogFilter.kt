package dev.dimvlachos.lab.catalog

import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.DemoTag
import org.jetbrains.compose.resources.StringResource

/** What the catalog is filtered by: words typed into the search, and the tags picked. */
internal data class CatalogQuery(val text: String = "", val tags: Set<DemoTag> = emptySet()) {
    /** Whether it filters anything at all. */
    val isActive: Boolean
        get() = text.isNotBlank() || tags.isNotEmpty()
}

/**
 * The [entries] that match [query], with [text] reading a string resource. With nothing to filter
 * by, the entries come back as they are. Otherwise folders are opened out, each of their versions a
 * card of its own that remembers its folder, and a demo is kept when it has any of the query's tags
 * (or there are none) and every word typed is found in its title, description, id or tags, or in
 * its folder's title, description or id. The catalog's order is kept.
 */
internal fun filterCatalog(
    entries: List<CatalogEntry>,
    query: CatalogQuery,
    text: (StringResource) -> String,
): List<CatalogEntry> {
    if (!query.isActive) return entries
    val words = query.text.lowercase().split(Whitespace).filter { it.isNotEmpty() }
    return entries
        .flatMap { entry ->
            when (entry) {
                is CatalogEntry.Single -> listOf(entry)
                is CatalogEntry.Group -> entry.demos.map { CatalogEntry.Single(it, folder = entry) }
            }
        }
        .filter { single ->
            val demo = single.demo
            val tagged = query.tags.isEmpty() || demo.tags.any { it in query.tags }
            tagged && words.all { word -> single.haystack(demo, text).any { word in it } }
        }
}

private val Whitespace = Regex("\\s+")

private fun CatalogEntry.Single.haystack(demo: Demo, text: (StringResource) -> String) = buildList {
    add(text(demo.title))
    add(text(demo.description))
    add(demo.id)
    for (tag in demo.tags) {
        add(text(tag.label))
        add(tag.name)
    }
    folder?.let {
        add(text(it.title))
        add(text(it.description))
        add(it.id)
    }
}
    .map { it.lowercase() }
