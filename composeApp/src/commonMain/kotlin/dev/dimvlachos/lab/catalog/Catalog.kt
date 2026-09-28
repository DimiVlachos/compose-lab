package dev.dimvlachos.lab.catalog

import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.imagemorphdemo.ImageMorphDemos
import dev.dimvlachos.lab.navbardemo.NavBarDemos

object Catalog {
    val demos: List<Demo> = NavBarDemos.all + ImageMorphDemos.layers

    fun find(id: String?): Demo? = demos.firstOrNull { it.id == id }
}
