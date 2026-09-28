package dev.dimvlachos.lab.catalog

import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.gallerydemo.GalleryDemos
import dev.dimvlachos.lab.imagemorphdemo.ImageMorphDemos
import dev.dimvlachos.lab.navbardemo.NavBarDemos
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.section_morph
import dev.dimvlachos.lab.resources.section_navbar

object Catalog {
    val sections: List<CatalogSection> =
        listOf(
            CatalogSection(Res.string.section_navbar, NavBarDemos.all),
            CatalogSection(Res.string.section_morph, ImageMorphDemos.layers + GalleryDemos.all),
        )

    // Every demo, section by section: ids stay flat so a recording can open any demo directly.
    val demos: List<Demo> = sections.flatMap { it.demos }

    fun find(id: String?): Demo? = demos.firstOrNull { it.id == id }

    fun sectionOf(demo: Demo): CatalogSection = sections.first { demo in it.demos }
}
