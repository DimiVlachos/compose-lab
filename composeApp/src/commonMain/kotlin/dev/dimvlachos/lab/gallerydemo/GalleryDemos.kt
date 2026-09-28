package dev.dimvlachos.lab.gallerydemo

import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.gallerydemo.presentation.components.GalleryDemo
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_morph_app
import kotlin.time.Duration.Companion.seconds

internal object GalleryDemos {
    // A photo, then the avatar with its pencil FAB and dialog, then search filtering the grid and a
    // photo opened from the results. Every open dwells long enough to land and show its chrome.
    private val appTour = demoScript {
        at(0.8.seconds) { select(3) } // Santorini
        at(2.6.seconds) { select(0) }
        at(3.6.seconds) { select(7) } // the avatar; the FAB fades in
        at(5.2.seconds) { select(8) } // FAB -> dialog
        at(6.8.seconds) { select(7) } // Cancel
        at(8.0.seconds) { select(0) }
        at(9.0.seconds) { select(9) } // search; "xos" types, the grid filters
        at(12.0.seconds) { select(14) } // Naxos from the results
        at(13.8.seconds) { select(9) }
        at(15.2.seconds) { select(0) } // back: the grid and the header return
    }

    val all: List<Demo> =
        listOf(Demo("morph.app", Res.string.demo_morph_app, appTour) { GalleryDemo(it) })
}
