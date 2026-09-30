package dev.dimvlachos.lab.catalog

import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.frostdemo.FrostDemos
import dev.dimvlachos.lab.gallerydemo.GalleryDemos
import dev.dimvlachos.lab.navbardemo.NavBarDemos

object Catalog {
    // The finished demo of each component, each one a clip.
    val demos: List<Demo> = NavBarDemos.all + GalleryDemos.all + FrostDemos.all

    fun find(id: String?): Demo? = demos.firstOrNull { it.id == id }
}
