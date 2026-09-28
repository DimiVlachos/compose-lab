package dev.dimvlachos.lab.catalog

import dev.dimvlachos.lab.core.demo.Demo
import org.jetbrains.compose.resources.StringResource

/** One concept in the catalog, such as the nav bar, with the demos that tell its story in order. */
class CatalogSection(val title: StringResource, val demos: List<Demo>)
