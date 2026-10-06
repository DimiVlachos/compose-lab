package dev.dimvlachos.lab.catalog

import dev.dimvlachos.lab.core.demo.Demo
import org.jetbrains.compose.resources.StringResource

/** A card on the home screen: one demo, or a folder of versions of one. */
internal sealed interface CatalogEntry {
    class Single(val demo: Demo) : CatalogEntry

    class Group(val id: String, val title: StringResource, val demos: List<Demo>) : CatalogEntry
}
