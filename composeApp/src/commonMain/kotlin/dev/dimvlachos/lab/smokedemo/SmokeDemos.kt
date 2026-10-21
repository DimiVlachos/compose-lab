package dev.dimvlachos.lab.smokedemo

import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.DemoTag
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_smoke
import dev.dimvlachos.lab.resources.demo_smoke_description
import dev.dimvlachos.lab.resources.ic_demo_smoke
import dev.dimvlachos.lab.smokedemo.presentation.components.SmokeDemo
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal object SmokeDemos {
    // Swipes in the row's widths: two that burn well past a third of the row (the front stays
    // under the finger), and one that burns short of it.
    const val Burn = -0.62f
    const val Short = -0.18f
    const val Archive = 0.62f

    // The controls the script taps.
    const val Undo = "undo"
    const val ClearRead = "clear-read"

    // Mrs. Patterson's chat burns away and Undo breathes it back; the ferry ticket is swiped short,
    // so
    // less than a third burns, and its smoke is drawn back in; Jake goes to the archive in mist
    // and Undo brings him back; Clear read burns the five read chats in a wave and Undo brings
    // them all back. Then the list is put back as it began, which it already is, so the clip loops.
    private val inbox = demoScript {
        at(0.6.seconds, lasts = 1.4.seconds) { swipeChat("patterson", Burn, 900.milliseconds) }
        at(4.seconds, lasts = 500.milliseconds) { tapControl(Undo) }
        at(6.8.seconds, lasts = 900.milliseconds) { swipeChat("ferry", Short, 450.milliseconds) }
        at(8.6.seconds, lasts = 1.4.seconds) { swipeChat("jake", Archive, 900.milliseconds) }
        at(12.seconds, lasts = 500.milliseconds) { tapControl(Undo) }
        at(14.6.seconds, lasts = 500.milliseconds) { tapControl(ClearRead) }
        at(19.2.seconds, lasts = 500.milliseconds) { tapControl(Undo) }
        at(22.6.seconds) { resetFeed() }
    }

    // A list is to swipe: on the phone it waits for the hand, and the recorder plays this.
    val all =
        listOf(
            Demo(
                "smoke.delete",
                Res.string.demo_smoke,
                inbox,
                description = Res.string.demo_smoke_description,
                icon = Res.drawable.ic_demo_smoke,
                tags = setOf(DemoTag.Gesture, DemoTag.Canvas, DemoTag.Motion),
                tall = true,
            ) {
                SmokeDemo(it)
            },
        )
}
