package dev.dimvlachos.lab.navbardemo

import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.navbardemo.presentation.components.NavBarDemo
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_navbar
import kotlin.time.Duration.Companion.seconds

internal object NavBarDemos {
    // All six tab switches, split across the collapsed and expanded bar, with only full scrolls.
    private val fullTour = demoScript {
        at(0.6.seconds) { scrollBy(900f) }
        at(2.1.seconds) { select(1) }
        at(3.3.seconds) { select(2) }
        at(4.5.seconds) { scrollBy(-900f) }
        at(6.0.seconds) { select(1) }
        at(7.2.seconds) { select(0) }
        at(8.4.seconds) { select(2) }
        at(9.6.seconds) { scrollBy(900f) }
        at(11.1.seconds) { select(0) }
        at(12.3.seconds) { scrollBy(-900f) }
    }

    // The finished bar only: the layer-by-layer story lives on in the README's GIFs.
    val all: List<Demo> =
        listOf(Demo("navbar.all", Res.string.demo_navbar, fullTour) { NavBarDemo(it) })
}
