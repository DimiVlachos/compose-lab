package dev.dimvlachos.lab.catalog

import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.DemoTag
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

/** A card on the home screen: one demo, or a folder of versions of one. */
internal sealed interface CatalogEntry {
    /** One demo; [folder] is the folder it lives in, when a search brings it out of it. */
    class Single(val demo: Demo, val folder: Group? = null) : CatalogEntry

    /** A folder of versions of one demo, with its own card: a [title], [description] and [icon]. */
    class Group(
        val id: String,
        val title: StringResource,
        val description: StringResource,
        val icon: DrawableResource,
        val demos: List<Demo>,
    ) : CatalogEntry {
        /** Every tag its versions have, in the tags' order. */
        val tags: Set<DemoTag> = demos.flatMap { it.tags }.sortedBy { it.ordinal }.toSet()
    }
}
