package dev.dimvlachos.lab.coinsorterdemo

import dev.dimvlachos.lab.coinsorterdemo.presentation.components.CoinSorterDemo
import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.DemoTag
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_coinsorter
import dev.dimvlachos.lab.resources.demo_coinsorter_description
import dev.dimvlachos.lab.resources.ic_demo_coinsorter
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal object CoinSorterDemos {
    // The script's steps, by name: tap the chip, then an option; or, for High and Best, press the
    // chip and slide onto the option.
    const val SortLow = "sort-low"
    const val SortHigh = "sort-high"
    const val SortTop = "sort-top"
    const val SortBest = "sort-best"

    // How long a step's taps take, as the prototype's finger moves: to the chip, a tap, the menu
    // opening, to the option and a tap. A slide is as long: to the chip, a hold, the slide onto
    // the option and a beat on it.
    val TapsTake = (560 + 110 + 650 + 460 + 110).milliseconds

    // A sort takes about 3.1 s from the pick to the last row landing, with room to spare.
    val SortTakes = 3300.milliseconds

    // The prototype's clip, with every sort and both ways to pick one: Price: low to high by
    // taps, Price: high to low by sliding, Top rated by taps, each left to be read for 1.4 s, then
    // Best match by sliding, so the clip ends where it began.
    private val sort = demoScript {
        var t = .9.seconds
        for (step in listOf(SortLow, SortHigh, SortTop)) {
            at(t, lasts = TapsTake + SortTakes) { tapControl(step) }
            t += TapsTake + SortTakes + 1400.milliseconds
        }
        at(t, lasts = TapsTake + SortTakes) { tapControl(SortBest) }
        t += TapsTake + SortTakes + 500.milliseconds
        at(t) { resetFeed() }
    }

    val all =
        listOf(
            Demo(
                "coinsorter.sort",
                Res.string.demo_coinsorter,
                sort,
                description = Res.string.demo_coinsorter_description,
                icon = Res.drawable.ic_demo_coinsorter,
                tags = setOf(DemoTag.Physics, DemoTag.Motion, DemoTag.Canvas),
                tall = true,
            ) {
                CoinSorterDemo(it)
            },
        )
}
