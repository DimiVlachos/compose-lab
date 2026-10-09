package dev.dimvlachos.lab.catalog

import dev.dimvlachos.lab.bookdemo.BookDemos
import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.fogdemo.FogDemos
import dev.dimvlachos.lab.gallerydemo.GalleryDemos
import dev.dimvlachos.lab.magnetdemo.MagnetDemos
import dev.dimvlachos.lab.navbardemo.NavBarDemos
import dev.dimvlachos.lab.planedemo.PlaneDemos
import dev.dimvlachos.lab.pullcorddemo.PullCordDemos
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_fog

internal object Catalog {
    // The finished demo of each component, each one a clip; the fogged mirror in two versions.
    val entries: List<CatalogEntry> =
        (NavBarDemos.all + GalleryDemos.all).map { CatalogEntry.Single(it) } +
            CatalogEntry.Group("fog.mirror", Res.string.demo_fog, FogDemos.all) +
            (BookDemos.all + PlaneDemos.all + PullCordDemos.all + MagnetDemos.all).map {
                CatalogEntry.Single(it)
            }

    /** Every demo, in or out of a folder: what an id finds, and what a recording plays. */
    val demos: List<Demo> = entries.flatMap {
        when (it) {
            is CatalogEntry.Single -> listOf(it.demo)
            is CatalogEntry.Group -> it.demos
        }
    }

    fun find(id: String?): Demo? = demos.firstOrNull { it.id == id }
}
