package dev.dimvlachos.lab.bookshelfdemo

import dev.dimvlachos.lab.bookshelfdemo.presentation.components.BookshelfDemo
import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.DemoTag
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_bookshelf
import dev.dimvlachos.lab.resources.demo_bookshelf_description
import dev.dimvlachos.lab.resources.ic_demo_bookshelf
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal object BookshelfDemos {
    const val SpinOn = "spin-on"
    const val SpinBack = "spin-back"
    const val TakeOut = "take-out"
    const val TurnOn = "turn-on"
    const val TurnBack = "turn-back"
    const val PutBack = "put-back"
    const val ToPlanks = "to-planks"
    const val TakeOutPlank = "take-out-plank"

    /** The plank book the clip opens at its title page. */
    const val PlankBook = 13

    // Two books on round the ring and back, the book in front taken out to open at its bookmark,
    // two pages turned on and one back, the book put away; then down to the planks, one taken out
    // at its title page and put back, and the ring and the list put back where they began.
    private val reading = demoScript {
        at(1.2.seconds, lasts = 1200.milliseconds) { tapControl(SpinOn) }
        at(3.0.seconds, lasts = 1200.milliseconds) { tapControl(SpinBack) }
        at(4.8.seconds, lasts = 3200.milliseconds) { tapControl(TakeOut) }
        at(8.6.seconds, lasts = 1000.milliseconds) { tapControl(TurnOn) }
        at(10.0.seconds, lasts = 1000.milliseconds) { tapControl(TurnOn) }
        at(11.6.seconds, lasts = 1000.milliseconds) { tapControl(TurnBack) }
        at(13.2.seconds, lasts = 2000.milliseconds) { tapControl(PutBack) }
        at(15.8.seconds, lasts = 1000.milliseconds) { tapControl(ToPlanks) }
        at(16.8.seconds, lasts = 2800.milliseconds) { tapControl(TakeOutPlank) }
        at(20.2.seconds, lasts = 2000.milliseconds) { tapControl(PutBack) }
        at(22.6.seconds, lasts = 1500.milliseconds) { resetFeed() }
    }

    val all =
        listOf(
            Demo(
                "bookshelf.reading",
                Res.string.demo_bookshelf,
                reading,
                description = Res.string.demo_bookshelf_description,
                icon = Res.drawable.ic_demo_bookshelf,
                tags = setOf(DemoTag.Gesture, DemoTag.Physics, DemoTag.ThreeD),
                tall = true,
            ) {
                BookshelfDemo(it)
            }
        )
}
