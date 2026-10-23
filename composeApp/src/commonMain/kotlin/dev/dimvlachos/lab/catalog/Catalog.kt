package dev.dimvlachos.lab.catalog

import dev.dimvlachos.lab.birdcagedemo.BirdcageDemos
import dev.dimvlachos.lab.bookdemo.BookDemos
import dev.dimvlachos.lab.bookshelfdemo.BookshelfDemos
import dev.dimvlachos.lab.cardfiledemo.CardFileDemos
import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.fishingdemo.FishingDemos
import dev.dimvlachos.lab.fogdemo.FogDemos
import dev.dimvlachos.lab.gallerydemo.GalleryDemos
import dev.dimvlachos.lab.keyhookdemo.KeyHookDemos
import dev.dimvlachos.lab.locketdemo.LocketDemos
import dev.dimvlachos.lab.magnetdemo.MagnetDemos
import dev.dimvlachos.lab.mailboxdemo.MailboxDemos
import dev.dimvlachos.lab.navbardemo.NavBarDemos
import dev.dimvlachos.lab.padlockdemo.PadlockDemos
import dev.dimvlachos.lab.paperbagdemo.PaperBagDemos
import dev.dimvlachos.lab.pencilcupdemo.PencilCupDemos
import dev.dimvlachos.lab.planedemo.PlaneDemos
import dev.dimvlachos.lab.popupdemo.PopUpDemos
import dev.dimvlachos.lab.pullcorddemo.PullCordDemos
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_fog
import dev.dimvlachos.lab.resources.demo_fog_description
import dev.dimvlachos.lab.resources.ic_demo_fog
import dev.dimvlachos.lab.smokedemo.SmokeDemos
import dev.dimvlachos.lab.snowglobedemo.SnowGlobeDemos
import dev.dimvlachos.lab.stickersdemo.StickerDemos
import dev.dimvlachos.lab.typewriterdemo.TypewriterDemos
import dev.dimvlachos.lab.zipperdemo.ZipperDemos

internal object Catalog {
    // The finished demo of each component, each one a clip; the fogged mirror in two versions.
    val entries: List<CatalogEntry> =
        (NavBarDemos.all + GalleryDemos.all).map { CatalogEntry.Single(it) } +
            CatalogEntry.Group(
                id = "fog.mirror",
                title = Res.string.demo_fog,
                description = Res.string.demo_fog_description,
                icon = Res.drawable.ic_demo_fog,
                demos = FogDemos.all,
            ) +
            (BookDemos.all +
                    PlaneDemos.all +
                    PullCordDemos.all +
                    MagnetDemos.all +
                    FishingDemos.all +
                    PopUpDemos.all +
                    TypewriterDemos.all +
                    SmokeDemos.all +
                    SnowGlobeDemos.all +
                    CardFileDemos.all +
                    BookshelfDemos.all +
                    PaperBagDemos.all +
                    StickerDemos.all +
                    MailboxDemos.all +
                    PadlockDemos.all +
                    PencilCupDemos.all +
                    LocketDemos.all +
                    BirdcageDemos.all +
                    ZipperDemos.all +
                    KeyHookDemos.all)
                .map {
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
