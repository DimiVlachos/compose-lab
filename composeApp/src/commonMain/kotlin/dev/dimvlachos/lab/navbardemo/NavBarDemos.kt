package dev.dimvlachos.lab.navbardemo

import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.core.presentation.components.navbar.NavBarLayers
import dev.dimvlachos.lab.navbardemo.presentation.components.NavBarDemo
import dev.dimvlachos.lab.navbardemo.presentation.components.RecompositionDemo
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_navbar_action
import dev.dimvlachos.lab.resources.demo_navbar_all
import dev.dimvlachos.lab.resources.demo_navbar_cutout
import dev.dimvlachos.lab.resources.demo_navbar_cutoutmorph
import dev.dimvlachos.lab.resources.demo_navbar_icons
import dev.dimvlachos.lab.resources.demo_navbar_indicator
import dev.dimvlachos.lab.resources.demo_navbar_recompositions
import dev.dimvlachos.lab.resources.demo_navbar_scroll
import kotlin.time.Duration.Companion.seconds

internal object NavBarDemos {
    private val tabTour = demoScript {
        at(0.8.seconds) { select(2) }
        at(2.0.seconds) { select(1) }
        at(3.2.seconds) { select(2) }
        at(4.4.seconds) { select(0) }
    }

    private val scrollTour = demoScript {
        at(0.6.seconds) { scrollBy(900f) }
        at(2.2.seconds) { select(2) }
        at(3.4.seconds) { scrollBy(-900f) }
        at(5.0.seconds) { select(0) }
    }

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

    val all: List<Demo> =
        listOf(
            Demo("navbar.indicator", Res.string.demo_navbar_indicator, tabTour) {
                NavBarDemo(it, NavBarLayers(indicator = true))
            },
            Demo("navbar.icons", Res.string.demo_navbar_icons, tabTour) {
                NavBarDemo(it, NavBarLayers(icons = true))
            },
            Demo("navbar.scroll", Res.string.demo_navbar_scroll, scrollTour) {
                NavBarDemo(it, NavBarLayers(scrollAware = true), scrollingContent = true)
            },
            Demo("navbar.cutout", Res.string.demo_navbar_cutout, tabTour) {
                NavBarDemo(it, NavBarLayers(cutout = true))
            },
            Demo("navbar.cutoutmorph", Res.string.demo_navbar_cutoutmorph, scrollTour) {
                NavBarDemo(
                    it,
                    NavBarLayers(indicator = true, cutout = true, scrollAware = true),
                    scrollingContent = true,
                )
            },
            Demo("navbar.action", Res.string.demo_navbar_action, tabTour) {
                NavBarDemo(it, NavBarLayers(indicator = true, action = true))
            },
            Demo("navbar.all", Res.string.demo_navbar_all, fullTour) {
                NavBarDemo(it, NavBarLayers.All, scrollingContent = true)
            },
            Demo("navbar.recompositions", Res.string.demo_navbar_recompositions, tabTour) {
                RecompositionDemo(it)
            },
        )
}
