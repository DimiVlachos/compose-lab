package dev.dimvlachos.lab.gallerydemo

import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.gallerydemo.presentation.components.GalleryDemo
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_morph_app
import kotlin.time.Duration.Companion.seconds

internal object GalleryDemos {
    // A photo, then the avatar with its pencil FAB and dialog, then the header collapsing on
    // scroll,
    // then search filtering the grid and a photo opened from the results. Every open dwells long
    // enough to land and show its chrome.
    private val appTour = demoScript {
        at(0.8.seconds) { select(3) } // Santorini
        at(2.6.seconds) { select(0) }
        at(3.6.seconds) { select(100) } // the avatar; the FAB fades in
        at(5.2.seconds) { select(101) } // FAB -> dialog
        at(6.8.seconds) { select(100) } // Cancel
        at(8.0.seconds) { select(0) }
        at(9.0.seconds) { scrollBy(1600f) } // the header collapses into the bar, with its shadow
        at(10.6.seconds) { scrollBy(-1600f) } // and grows back at the top
        at(12.2.seconds) { select(200) } // search; "xos" types, the grid filters
        at(15.2.seconds) { select(205) } // Naxos from the results
        at(17.0.seconds) { select(200) }
        at(18.4.seconds) { select(0) } // back: the grid and the header return
    }

    val all: List<Demo> =
        listOf(Demo("morph.app", Res.string.demo_morph_app, appTour) { GalleryDemo(it) })
}
