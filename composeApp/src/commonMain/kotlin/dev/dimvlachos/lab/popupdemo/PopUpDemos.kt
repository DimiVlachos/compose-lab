package dev.dimvlachos.lab.popupdemo

import androidx.compose.ui.geometry.Offset
import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.popupdemo.presentation.components.PopUpDemo
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_popup
import kotlin.time.Duration.Companion.seconds

internal object PopUpDemos {
    // A drag up the book from low on its near half to high on its far half: past halfway, so the
    // leaf finishes.
    private val Low = Offset(0.5f, 0.85f)
    private val High = Offset(0.5f, 0.2f)

    // The tour: the cover dragged open, a page dragged over, the boat sailed out on its tab, Next
    // to the windmill, its sails spun twice, and the book shut again, with time for three leaves to
    // land, so the clip loops.
    private val tour = demoScript {
        at(0.8.seconds, lasts = 1.4.seconds) { dragPopUpPage(Low, High, 1.0.seconds) }
        at(3.6.seconds, lasts = 1.4.seconds) {
            dragPopUpPage(Low.copy(x = 0.55f), High.copy(x = 0.55f), 1.0.seconds)
        }
        at(6.4.seconds, lasts = 1.6.seconds) { pullPopUpTab(50f, 0.9.seconds) }
        at(9.0.seconds) { select(3) }
        at(11.0.seconds, lasts = 1.2.seconds) { pullPopUpTab(55f, 0.6.seconds) }
        at(12.8.seconds, lasts = 1.2.seconds) { pullPopUpTab(55f, 0.6.seconds) }
        at(16.0.seconds, lasts = 3.5.seconds) { select(0) }
    }

    val all = listOf(Demo("tour.popup", Res.string.demo_popup, tour, tall = true) { PopUpDemo(it) })
}
